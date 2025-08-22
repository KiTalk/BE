package likelion.kitalk.touch.controller;

import io.swagger.v3.oas.annotations.Operation;
import likelion.kitalk.global.exception.CustomException;
import likelion.kitalk.touch.dto.request.CartAddRequest;
import likelion.kitalk.touch.dto.request.CartRemoveRequest;
import likelion.kitalk.touch.dto.request.CartUpdateRequest;
import likelion.kitalk.touch.dto.request.PackagingRequest;
import likelion.kitalk.touch.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/touch/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

  private final CartService cartService;

  @Operation(
      summary = "메뉴 추가"
  )

  @PostMapping("/{sessionId}/add")
  public ResponseEntity<Map<String, Object>> addToCart(@PathVariable String sessionId, @RequestBody CartAddRequest request) {
    log.info("장바구니 추가 API 호출 - sessionId: {}, menuId: {}, quantity: {}",
        sessionId, request.getMenuId(), request.getQuantity());

    try {
      Map<String, Object> response = cartService.addToCart(sessionId, request);

      log.info("장바구니 추가 API 성공 - sessionId: {}", sessionId);
      return ResponseEntity.ok(response);

    } catch (CustomException e) {
      log.warn("장바구니 추가 API 실패 - sessionId: {}, error: {}",
          sessionId, e.getMessage());
      return createErrorResponse(e.getErrorCode().getStatus(), e.getMessage());

    } catch (Exception e) {
      log.error("장바구니 추가 API 예상치 못한 오류 - sessionId: {}",
          sessionId, e);
      return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "장바구니 추가 중 오류가 발생했습니다");
    }
  }

  @Operation(
      summary = "장바구니 업데이트 (부분 업데이트)"
  )

  @PutMapping("/{sessionId}/update")
  public ResponseEntity<Map<String, Object>> updateCart(@PathVariable String sessionId, @RequestBody CartUpdateRequest request) {
    log.info("장바구니 업데이트 API 호출 - sessionId: {}, 요청 항목 수: {}",
        sessionId, request.getOrders() != null ? request.getOrders().size() : 0);

    try {
      Map<String, Object> response = cartService.updateCart(sessionId, request);

      log.info("장바구니 업데이트 API 성공 - sessionId: {}", sessionId);
      return ResponseEntity.ok(response);

    } catch (CustomException e) {
      log.warn("장바구니 업데이트 API 실패 - sessionId: {}, error: {}",
          sessionId, e.getMessage());
      return createErrorResponse(e.getErrorCode().getStatus(), e.getMessage());

    } catch (Exception e) {
      log.error("장바구니 업데이트 API 예상치 못한 오류 - sessionId: {}",
          sessionId, e);
      return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "장바구니 업데이트 중 오류가 발생했습니다");
    }
  }

  @Operation(
      summary = "특정 메뉴 삭제"
  )

  @DeleteMapping("/{sessionId}/remove")
  public ResponseEntity<Map<String, Object>> removeMenuItem(@PathVariable String sessionId, @RequestBody CartRemoveRequest request) {
    log.info("메뉴 삭제 API 호출 - sessionId: {}, menuId: {}",
        sessionId, request.getMenuId());

    try {
      Map<String, Object> response = cartService.removeMenuItem(sessionId, request);

      log.info("메뉴 삭제 API 성공 - sessionId: {}", sessionId);
      return ResponseEntity.ok(response);

    } catch (CustomException e) {
      log.warn("메뉴 삭제 API 실패 - sessionId: {}, error: {}",
          sessionId, e.getMessage());
      return createErrorResponse(e.getErrorCode().getStatus(), e.getMessage());

    } catch (Exception e) {
      log.error("메뉴 삭제 API 예상치 못한 오류 - sessionId: {}",
          sessionId, e);
      return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "메뉴 삭제 중 오류가 발생했습니다");
    }
  }

  @Operation(
      summary = "장바구니 전체 지우기"
  )

  @DeleteMapping("/{sessionId}/clear")
  public ResponseEntity<Map<String, Object>> clearCart(@PathVariable String sessionId) {
    log.info("장바구니 비우기 API 호출 - sessionId: {}", sessionId);

    try {
      Map<String, Object> response = cartService.clearCart(sessionId);

      log.info("장바구니 비우기 API 성공 - sessionId: {}", sessionId);
      return ResponseEntity.ok(response);

    } catch (CustomException e) {
      log.warn("장바구니 비우기 API 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
      return createErrorResponse(e.getErrorCode().getStatus(), e.getMessage());

    } catch (Exception e) {
      log.error("장바구니 비우기 API 예상치 못한 오류 - sessionId: {}", sessionId, e);
      return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "장바구니 비우기 중 오류가 발생했습니다");
    }
  }

  @Operation(
      summary = "장바구니 조회"
  )

  @GetMapping("/{sessionId}")
  public ResponseEntity<Map<String, Object>> getCart(@PathVariable("sessionId") String sessionId) {
    log.info("장바구니 조회 API 호출 - sessionId: {}", sessionId);

    try {
      Map<String, Object> response = cartService.getCart(sessionId);

      log.info("장바구니 조회 API 성공 - sessionId: {}", sessionId);
      return ResponseEntity.ok(response);

    } catch (CustomException e) {
      log.warn("장바구니 조회 API 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
      return createErrorResponse(e.getErrorCode().getStatus(), e.getMessage());

    } catch (Exception e) {
      log.error("장바구니 조회 API 예상치 못한 오류 - sessionId: {}", sessionId, e);
      return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "장바구니 조회 중 오류가 발생했습니다");
    }
  }

  @Operation(
      summary = "포장 방식 설정"
  )

  @PostMapping("/{sessionId}/packaging")
  public ResponseEntity<Map<String, Object>> setPackagingType(@PathVariable String sessionId, @RequestBody PackagingRequest request) {  // ✅ 수정: @PathVariable 추가
    log.info("포장 방식 설정 API 호출 - sessionId: {}, packagingType: {}",
        sessionId, request.getPackagingType());

    try {
      Map<String, Object> response = cartService.setPackagingType(sessionId, request);

      log.info("포장 방식 설정 API 성공 - sessionId: {}", sessionId);
      return ResponseEntity.ok(response);

    } catch (CustomException e) {
      log.warn("포장 방식 설정 API 실패 - sessionId: {}, error: {}",
          sessionId, e.getMessage());
      return createErrorResponse(e.getErrorCode().getStatus(), e.getMessage());

    } catch (Exception e) {
      log.error("포장 방식 설정 API 예상치 못한 오류 - sessionId: {}",
          sessionId, e);
      return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "포장 방식 설정 중 오류가 발생했습니다");
    }
  }

  @Operation(
      summary = "장바구니 헬스 체크"
  )

  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> healthCheck() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "healthy");
    response.put("service", "touch-cart");
    response.put("timestamp", System.currentTimeMillis());

    return ResponseEntity.ok(response);
  }

// 에러 응답 생성
  private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String message) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("success", false);
    errorResponse.put("message", message);
    errorResponse.put("status", status.value());
    errorResponse.put("timestamp", System.currentTimeMillis());

    return ResponseEntity.status(status).body(errorResponse);
  }
}