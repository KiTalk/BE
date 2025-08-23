package likelion.kitalk.touch.repository;

import likelion.kitalk.touch.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
  /**
 * Finds orders for the given phone number, returning results in pages.
 *
 * Returns a Page of Order entities whose phoneNumber equals the provided value,
 * ordered by createdAt descending and then id descending.
 *
 * @param phoneNumber the phone number to filter orders by (exact match)
 * @param pageable    pagination information (page number and size); sorting here is ignored in favor of the method's defined ordering
 * @return a page of matching Order entities ordered by createdAt desc, id desc
 */
Page<Order> findByPhoneNumberOrderByCreatedAtDescIdDesc(String phoneNumber, org.springframework.data.domain.Pageable pageable);
}
