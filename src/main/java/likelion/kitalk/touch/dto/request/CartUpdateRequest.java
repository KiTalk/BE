package likelion.kitalk.touch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartUpdateRequest {
  private List<CartUpdateItem> orders;
  
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CartUpdateItem {
    private Long menu_id;
    private Integer quantity;
  }
}
