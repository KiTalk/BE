package likelion.kitalk.touch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuResponse {
  private Long id;
  private String name;
  private String temperature;
  private Integer price;
  private String category;
  private Boolean isActive;
  private String profile;
}