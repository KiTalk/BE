package likelion.kitalk.phone.service;

import java.util.function.Function;
import likelion.kitalk.global.exception.CustomException;
import likelion.kitalk.phone.dto.response.PhoneOrdersResponse;
import likelion.kitalk.phone.dto.response.TopMenusResponse;
import likelion.kitalk.phone.exception.PhoneOrderErrorCode;
import likelion.kitalk.touch.entity.Menu;
import likelion.kitalk.touch.entity.Order;
import likelion.kitalk.touch.entity.OrderItems;
import likelion.kitalk.touch.repository.MenuRepository;
import likelion.kitalk.touch.repository.OrderItemsRepository;
import likelion.kitalk.touch.repository.OrderItemsRepository.TopMenuRow;
import likelion.kitalk.touch.repository.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PhoneOrderService {

  private final OrderRepository orderRepository;
  private final OrderItemsRepository orderItemsRepository;
  private final MenuRepository menuRepository;

  public PhoneOrderService(OrderRepository orderRepository, OrderItemsRepository orderItemsRepository,
      MenuRepository menuRepository) {
    this.orderRepository = orderRepository;
    this.orderItemsRepository = orderItemsRepository;
    this.menuRepository = menuRepository;
  }

  public PhoneOrdersResponse getRecentOrders(String phone) {
    var pageable = PageRequest.of(0, 5);

    var page = orderRepository.findByPhoneNumberOrderByCreatedAtDescIdDesc(phone, pageable);
    var orders = page.getContent();

    if (orders.isEmpty()) {
      throw new CustomException(PhoneOrderErrorCode.PHONE_ORDER_NOT_FOUND);
    }

    List<Long> orderIds = orders.stream().map(Order::getId).toList();

    List<OrderItems> allItems = orderItemsRepository
        .findAllByOrderIdInOrderByOrderIdAscIdAsc(orderIds);

    Map<Long, List<OrderItems>> grouped = allItems.stream()
        .collect(Collectors.groupingBy(
            OrderItems::getOrderId, LinkedHashMap::new, Collectors.toList()));

    var blocks = new ArrayList<PhoneOrdersResponse.OrderBlock>();
    for (Order o : orders) {
      var lines = grouped.getOrDefault(o.getId(), List.of()).stream()
          .map(oi -> {
            String profile = menuRepository.findById(oi.getMenuId())
                .map(m -> m.getProfile())
                .orElse(null); // profile 가져오기
            return new PhoneOrdersResponse.OrderLine(
                oi.getMenuId(),
                oi.getMenuName(),
                oi.getPrice(),
                oi.getTemp(),
                profile
            );
          })
          .toList();

      blocks.add(new PhoneOrdersResponse.OrderBlock(
          o.getId(), o.getCreatedAt(), lines
      ));
    }

    return new PhoneOrdersResponse(blocks);
  }

  public TopMenusResponse getTopMenusByPhone(String phone) {
    List<TopMenuRow> rows = orderItemsRepository.findTopMenusByPhone(phone);
    if (rows.isEmpty()) {
      throw new CustomException(PhoneOrderErrorCode.PHONE_ORDER_NOT_FOUND);
    }

    List<Long> menuIds = rows.stream().map(TopMenuRow::getMenuId).distinct().toList();

    Map<Long, String> profileByMenu = new HashMap<>();
    menuRepository.findAllById(menuIds).forEach(m -> profileByMenu.put(m.getId(), m.getProfile()));

    var list = rows.stream()
        .map(r -> new TopMenusResponse.MenuStat(
            r.getMenuId(),
            r.getMenuName(),
            r.getTemp(),
            profileByMenu.get(r.getMenuId()),
            r.getOrderCount()
        ))
        .toList();

    return new TopMenusResponse(list);
  }
}