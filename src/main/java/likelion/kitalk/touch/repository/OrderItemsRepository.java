package likelion.kitalk.touch.repository;

import java.util.Collection;
import java.util.List;
import likelion.kitalk.touch.entity.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {

  /**
   * Retrieves all OrderItems whose orderId is in the given collection, ordered by orderId then item id (both ascending).
   *
   * @param orderIds collection of order IDs to fetch items for
   * @return list of matching OrderItems ordered by orderId ASC, id ASC
   */
  @Query("""
        SELECT oi
        FROM OrderItems oi
        WHERE oi.orderId IN :orderIds
        ORDER BY oi.orderId ASC, oi.id ASC
    """)
  List<OrderItems> findAllByOrderIdInOrderByOrderIdAscIdAsc(
      @Param("orderIds") Collection<Long> orderIds
  );

  /**
   * Returns the top 3 most-ordered menus for a customer phone number.
   *
   * Executes a native query that counts distinct orders per menu (menuId, menuName)
   * for orders associated with the given phone number and returns the three menus
   * with highest distinct-order counts; results are ordered by order count
   * descending then menuId ascending and mapped to the TopMenuRow projection.
   *
   * @param phone the customer's phone number to filter orders by
   * @return a list of up to three TopMenuRow projections containing menuId, menuName, and orderCount
   */
  @Query(value = """
        SELECT 
            oi.menu_id   AS menuId,
            oi.menu_name AS menuName,
            COUNT(DISTINCT oi.order_id) AS orderCount
        FROM order_items oi
        JOIN orders o ON o.id = oi.order_id
        WHERE o.phone_number = :phone
        GROUP BY oi.menu_id, oi.menu_name
        ORDER BY orderCount DESC, oi.menu_id ASC
        LIMIT 3
    """, nativeQuery = true)
  List<TopMenuRow> findTopMenusByPhone(@Param("phone") String phone);

  interface TopMenuRow {
    /**
 * Returns the menu's identifier.
 *
 * Maps to the `menu_id` column produced by the repository query.
 *
 * @return the menu id, or null if not present
 */
Long getMenuId();
    /**
 * Returns the menu's display name.
 *
 * This value maps to the native query column `menuName` in the projection.
 *
 * @return the menu name (as returned by the query)
 */
String getMenuName();
    /**
 * Returns the number of distinct orders that included this menu item.
 *
 * <p>Maps to the `orderCount` column from the native query; may be null if no value is present.</p>
 *
 * @return the count of distinct orders for this menu
 */
Long getOrderCount();
  }
}