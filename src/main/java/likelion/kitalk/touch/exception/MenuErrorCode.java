package likelion.kitalk.touch.exception;

import likelion.kitalk.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MenuErrorCode implements BaseErrorCode {
  // 카테고리 관련 에러
  INVALID_CATEGORY("M001", "유효하지 않은 카테고리입니다.", HttpStatus.BAD_REQUEST),
  EMPTY_CATEGORY_RESULT("M002", "해당 카테고리에 조회된 메뉴가 없습니다.", HttpStatus.NOT_FOUND),

  // 메뉴 관련 에러
  MENU_NOT_FOUND("M006", "메뉴를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  // 서버 에러
  MENU_LIST_FETCH_ERROR("M003", "메뉴 목록 조회 중 서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  CATEGORY_LIST_FETCH_ERROR("M004", "카테고리 목록 조회 중 서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  DATABASE_ACCESS_ERROR("M005", "데이터베이스 접근 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus status;
}