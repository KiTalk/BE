package likelion.kitalk.touch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDetail {
  private Long menu_id;
  private String menu_item;
  private Integer price;
  private Integer quantity;
  private Boolean popular;
  private String temp;
  private String profile;
}