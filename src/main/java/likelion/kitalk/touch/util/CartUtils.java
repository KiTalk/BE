package likelion.kitalk.touch.util;

import likelion.kitalk.touch.dto.CartItemDetail;
import likelion.kitalk.touch.dto.response.CartResponse;
import likelion.kitalk.touch.dto.response.PackagingResponse;
import likelion.kitalk.touch.entity.Menu;
import likelion.kitalk.touch.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartUtils {

    private final MenuService menuService;

    // 빈 장바구니 생성
    public Map<String, Object> createEmptyCart() {
        Map<String, Object> cartData = new HashMap<>();
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        cartData.put("items", new ArrayList<>());
        cartData.put("createdAt", now);
        cartData.put("updatedAt", now);

        return cartData;
    }

    // 장바구니에서 특정 메뉴 찾기
    public Map<String, Object> findCartItem(List<Map<String, Object>> items, Long menuId) {
        return items.stream()
                .filter(item -> ((Number) item.get("menuId")).longValue() == menuId)
                .findFirst()
                .orElse(null);
    }

    // 총 가격 계싼
    public int calculateTotalPrice(List<Map<String, Object>> items) {
        int totalPrice = 0;

        for (Map<String, Object> item : items) {
            try {
                Long menuId = ((Number) item.get("menuId")).longValue();
                Integer quantity = (Integer) item.get("quantity");

                // MySQL에서 메뉴 정보 조회
                Menu menu = menuService.getMenuById(menuId);
                totalPrice += menu.getPrice() * quantity;

            } catch (Exception e) {
                log.warn("메뉴 가격 계산 중 오류 - menuId: {}, error: {}",
                    item.get("menuId"), e.getMessage());
                // 오류 발생시 해당 항목은 0원으로 처리
            }
        }

        return totalPrice;
    }

    // Redis 장바구니 아이템을 CartItemDetail로 변환
    public List<CartItemDetail> convertToCartItemDetails(List<Map<String, Object>> items) {
        List<CartItemDetail> cartItems = new ArrayList<>();

        for (Map<String, Object> item : items) {
            try {
                Long menuId = ((Number) item.get("menuId")).longValue();
                Integer quantity = (Integer) item.get("quantity");

                // MySQL에서 메뉴 정보 조회
                Menu menu = menuService.getMenuById(menuId);

                CartItemDetail cartItem = CartItemDetail.builder()
                        .menu_id(menuId)
                        .menu_item(menu.getName())
                        .price(menu.getPrice())
                        .quantity(quantity)
                        .popular(menu.getIsPopular() != null ? menu.getIsPopular() : false)
                        .temp(menu.getTemperature())
                        .profile(menu.getProfile())
                        .build();

                cartItems.add(cartItem);

            } catch (Exception e) {
                log.error("장바구니 아이템 변환 중 오류 - menuId: {}, error: {}",
                    item.get("menuId"), e.getMessage());
                // 오류 발생한 아이템은 제외하고 계속 진행
            }
        }

        return cartItems;
    }

    // 포장 방식 응답 생성
    public PackagingResponse createPackagingResponse(String message, String sessionId, String packagingType) {
        return PackagingResponse.builder()
                .success(true)
                .message(message)
                .sessionId(sessionId)
                .packagingType(packagingType)
                .build();
    }

    // CartResponse를 Map으로 변환
    public Map<String, Object> convertToMap(CartResponse cartResponse) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", cartResponse.getMessage());
        response.put("orders", cartResponse.getOrders());           // items → orders
        response.put("total_items", cartResponse.getTotal_items()); // totalItems → total_items
        response.put("total_price", cartResponse.getTotal_price()); // 새로 추가
        response.put("packaging", cartResponse.getPackaging());     // 새로 추가
        response.put("session_id", cartResponse.getSession_id());   // sessionId → session_id
        return response;
    }

    // PackagingResponse를 Map으로 변환
    public Map<String, Object> convertToMap(PackagingResponse packagingResponse) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", packagingResponse.getSuccess());
        response.put("message", packagingResponse.getMessage());
        response.put("sessionId", packagingResponse.getSessionId());
        response.put("packagingType", packagingResponse.getPackagingType());
        return response;
    }
}