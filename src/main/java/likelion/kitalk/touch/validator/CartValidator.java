package likelion.kitalk.touch.validator;

import likelion.kitalk.global.exception.CustomException;
import likelion.kitalk.touch.dto.request.CartAddRequest;
import likelion.kitalk.touch.dto.request.CartRemoveRequest;
import likelion.kitalk.touch.dto.request.CartUpdateRequest;
import likelion.kitalk.touch.dto.request.PackagingRequest;
import likelion.kitalk.touch.exception.CartErrorCode;
import likelion.kitalk.touch.exception.MenuErrorCode;
import likelion.kitalk.touch.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CartValidator {

  private final MenuService menuService;

  // 유효한 포장 방식 목록
  private static final List<String> VALID_PACKAGING_TYPES = Arrays.asList("포장", "매장", "takeout", "dine-in");

  // 장바구니 추가 요청 검증
  public void validateAddRequest(String sessionId, CartAddRequest request) {
    validateSessionId(sessionId);
    validateMenuId(request.getMenuId());
    validateQuantity(request.getQuantity());
    
    // 실제 메뉴 존재 여부 확인
    validateMenuExists(request.getMenuId());
  }

  // 장바구니 업데이트 요청 검증
  public void validateUpdateRequest(String sessionId, CartUpdateRequest request) {
    validateSessionId(sessionId);
    validateUpdateItems(request.getOrders());
  }

  // 장바구니 제거 요청 검증
  public void validateRemoveRequest(String sessionId, CartRemoveRequest request) {
    validateSessionId(sessionId);
    validateMenuId(request.getMenuId());
    
    // 실제 메뉴 존재 여부 확인
    validateMenuExists(request.getMenuId());
  }

  // 포장 방식 요청 검증
  public void validatePackagingRequest(String sessionId, PackagingRequest request) {
    validateSessionId(sessionId);
    validatePackagingType(request.getPackagingType());
  }

  // 세션 ID만 검증
  public void validateSessionOnly(String sessionId) {
    validateSessionId(sessionId);
  }


  private void validateSessionId(String sessionId) {
    if (sessionId == null || sessionId.trim().isEmpty()) {
      throw new CustomException(CartErrorCode.INVALID_SESSION_ID);
    }
  }

  private void validateMenuId(Long menuId) {
    if (menuId == null || menuId <= 0) {
      throw new CustomException(CartErrorCode.INVALID_MENU_ID);
    }
  }

  private void validateQuantity(Integer quantity) {
    if (quantity == null || quantity <= 0) {
      throw new CustomException(CartErrorCode.INVALID_QUANTITY);
    }
  }

  // 실제 메뉴 존재 여부 확인
  private void validateMenuExists(Long menuId) {
    try {
      menuService.getMenuById(menuId);
    } catch (Exception e) {
      throw new CustomException(MenuErrorCode.MENU_NOT_FOUND);
    }
  }

  private void validateUpdateItems(List<CartUpdateRequest.CartUpdateItem> orders) {
    if (orders == null || orders.isEmpty()) {
      throw new CustomException(CartErrorCode.INVALID_REQUEST);
    }

    for (CartUpdateRequest.CartUpdateItem item : orders) {
      if (item.getMenu_id() == null) {
        throw new CustomException(CartErrorCode.INVALID_MENU_ID);
      }
      if (item.getQuantity() == null) {
        throw new CustomException(CartErrorCode.INVALID_QUANTITY);
      }

      // menuId 검증
      if (item.getMenu_id() <= 0) {
        throw new CustomException(CartErrorCode.INVALID_MENU_ID);
      }
      
      // 실제 메뉴 존재 여부 확인
      validateMenuExists(item.getMenu_id());

      // quantity 검증 (0은 허용 - 삭제 의미)
      if (item.getQuantity() < 0) {
        throw new CustomException(CartErrorCode.INVALID_QUANTITY);
      }
    }
  }

  private void validatePackagingType(String packagingType) {
    if (packagingType == null || packagingType.trim().isEmpty()) {
      throw new CustomException(CartErrorCode.INVALID_PACKAGING_TYPE);
    }

    if (!VALID_PACKAGING_TYPES.contains(packagingType)) {
      throw new CustomException(CartErrorCode.INVALID_PACKAGING_TYPE);
    }
  }
}