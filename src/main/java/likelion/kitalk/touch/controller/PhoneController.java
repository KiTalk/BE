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

    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("status", status.value());
        errorResponse.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(status).body(errorResponse);
    }
}
