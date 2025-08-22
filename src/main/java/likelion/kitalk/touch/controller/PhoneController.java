package likelion.kitalk.touch.controller;

import io.swagger.v3.oas.annotations.Operation;
import likelion.kitalk.global.exception.CustomException;
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
