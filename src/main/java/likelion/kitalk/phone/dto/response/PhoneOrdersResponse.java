package likelion.kitalk.phone.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PhoneOrdersResponse(
    @JsonProperty("results") List<OrderBlock> results
) {
  public record OrderBlock(
      @JsonProperty("order_id") Long orderId,
      @JsonProperty("orders") List<OrderLine> orders
  ) {}

  public record OrderLine(
      @JsonProperty("menu_id") Long menuId,
      @JsonProperty("menu_item") String menuItem,
      @JsonProperty("price") Integer price,
      @JsonProperty("temp") String temp
  ) {}
}
