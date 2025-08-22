package likelion.kitalk.touch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "menu")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Menu {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String temperature;

  @Column(nullable = false)
  private Integer price;

  @Column(nullable = false)
  private String category;

  @Column(name = "is_active", columnDefinition = "TINYINT(1) DEFAULT 1")
  private Boolean isActive = true;

  @Column(name = "popular", columnDefinition = "TINYINT(1) DEFAULT 0")
  private Boolean isPopular = false;

  @Column(name = "profile", length = 500)
  private String profile;  // AWS S3 이미지 URL
}