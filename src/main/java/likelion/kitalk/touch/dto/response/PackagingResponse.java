package likelion.kitalk.touch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagingResponse {
  private Boolean success;
  private String message;
  private String sessionId;
  private String packagingType;
}