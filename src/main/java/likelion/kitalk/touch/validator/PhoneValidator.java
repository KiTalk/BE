package likelion.kitalk.touch.validator;

import likelion.kitalk.global.exception.CustomException;
import likelion.kitalk.touch.dto.request.PhoneChoiceRequest;
import likelion.kitalk.touch.dto.request.PhoneInputRequest;
import likelion.kitalk.touch.exception.PhoneErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class PhoneValidator {

    // 전화번호 유효성 검사 패턴 (010으로 시작하는 11자리)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^010\\d{8}$");

    // 세션 ID 검증
    public void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("유효하지 않은 세션 ID: {}", sessionId);
            throw new CustomException(PhoneErrorCode.INVALID_SESSION_FOR_PHONE);
        }
    }

    // 전화번호 선택 요청 검증
    public void validatePhoneChoiceRequest(String sessionId, PhoneChoiceRequest request) {
        validateSessionId(sessionId);
        
        if (request == null) {
            throw new CustomException(PhoneErrorCode.PHONE_CHOICE_REQUIRED);
        }
        
        if (request.getWants_phone() == null) {
            log.warn("전화번호 입력 여부가 null - sessionId: {}", sessionId);
            throw new CustomException(PhoneErrorCode.PHONE_CHOICE_REQUIRED);
        }
        
        log.debug("전화번호 선택 요청 검증 통과 - sessionId: {}, wants_phone: {}", 
            sessionId, request.getWants_phone());
    }

    // 전화번호 입력 요청 검증
    public void validatePhoneInputRequest(String sessionId, PhoneInputRequest request) {
        validateSessionId(sessionId);
        
        if (request == null) {
            throw new CustomException(PhoneErrorCode.PHONE_NUMBER_REQUIRED);
        }
        
        String phoneNumber = request.getPhone_number();
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.warn("전화번호가 비어있음 - sessionId: {}", sessionId);
            throw new CustomException(PhoneErrorCode.PHONE_NUMBER_REQUIRED);
        }
        
        // 전화번호 형식 검증
        if (!isValidPhoneNumber(phoneNumber)) {
            log.warn("유효하지 않은 전화번호 형식 - sessionId: {}, phone: {}", sessionId, phoneNumber);
            throw new CustomException(PhoneErrorCode.INVALID_PHONE_NUMBER);
        }
        
        log.debug("전화번호 입력 요청 검증 통과 - sessionId: {}, phone: {}", sessionId, phoneNumber);
    }

    // 주문 완료 요청 검증
    public void validateCompleteOrderRequest(String sessionId) {
        validateSessionId(sessionId);
        
        log.debug("주문 완료 요청 검증 통과 - sessionId: {}", sessionId);
    }

    // 전화번호 유효성 검사
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // 공백과 하이픈 제거
        String cleanPhone = phoneNumber.replace("-", "").replace(" ", "");
        
        // 010으로 시작하고 11자리 숫자인지 확인
        boolean isValid = PHONE_PATTERN.matcher(cleanPhone).matches();
        
        if (!isValid) {
            log.debug("전화번호 형식 검증 실패: {} -> {}", phoneNumber, cleanPhone);
        }
        
        return isValid;
    }
}
