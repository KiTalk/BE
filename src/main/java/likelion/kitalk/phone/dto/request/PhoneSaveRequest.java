package likelion.kitalk.phone.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneSaveRequest {

  @Schema(description = "저장할 휴대폰 번호", example = "010-1234-5678")
  private String phone;
}