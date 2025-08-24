package likelion.kitalk.touch.repository;

import likelion.kitalk.touch.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
Page<Order> findByPhoneNumberOrderByCreatedAtDescIdDesc(String phoneNumber, org.springframework.data.domain.Pageable pageable);
}
