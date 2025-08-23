package likelion.kitalk.touch.controller;

import io.swagger.v3.oas.annotations.Operation;
import likelion.kitalk.global.exception.CustomException;
import likelion.kitalk.phone.dto.request.PhoneSaveRequest;
import likelion.kitalk.touch.dto.request.PhoneChoiceRequest;
import likelion.kitalk.touch.dto.request.PhoneInputRequest;
import likelion.kitalk.touch.service.PhoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/touch/phone")
@RequiredArgsConstructor
@Slf4j
public class PhoneController {

    private final PhoneService phoneService;

    @Operation(
        summary = "전화번호 입력 여부 선택"
    )

    @PostMapping("/{sessionId}/choice")
    public ResponseEntity<Map<String, Object>> processPhoneChoice(
            @PathVariable String sessionId, 
            @RequestBody PhoneChoiceRequest request) {
        
        log.info("전화번호 선택 API 호출 - sessionId: {}, wants_phone: {}", 
            sessionId, request.getWants_phone());

        try {
            Map<String, Object> response = phoneService.processPhoneChoice(sessionId, request.getWants_phone());
            
            log.info("전화번호 선택 API 성공 - sessionId: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (CustomException e) {
            log.warn("전화번호 선택 API 실패 - sessionId: {}, error: {}", 
                sessionId, e.getMessage());
            return createErrorResponse(e.getErrorCode().getStatus(), e.getMessage());

        } catch (Exception e) {
            log.error("전화번호 선택 API 예상치 못한 오류 - sessionId: {}", sessionId, e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "전화번호 선택 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * Handle submission of a user's phone number for a given session.
     *
     * Processes the provided phone number via the phone service and returns the service's response map on success.
     *
     * @param sessionId identifier for the current session
     * @param request   request DTO containing the phone number to process
     * @return ResponseEntity containing a Map<String, Object> with the service result on success, or a structured error payload on failure
     */
    @Operation(
        summary = "전화번호 입력"
    )

    @PostMapping("/{sessionId}/input")
    public ResponseEntity<Map<String, Object>> processPhoneInput(
            @PathVariable String sessionId, 
            @RequestBody PhoneInputRequest request) {
        
        log.info("전화번호 입력 API 호출 - sessionId: {}, phone_number: {}", 
            sessionId, request.getPhone_number());

        try {
            Map<String, Object> response = phoneService.processPhoneInput(sessionId, request.getPhone_number());
            
            log.info("전화번호 입력 API 성공 - sessionId: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (CustomException e) {
            log.warn("전화번호 입력 API 실패 - sessionId: {}, error: {}", 
                sessionId, e.getMessage());
            return createErrorResponse(e.getErrorCode().getStatus(), e.getMessage());

        } catch (Exception e) {
            log.error("전화번호 입력 API 예상치 못한 오류 - sessionId: {}", sessionId, e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "전화번호 입력 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * Completes an order for the given session when no phone number is provided.
     *
     * Calls the service to finalize the order immediately for the specified session.
     *
     * @param sessionId the touch-session identifier for which the order should be completed
     * @return a ResponseEntity containing the service result map on success (HTTP 200), or an error response map with an appropriate HTTP status when completion fails
     */
    @PostMapping("/{sessionId}/complete")
    @Operation(
        summary = "주문 완료(전화번호 입력 X)",
        description = "전화번호 입력을 마친 세션에서 주문을 즉시 완료"
    )
    public ResponseEntity<Map<String, Object>> completeOrder(
        @PathVariable("sessionId") String sessionId
    ) {
        log.info("주문 완료 API 호출 - sessionId: {}", sessionId);

        try {
            Map<String, Object> response = phoneService.completeOrderWithoutPhone(sessionId);

            log.info("주문 완료 API 성공 - sessionId: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (CustomException e) {
            log.warn("주문 완료 API 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
            return createErrorResponse(e.getErrorCode().getStatus(), e.getMessage());

        } catch (Exception e) {
            log.error("주문 완료 API 예상치 못한 오류 - sessionId: {}", sessionId, e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "주문 완료 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * Persist the provided phone number for the given session.
     *
     * <p>Accepts a PhoneSaveRequest and stores its phone value in the session identified by {@code sessionId}.
     *
     * @param request PhoneSaveRequest whose {@code getPhone()} value will be saved for the session
     * @return ResponseEntity containing the service response map on success (HTTP 200) or an error response map on failure
     */
    @PostMapping("/{sessionId}/phone_number")
    @Operation(
        summary = "전화번호 저장",
        description = "세션에 전화번호 저장"
    )
    public ResponseEntity<Map<String, Object>> savePhone(
        @PathVariable("sessionId") String sessionId,
        @RequestBody PhoneSaveRequest request
    ) {
        log.info("전화번호 저장 API 호출 - sessionId: {}", sessionId);
        try {
            Map<String, Object> response = phoneService.savePhone(sessionId, request.getPhone());
            log.info("전화번호 저장 API 성공 - sessionId: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (CustomException e) {
            log.warn("전화번호 저장 API 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
            return createErrorResponse(e.getErrorCode().getStatus(), e.getMessage());

        } catch (Exception e) {
            log.error("전화번호 저장 API 예상치 못한 오류 - sessionId: {}", sessionId, e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "전화번호 저장 중 오류가 발생했습니다");
        }
    }

    /**
     * Build a standardized error response payload wrapped in a ResponseEntity with the given HTTP status.
     *
     * The response body is a Map with keys:
     * - "success" (Boolean): always false
     * - "message" (String): provided human-readable error message
     * - "status" (Integer): numeric HTTP status code
     * - "timestamp" (Long): epoch milliseconds when the response was created
     *
     * @param status  HTTP status to set on the ResponseEntity and include in the payload
     * @param message Human-readable error message to include in the payload
     * @return ResponseEntity containing the error map and the provided HTTP status
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("status", status.value());
        errorResponse.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(status).body(errorResponse);
    }
}
