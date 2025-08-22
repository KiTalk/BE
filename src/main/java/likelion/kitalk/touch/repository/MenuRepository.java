package likelion.kitalk.touch.repository;

import java.util.List;
import likelion.kitalk.touch.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

  // 기본 조회 메서드들
  List<Menu> findByCategoryAndIsActiveTrue(String category);

  // 정렬된 메뉴 조회
  List<Menu> findByIsActiveTrueOrderByCategoryAscNameAscTemperatureAsc();
  List<Menu> findByCategoryAndIsActiveTrueOrderByNameAscTemperatureAsc(String category);

  // "모든메뉴" 카테고리용 - 여러 카테고리를 IN으로 조회
  List<Menu> findByCategoryInAndIsActiveTrueOrderByNameAscTemperatureAsc(List<String> categories);

  // 누락된 메서드 추가
  List<Menu> findByCategoryInAndIsActiveTrue(List<String> categories);
}