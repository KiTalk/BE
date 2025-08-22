package likelion.kitalk.touch.exception;

import likelion.kitalk.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PhoneErrorCode implements BaseErrorCode {
    // 입력값 검증 에러
    INVALID_PHONE_NUMBER("P001", "유효하지 않은 전화번호 형식입니다. (예: 010-1234-5678)", HttpStatus.BAD_REQUEST),
    PHONE_NUMBER_REQUIRED("P002", "전화번호는 필수 입력값입니다.", HttpStatus.BAD_REQUEST),
    PHONE_CHOICE_REQUIRED("P003", "전화번호 입력 여부를 선택해주세요.", HttpStatus.BAD_REQUEST),
    
    // 세션 관련 에러
    INVALID_SESSION_FOR_PHONE("P004", "전화번호 처리가 불가능한 세션 상태입니다.", HttpStatus.BAD_REQUEST),
    SESSION_EXPIRED_FOR_PHONE("P005", "세션이 만료되어 전화번호 처리가 불가능합니다.", HttpStatus.GONE),
    
    // 주문 관련 에러
    NO_ITEMS_TO_ORDER("P006", "주문할 메뉴가 없습니다.", HttpStatus.BAD_REQUEST),
    PACKAGING_TYPE_NOT_SET("P007", "포장 방식이 설정되지 않았습니다.", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_COMPLETED("P008", "이미 완료된 주문입니다.", HttpStatus.CONFLICT),
    
    // 데이터 처리 에러
    PHONE_DATA_SAVE_FAILED("P009", "전화번호 저장 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ORDER_SAVE_FAILED("P010", "주문 저장 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    PHONE_DATA_CORRUPTED("P011", "전화번호 데이터가 손상되었습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // 외부 서비스 에러
    DATABASE_CONNECTION_FAILED("P012", "데이터베이스 연결에 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    REDIS_CONNECTION_FAILED("P013", "Redis 연결에 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
