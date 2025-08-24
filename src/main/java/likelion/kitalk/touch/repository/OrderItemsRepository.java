package likelion.kitalk.touch.repository;

import java.util.Collection;
import java.util.List;
import likelion.kitalk.touch.entity.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {

  @Query("""
        SELECT oi
        FROM OrderItems oi
        WHERE oi.orderId IN :orderIds
        ORDER BY oi.orderId ASC, oi.id ASC
    """)
  List<OrderItems> findAllByOrderIdInOrderByOrderIdAscIdAsc(
      @Param("orderIds") Collection<Long> orderIds
  );

  @Query(value = """
        SELECT
            oi.menu_id   AS menuId,
            oi.menu_name AS menuName,
            oi.temp      AS temp,
            COUNT(DISTINCT oi.order_id) AS orderCount
        FROM order_items oi
        JOIN orders o ON o.id = oi.order_id
        WHERE o.phone_number = :phone
        GROUP BY oi.menu_id, oi.menu_name, oi.temp
        ORDER BY orderCount DESC, oi.menu_id ASC
        LIMIT 3
    """, nativeQuery = true)
  List<TopMenuRow> findTopMenusByPhone(@Param("phone") String phone);

  interface TopMenuRow {
    Long getMenuId();
    String getMenuName();
    String getTemp();
    Long getOrderCount();
  }
}
