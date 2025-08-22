package likelion.kitalk.touch.converter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import likelion.kitalk.touch.dto.response.CategoryResponse;
import likelion.kitalk.touch.dto.response.MenuResponse;
import likelion.kitalk.touch.entity.Menu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MenuConverter {

  // Menu Entity MenuResponse로 반환
  public MenuResponse toResponse(Menu menu) {
    if (menu == null) {
      return null;
    }

    return MenuResponse.builder()
        .id(menu.getId())
        .name(menu.getName())
        .temperature(menu.getTemperature())
        .price(menu.getPrice())
        .category(menu.getCategory())
        .isActive(menu.getIsActive())
        .profile(menu.getProfile())
        .build();
  }

  // Menu 리스트를 MenuResponse 리스트로 변환
  public List<MenuResponse> toResponseList(List<Menu> menus) {
    if (menus == null || menus.isEmpty()) {
      return Collections.emptyList();
    }

    return menus.stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  public CategoryResponse toCategoryResponse(String categoryName, List<Menu> menus) {
    if (menus == null || menus.isEmpty()) {
      return CategoryResponse.builder()
          .name(categoryName)
          .menuCount(0)
          .build();
    }

    return CategoryResponse.builder()
        .name(categoryName)
        .menuCount(menus.size())
        .build();
  }
}