package likelion.kitalk.phone.exception;

import likelion.kitalk.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PhoneOrderErrorCode implements BaseErrorCode {
  PHONE_ORDER_NOT_FOUND("PO001", "등록된 번호가 없습니다.", HttpStatus.NOT_FOUND);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
