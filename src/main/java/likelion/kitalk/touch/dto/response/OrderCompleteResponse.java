package likelion.kitalk.touch.dto.response;

import likelion.kitalk.touch.dto.CartItemDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCompleteResponse {
    private String message;
    private Integer order_id;
    private List<CartItemDetail> orders;
    private Integer total_items;
    private Integer total_price;
    private String packaging;
    private String phone_number;
    private String next_step;
}
