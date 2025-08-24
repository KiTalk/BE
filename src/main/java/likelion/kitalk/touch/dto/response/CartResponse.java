package likelion.kitalk.touch.dto.response;

import likelion.kitalk.touch.dto.CartItemDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
  private String message;
  private List<CartItemDetail> orders;
  private Integer total_items;
  private Integer total_price;
  private String packaging;
  private String session_id;
}
