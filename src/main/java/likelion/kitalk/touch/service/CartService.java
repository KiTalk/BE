package likelion.kitalk.touch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.kitalk.global.exception.CustomException;
import likelion.kitalk.touch.dto.request.CartAddRequest;
import likelion.kitalk.touch.dto.request.CartRemoveRequest;
import likelion.kitalk.touch.dto.request.CartUpdateRequest;
import likelion.kitalk.touch.dto.request.PackagingRequest;
import likelion.kitalk.touch.exception.CartErrorCode;
import likelion.kitalk.touch.util.CartUtils;
import likelion.kitalk.touch.validator.CartValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private final CartValidator cartValidator;
  private final CartUtils cartUtils;

  private static final String CART_KEY_PREFIX = "touch_cart:";
  private static final String PACKAGING_KEY_PREFIX = "touch_packaging:";  // 포장 방식 키 prefix
  private static final long CART_EXPIRE_HOURS = 2;

  // 장바구니에 메뉴 추가
  public Map<String, Object> addToCart(String sessionId, CartAddRequest request) {
    log.info("장바구니 담기 - sessionId: {}, menuId: {}, quantity: {}",
        sessionId, request.getMenuId(), request.getQuantity());

    cartValidator.validateAddRequest(sessionId, request);

    try {
      Map<String, Object> cartData = getCartData(sessionId);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> items = (List<Map<String, Object>>) cartData.get("items");

      // 동일한 메뉴가 있는지 확인
      Map<String, Object> existingItem = cartUtils.findCartItem(items, request.getMenuId());

      if (existingItem != null) {
        // 기존 항목 수량 증가
        Integer currentQuantity = (Integer) existingItem.get("quantity");
        Integer newQuantity = currentQuantity + request.getQuantity();
        existingItem.put("quantity", newQuantity);
        log.debug("기존 메뉴 수량 증가 - menuId: {}, {} → {}",
            request.getMenuId(), currentQuantity, newQuantity);
      } else {
        // 새 항목 추가
        Map<String, Object> newItem = new HashMap<>();
        newItem.put("menuId", request.getMenuId());
        newItem.put("quantity", request.getQuantity());
        items.add(newItem);
        log.debug("새 메뉴 추가 - menuId: {}, quantity: {}",
            request.getMenuId(), request.getQuantity());
      }

      saveCartData(sessionId, cartData);

      log.info("장바구니 담기 완료 - sessionId: {}, 총 항목 수: {}",
          sessionId, items.size());

      return cartUtils.convertToMap(
          createCartResponseWithPackaging("장바구니에 담겼습니다", cartData, sessionId)
      );

    } catch (Exception e) {
      log.error("장바구니 담기 중 오류 발생 - sessionId: {}", sessionId, e);
      throw new CustomException(CartErrorCode.CART_UPDATE_FAILED);
    }
  }

  // 장바구니 전체 업데이트
  public Map<String, Object> updateCart(String sessionId, CartUpdateRequest request) {
    log.info("장바구니 업데이트 - sessionId: {}, 요청 항목 수: {}",
        sessionId, request.getOrders().size());

    cartValidator.validateUpdateRequest(sessionId, request);

    try {
      Map<String, Object> cartData = getCartData(sessionId);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> currentItems = (List<Map<String, Object>>) cartData.get("items");

      // 현재 장바구니를 Map으로 변환 (효율적인 조회를 위해)
      Map<Long, Map<String, Object>> currentItemsMap = new HashMap<>();
      for (Map<String, Object> item : currentItems) {
        Long menuId = ((Number) item.get("menuId")).longValue();
        currentItemsMap.put(menuId, item);
      }

      // 요청된 항목들을 Map으로 변환
      Map<Long, Integer> requestItemsMap = new HashMap<>();
      for (CartUpdateRequest.CartUpdateItem requestItem : request.getOrders()) {
        Long menuId = requestItem.getMenu_id();
        Integer quantity = requestItem.getQuantity();
        requestItemsMap.put(menuId, quantity);
      }

      int addedCount = 0, updatedCount = 0, removedCount = 0;

      // 1. 요청된 항목들 처리 (추가 또는 수량 변경)
      for (Map.Entry<Long, Integer> entry : requestItemsMap.entrySet()) {
        Long menuId = entry.getKey();
        Integer newQuantity = entry.getValue();

        if (newQuantity <= 0) {
          // 수량이 0 이하면 제거
          if (currentItemsMap.containsKey(menuId)) {
            currentItems.removeIf(item -> ((Number) item.get("menuId")).longValue() == menuId);
            removedCount++;
            log.debug("메뉴 제거 - menuId: {}", menuId);
          }
        } else {
          Map<String, Object> existingItem = currentItemsMap.get(menuId);
          if (existingItem != null) {
            // 기존 항목 수량 변경
            Integer oldQuantity = (Integer) existingItem.get("quantity");
            if (!oldQuantity.equals(newQuantity)) {
              existingItem.put("quantity", newQuantity);
              updatedCount++;
              log.debug("메뉴 수량 변경 - menuId: {}, {} → {}",
                  menuId, oldQuantity, newQuantity);
            }
          } else {
            // 새 항목 추가
            Map<String, Object> newItem = new HashMap<>();
            newItem.put("menuId", menuId);
            newItem.put("quantity", newQuantity);
            currentItems.add(newItem);
            addedCount++;
            log.debug("새 메뉴 추가 - menuId: {}, 수량: {}", menuId, newQuantity);
          }
        }
      }

      // 2. 요청에 없는 기존 항목들 제거
      Iterator<Map<String, Object>> iterator = currentItems.iterator();
      while (iterator.hasNext()) {
        Map<String, Object> item = iterator.next();
        Long menuId = ((Number) item.get("menuId")).longValue();
        if (!requestItemsMap.containsKey(menuId)) {
          iterator.remove();
          removedCount++;
          log.debug("요청에 없는 메뉴 제거 - menuId: {}", menuId);
        }
      }

      saveCartData(sessionId, cartData);

      log.info("장바구니 업데이트 완료 - sessionId: {}, 추가: {}, 변경: {}, 제거: {}, 총 항목: {}",
          sessionId, addedCount, updatedCount, removedCount, currentItems.size());

      String message = String.format("장바구니가 업데이트되었습니다 (추가: %d, 변경: %d, 제거: %d)",
          addedCount, updatedCount, removedCount);

      return cartUtils.convertToMap(
          createCartResponseWithPackaging(message, cartData, sessionId)
      );

    } catch (Exception e) {
      log.error("장바구니 업데이트 중 오류 발생 - sessionId: {}", sessionId, e);
      throw new CustomException(CartErrorCode.CART_UPDATE_FAILED);
    }
  }

  // 특정 메뉴 삭제
  public Map<String, Object> removeMenuItem(String sessionId, CartRemoveRequest request) {
    log.info("특정 메뉴 삭제 - sessionId: {}, menuId: {}",
        sessionId, request.getMenuId());

    cartValidator.validateRemoveRequest(sessionId, request);

    try {
      Map<String, Object> cartData = getCartData(sessionId);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> items = (List<Map<String, Object>>) cartData.get("items");

      // 해당 메뉴 찾기 및 제거
      boolean removed = items.removeIf(item ->
          ((Number) item.get("menuId")).longValue() == request.getMenuId());

      if (!removed) {
        throw new CustomException(CartErrorCode.CART_ITEM_NOT_FOUND);
      }

      saveCartData(sessionId, cartData);

      log.info("특정 메뉴 삭제 완료 - sessionId: {}, menuId: {}, 남은 항목 수: {}",
          sessionId, request.getMenuId(), items.size());

      return cartUtils.convertToMap(
          createCartResponseWithPackaging("메뉴가 삭제되었습니다", cartData, sessionId)
      );

    } catch (Exception e) {
      log.error("특정 메뉴 삭제 중 오류 발생 - sessionId: {}", sessionId, e);
      throw new CustomException(CartErrorCode.CART_UPDATE_FAILED);
    }
  }

  // 장바구니 전체 비우기
  public Map<String, Object> clearCart(String sessionId) {
    log.info("장바구니 전체 비우기 - sessionId: {}", sessionId);

    cartValidator.validateSessionOnly(sessionId);

    try {
      String cartKey = CART_KEY_PREFIX + sessionId;
      redisTemplate.delete(cartKey);

      Map<String, Object> emptyCart = cartUtils.createEmptyCart();

      log.info("장바구니 비우기 완료 - sessionId: {}", sessionId);

      return cartUtils.convertToMap(
          createCartResponseWithPackaging("장바구니가 비워졌습니다", emptyCart, sessionId)
      );

    } catch (Exception e) {
      log.error("장바구니 비우기 중 오류 발생 - sessionId: {}", sessionId, e);
      throw new CustomException(CartErrorCode.CART_CLEAR_FAILED);
    }
  }

  // 장바구니 조회
  public Map<String, Object> getCart(String sessionId) {
    log.info("장바구니 조회 - sessionId: {}", sessionId);

    cartValidator.validateSessionOnly(sessionId);

    try {
      Map<String, Object> cartData = getCartData(sessionId);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> items = (List<Map<String, Object>>) cartData.get("items");

      log.info("장바구니 조회 완료 - sessionId: {}, 항목 수: {}", sessionId, items.size());

      return cartUtils.convertToMap(
          createCartResponseWithPackaging("장바구니 조회 성공", cartData, sessionId)
      );

    } catch (Exception e) {
      log.error("장바구니 조회 중 오류 발생 - sessionId: {}", sessionId, e);
      throw new CustomException(CartErrorCode.CART_FETCH_FAILED);
    }
  }

  // 포장 방식 설정
  public Map<String, Object> setPackagingType(String sessionId, PackagingRequest request) {
    log.info("포장 방식 설정 - sessionId: {}, packagingType: {}",
        sessionId, request.getPackagingType());

    cartValidator.validatePackagingRequest(sessionId, request);

    try {
      // 별도 키로 포장 방식 저장
      String packagingKey = PACKAGING_KEY_PREFIX + sessionId;
      Map<String, Object> packagingData = new HashMap<>();
      packagingData.put("packagingType", request.getPackagingType());
      packagingData.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

      String packagingJson = objectMapper.writeValueAsString(packagingData);
      redisTemplate.opsForValue().set(packagingKey, packagingJson, CART_EXPIRE_HOURS, TimeUnit.HOURS);

      log.info("포장 방식 설정 완료 - sessionId: {}, packagingType: {}",
          sessionId, request.getPackagingType());

      return cartUtils.convertToMap(
          cartUtils.createPackagingResponse("포장 방식이 설정되었습니다",
              sessionId,
              request.getPackagingType())
      );

    } catch (Exception e) {
      log.error("포장 방식 설정 중 오류 발생 - sessionId: {}", sessionId, e);
      throw new CustomException(CartErrorCode.PACKAGING_UPDATE_FAILED);
    }
  }


  // 포장 방식을 포함한 CartResponse 생성
  private likelion.kitalk.touch.dto.response.CartResponse createCartResponseWithPackaging(
      String message, Map<String, Object> cartData, String sessionId) {
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = (List<Map<String, Object>>) cartData.get("items");

    // Redis 아이템을 CartItemDetail로 변환
    var orders = cartUtils.convertToCartItemDetails(items);
    
    // 총 가격 계산
    int totalPrice = cartUtils.calculateTotalPrice(items);
    
    // 포장 방식 조회
    String packaging = getPackagingType(sessionId);
    
    return likelion.kitalk.touch.dto.response.CartResponse.builder()
            .message(message)
            .orders(orders)
            .total_items(orders.size())
            .total_price(totalPrice)
            .packaging(packaging)
            .session_id(sessionId)
            .build();
  }

  // Redis에서 포장 방식 조회
  private String getPackagingType(String sessionId) {
    try {
      String packagingKey = PACKAGING_KEY_PREFIX + sessionId;
      String packagingJson = redisTemplate.opsForValue().get(packagingKey);
      
      if (packagingJson == null) {
        return null;  // 설정되지 않았으면 null 반환
      }
      
      @SuppressWarnings("unchecked")
      Map<String, Object> packagingData = objectMapper.readValue(packagingJson, Map.class);
      return (String) packagingData.get("packagingType");
      
    } catch (Exception e) {
      log.warn("포장 방식 조회 실패 - sessionId: {}, null 반환", sessionId, e);
      return null;  // 오류시 null 반환
    }
  }

  // Redis에서 장바구니 데이터 조회
  @SuppressWarnings("unchecked")
  private Map<String, Object> getCartData(String sessionId) {
    try {
      String cartKey = CART_KEY_PREFIX + sessionId;
      String cartJson = redisTemplate.opsForValue().get(cartKey);

      if (cartJson == null) {
        return cartUtils.createEmptyCart();
      }

      return objectMapper.readValue(cartJson, Map.class);

    } catch (JsonProcessingException e) {
      log.error("장바구니 데이터 파싱 실패 - sessionId: {}", sessionId, e);
      throw new CustomException(CartErrorCode.CART_DATA_CORRUPTED);
    }
  }

  // Redis에 장바구니 데이터 저장
  private void saveCartData(String sessionId, Map<String, Object> cartData) {
    try {
      // 업데이트 시간 갱신
      cartData.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

      String cartKey = CART_KEY_PREFIX + sessionId;
      String cartJson = objectMapper.writeValueAsString(cartData);

      redisTemplate.opsForValue().set(cartKey, cartJson, CART_EXPIRE_HOURS, TimeUnit.HOURS);

    } catch (JsonProcessingException e) {
      log.error("장바구니 데이터 저장 실패 - sessionId: {}", sessionId, e);
      throw new CustomException(CartErrorCode.CART_SAVE_FAILED);
    }
  }
}