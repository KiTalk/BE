package likelion.kitalk.touch.controller;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import likelion.kitalk.global.dto.BaseResponse;
import likelion.kitalk.touch.dto.response.CategoryResponse;
import likelion.kitalk.touch.dto.response.MenuResponse;
import likelion.kitalk.touch.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // CORS 설정 (개발용)
@Slf4j
public class MenuController {

  private final MenuService menuService;

  @Operation(
      summary = "카테고리 음료 목록 조회"
  )

  // 메뉴 리스트 조회
  @GetMapping("/list")
  public ResponseEntity<BaseResponse<List<MenuResponse>>> getMenuList(
      @RequestParam(required = false) String category) {

    log.info("메뉴 리스트 조회 API 호출 - category: {}", category);

    BaseResponse<List<MenuResponse>> response = menuService.getMenuList(category);

    if (response.isSuccess()) {
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.badRequest().body(response);
    }
  }

  @Operation(
      summary = "카테고리 목록 및 수량 조회"
  )

  // 카테고리 목록 조회
  @GetMapping("/categories")
  public ResponseEntity<BaseResponse<List<CategoryResponse>>> getCategoryList() {

    log.info("카테고리 목록 조회 API 호출");

    BaseResponse<List<CategoryResponse>> response = menuService.getCategoryList();

    if (response.isSuccess()) {
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.badRequest().body(response);
    }
  }
}