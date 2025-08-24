package likelion.kitalk.phone.controller;

import io.swagger.v3.oas.annotations.Operation;
import likelion.kitalk.global.dto.BaseResponse;
import likelion.kitalk.phone.dto.response.PhoneOrdersResponse;
import likelion.kitalk.phone.dto.response.TopMenusResponse;
import likelion.kitalk.phone.service.PhoneOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/phone")
@RequiredArgsConstructor
public class PhoneOrderController {

  private final PhoneOrderService service;

  @GetMapping("/orders")
  @Operation(
      summary = "최근 주문 5건 조회",
      description = "전화번호로 필터링하여 최근 주문을 최대 5건까지 반환"
  )
  public BaseResponse<PhoneOrdersResponse> getRecentOrders(
      @RequestParam("phone") String phone
  ) {
    return BaseResponse.success(service.getRecentOrders(phone));
  }

  @GetMapping("/top-menus")
  @Operation(
      summary = "가장 많이 주문한 메뉴 TOP 3",
      description = "상위 3개 메뉴를 반환"
  )
  public BaseResponse<TopMenusResponse> getTopMenus(@RequestParam("phone") String phone) {
    return BaseResponse.success(service.getTopMenusByPhone(phone));
  }
}