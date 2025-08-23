package likelion.kitalk.phone.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TopMenusResponse(
    @JsonProperty("top_menus") List<MenuStat> topMenus
) {
  public record MenuStat(
      @JsonProperty("menu_id") Long menuId,
      @JsonProperty("menu_item") String menuItem,
      @JsonProperty("count") Long count   // 주문 건수 기준(동일 주문 내 중복 1회)
  ) {}
}