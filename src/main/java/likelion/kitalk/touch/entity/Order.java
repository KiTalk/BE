package likelion.kitalk.touch.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import likelion.kitalk.global.common.BaseTimeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "phone_number", length = 20)
  private String phoneNumber;  // NULL 허용

  @Column(name = "total_price", nullable = false)
  private Integer totalPrice;

  @Column(name = "packaging_type", length = 50, nullable = false)
  private String packagingType;

  @Column(length = 50)
  @Builder.Default
  private String status = "completed";

  // 연관관계
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
  private List<OrderItems> orderItems = new ArrayList<>();
}