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

  /**
   * Add a menu item to the cart for the given session.
   *
   * <p>Accepts a session identifier and a CartAddRequest (typically containing menuId and quantity).
   * On success returns HTTP 200 with a response map produced by the cart service (current cart state or
   * operation result). On failure returns a ResponseEntity containing an error map and an appropriate
   * HTTP status (CustomException yields its mapped status; other errors yield 500).</p>
   *
   * @param sessionId the touch session identifier for the cart
   * @param request   the add-to-cart request (menu id and quantity)
   * @return a ResponseEntity whose body is a map with either the service response (on success) or an
   *         error payload (on failure)
   */
  @Operation(
      summary = "메뉴 추가"
  )

  @PostMapping("/{sessionId}/add")
  public ResponseEntity<Map<String, Object>> addToCart(@PathVariable("sessionId") String sessionId, @RequestBody CartAddRequest request) {
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

  /**
   * Partially updates a user's cart for the given session.
   *
   * <p>Applies the changes described in the provided CartUpdateRequest to the cart identified
   * by sessionId and returns a response map with the updated cart state and metadata.
   * On success returns HTTP 200 with the service response; on error the controller maps
   * known domain errors to their corresponding HTTP status and returns a standardized
   * error body, while unexpected errors yield HTTP 500.</p>
   *
   * @param sessionId the session identifier for the cart to update
   * @param request   the partial update payload describing changes to apply to the cart
   * @return a ResponseEntity containing a map with the operation result (on success) or
   *         an error body (on failure)
   */
  @Operation(
      summary = "장바구니 업데이트 (부분 업데이트)"
  )

  @PutMapping("/{sessionId}/update")
  public ResponseEntity<Map<String, Object>> updateCart(@PathVariable("sessionId") String sessionId, @RequestBody CartUpdateRequest request) {
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

  /**
   * Remove a menu item from the cart for the given session.
   *
   * <p>Calls the cart service to remove the specified menu item and returns a ResponseEntity
   * containing a result map. On success the body contains the service response; on failure
   * the body is an error map with keys like `success`, `message`, `status`, and `timestamp`.</p>
   *
   * @param sessionId the session identifier for the cart
   * @param request   request payload containing the menu item to remove (e.g., `menuId`)
   * @return a ResponseEntity whose body is a map with the operation result or an error payload
   */
  @Operation(
      summary = "특정 메뉴 삭제"
  )

  @DeleteMapping("/{sessionId}/remove")
  public ResponseEntity<Map<String, Object>> removeMenuItem(@PathVariable("sessionId") String sessionId, @RequestBody CartRemoveRequest request) {
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

  /**
   * Removes all items from the cart for the specified session and returns the result.
   *
   * @param sessionId the session identifier of the cart to clear
   * @return HTTP 200 with the service response map on success; on error returns a ResponseEntity containing an error body and the appropriate HTTP status
   */
  @Operation(
      summary = "장바구니 전체 지우기"
  )

  @DeleteMapping("/{sessionId}/clear")
  public ResponseEntity<Map<String, Object>> clearCart(@PathVariable("sessionId") String sessionId) {
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

  /**
   * Set the packaging type for the cart identified by the given session.
   *
   * <p>Accepts a PackagingRequest and updates the cart's packaging preference via the CartService.
   * On success returns HTTP 200 with the service response map. If a CustomException occurs the
   * response uses the exception's error status and message; unexpected errors return HTTP 500 with
   * a generic error message.
   *
   * @param sessionId the cart session identifier
   * @param request   request payload containing the desired packaging type
   * @return ResponseEntity containing a map with the operation result or an error payload
   */
  @Operation(
      summary = "포장 방식 설정"
  )

  @PostMapping("/{sessionId}/packaging")
  public ResponseEntity<Map<String, Object>> setPackagingType(@PathVariable("sessionId") String sessionId, @RequestBody PackagingRequest request) {
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