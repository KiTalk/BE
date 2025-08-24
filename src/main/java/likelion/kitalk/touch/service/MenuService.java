package likelion.kitalk.touch.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import likelion.kitalk.global.dto.BaseResponse;
import likelion.kitalk.global.exception.CustomException;
import likelion.kitalk.touch.converter.MenuConverter;
import likelion.kitalk.touch.dto.response.CategoryResponse;
import likelion.kitalk.touch.dto.response.MenuResponse;
import likelion.kitalk.touch.entity.Menu;
import likelion.kitalk.touch.exception.MenuErrorCode;
import likelion.kitalk.touch.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MenuService {

  private final MenuRepository menuRepository;
  private final MenuConverter menuConverter;

  // "모든메뉴"에 포함될 카테고리들
  private final List<String> ALL_MENU_CATEGORIES = Arrays.asList(
      "스무디", "프라페", "특색 라떼", "스페셜 티", "에이드", "버블티"
  );

  // 개별 카테고리들
  private final List<String> INDIVIDUAL_CATEGORIES = Arrays.asList(
      "커피", "기타 음료", "주스", "차", "디저트"
  );

  // 메뉴 리스트 조회
  public BaseResponse<List<MenuResponse>> getMenuList(String category) {
    log.info("메뉴 리스트 조회 시작 - category: {}", category);

    try {
      validateCategory(category);

      List<Menu> menus;

      if ("모든 메뉴".equals(category)) {
        log.debug("모든 메뉴 카테고리 조회 실행");
        menus = menuRepository.findByCategoryInAndIsActiveTrueOrderByNameAscTemperatureAsc(ALL_MENU_CATEGORIES);
      } else if (INDIVIDUAL_CATEGORIES.contains(category)) {
        log.debug("개별 카테고리 조회 실행: {}", category);
        menus = menuRepository.findByCategoryAndIsActiveTrueOrderByNameAscTemperatureAsc(category);
      } else if (category == null || category.trim().isEmpty()) {
        log.debug("전체 메뉴 조회 실행");
        menus = menuRepository.findByIsActiveTrueOrderByCategoryAscNameAscTemperatureAsc();
      } else {
        log.warn("유효하지 않은 카테고리 요청: {}", category);
        throw new CustomException(MenuErrorCode.INVALID_CATEGORY);
      }

      // 🔧 디테일한 빈 결과 처리
      if (menus == null || menus.isEmpty()) {
        if (category != null && !category.trim().isEmpty() && !"모든 메뉴".equals(category)) {
          log.warn("카테고리 '{}' 조회 결과가 비어있음", category);
          throw new CustomException(MenuErrorCode.EMPTY_CATEGORY_RESULT);
        }
      }

      List<MenuResponse> menuResponses = menuConverter.toResponseList(menus);

      log.info("메뉴 리스트 조회 완료 - category: {}, 결과 수: {}", category, menuResponses.size());

      return BaseResponse.success("메뉴 조회 성공", menuResponses);

    } catch (CustomException e) {
      log.error("메뉴 리스트 조회 비즈니스 예외 - category: {}, error: {}", category, e.getErrorCode().getMessage());
      throw e;
    } catch (DataAccessException e) {
      log.error("메뉴 리스트 조회 중 데이터베이스 오류 - category: {}", category, e);
      throw new CustomException(MenuErrorCode.DATABASE_ACCESS_ERROR);
    } catch (Exception e) {
      log.error("메뉴 리스트 조회 중 예상치 못한 오류 - category: {}", category, e);
      throw new CustomException(MenuErrorCode.MENU_LIST_FETCH_ERROR);
    }
  }

  // 카테고리 목록 및 각 카테고리별 메뉴 수 조회
  public BaseResponse<List<CategoryResponse>> getCategoryList() {
    log.info("카테고리 목록 조회 시작");

    try {
      List<CategoryResponse> categories = new ArrayList<>();

      List<Menu> allMenus = menuRepository.findByCategoryInAndIsActiveTrue(ALL_MENU_CATEGORIES);
      categories.add(menuConverter.toCategoryResponse("모든메뉴", allMenus));
      log.debug("모든메뉴 카테고리 추가 완료 - 메뉴 수: {}", allMenus.size());

      int addedCategories = 0;
      for (String category : INDIVIDUAL_CATEGORIES) {
        try {
          List<Menu> categoryMenus = menuRepository.findByCategoryAndIsActiveTrue(category);
          if (!categoryMenus.isEmpty()) {
            categories.add(menuConverter.toCategoryResponse(category, categoryMenus));
            addedCategories++;
            log.debug("카테고리 '{}' 추가 완료 - 메뉴 수: {}", category, categoryMenus.size());
          } else {
            log.debug("카테고리 '{}' 메뉴 없음으로 제외", category);
          }
        } catch (Exception e) {
          log.warn("카테고리 '{}' 조회 중 오류 발생, 해당 카테고리 제외: {}", category, e.getMessage());
        }
      }

      if (categories.isEmpty()) {
        log.error("조회된 카테고리가 하나도 없음");
        throw new CustomException(MenuErrorCode.EMPTY_CATEGORY_RESULT);
      }

      log.info("카테고리 목록 조회 완료 - 총 카테고리 수: {}, 개별 카테고리 수: {}",
          categories.size(), addedCategories);

      return BaseResponse.success("카테고리 조회 성공", categories);

    } catch (CustomException e) {
      log.error("카테고리 목록 조회 비즈니스 예외: {}", e.getErrorCode().getMessage());
      throw e;
    } catch (DataAccessException e) {
      log.error("카테고리 목록 조회 중 데이터베이스 오류", e);
      throw new CustomException(MenuErrorCode.DATABASE_ACCESS_ERROR);
    } catch (Exception e) {
      log.error("카테고리 목록 조회 중 예상치 못한 오류", e);
      throw new CustomException(MenuErrorCode.CATEGORY_LIST_FETCH_ERROR);
    }
  }

  // 카테고리 유효성 검증
  private void validateCategory(String category) {
    if (category == null || category.trim().isEmpty()) {
      log.debug("카테고리가 null 또는 빈 값으로 전체 조회로 처리");
      return;
    }

    // 허용된 카테고리 목록 구성
    List<String> validCategories = new ArrayList<>();
    validCategories.add("모든 메뉴");
    validCategories.addAll(ALL_MENU_CATEGORIES);
    validCategories.addAll(INDIVIDUAL_CATEGORIES);

    // 대소문자 및 공백 무시하고 검증
    String trimmedCategory = category.trim();
    boolean isValid = validCategories.stream()
        .anyMatch(valid -> valid.equalsIgnoreCase(trimmedCategory));

    if (!isValid) {
      log.error("유효하지 않은 카테고리 요청: '{}'. 허용된 카테고리: {}",
          trimmedCategory, validCategories);
      throw new CustomException(MenuErrorCode.INVALID_CATEGORY);
    }

    log.debug("카테고리 유효성 검증 통과: '{}'", trimmedCategory);
  }

  // 메뉴 ID로 메뉴 불러오기
  public Menu getMenuById(Long menuId) {
    return menuRepository.findById(menuId)
        .filter(Menu::getIsActive)  // 활성화된 메뉴만
        .orElseThrow(() -> new CustomException(MenuErrorCode.MENU_NOT_FOUND));
  }
}