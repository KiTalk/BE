package likelion.kitalk.touch.exception;

import likelion.kitalk.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CartErrorCode implements BaseErrorCode {
  // 입력값 검증 에러
  INVALID_SESSION_ID("C001", "유효하지 않은 세션 ID입니다.", HttpStatus.BAD_REQUEST),
  INVALID_MENU_ID("C002", "유효하지 않은 메뉴 ID입니다.", HttpStatus.BAD_REQUEST),
  INVALID_QUANTITY("C003", "유효하지 않은 수량입니다.", HttpStatus.BAD_REQUEST),
  INVALID_REQUEST("C004", "유효하지 않은 요청입니다.", HttpStatus.BAD_REQUEST),
  INVALID_PACKAGING_TYPE("C005", "유효하지 않은 포장 방식입니다.", HttpStatus.BAD_REQUEST),

  // 장바구니 관련 에러
  CART_ITEM_NOT_FOUND("C006", "장바구니에서 해당 메뉴를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  CART_IS_EMPTY("C007", "장바구니가 비어있습니다.", HttpStatus.NOT_FOUND),

  // Redis 관련 에러
  CART_UPDATE_FAILED("C008", "장바구니 업데이트 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  CART_FETCH_FAILED("C009", "장바구니 조회 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  CART_SAVE_FAILED("C010", "장바구니 저장 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  CART_CLEAR_FAILED("C011", "장바구니 비우기 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  CART_DATA_CORRUPTED("C012", "장바구니 데이터가 손상되었습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

  // 세션 관련 에러
  SESSION_UPDATE_FAILED("C013", "세션 업데이트 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  PACKAGING_UPDATE_FAILED("C014", "포장 방식 설정 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus status;
}