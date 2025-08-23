package likelion.kitalk.phone.service;

import likelion.kitalk.global.exception.CustomException;
import likelion.kitalk.phone.dto.response.PhoneOrdersResponse;
import likelion.kitalk.phone.dto.response.TopMenusResponse;
import likelion.kitalk.phone.exception.PhoneOrderErrorCode;
import likelion.kitalk.touch.entity.Order;
import likelion.kitalk.touch.entity.OrderItems;
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

  /**
   * Creates a PhoneOrderService with the required repositories.
   */
  public PhoneOrderService(OrderRepository orderRepository, OrderItemsRepository orderItemsRepository) {
    this.orderRepository = orderRepository;
    this.orderItemsRepository = orderItemsRepository;
  }

  /**
   * Retrieves up to five most recent orders for the given phone number and returns them with their items.
   *
   * The response contains an ordered list of order blocks (most recent first), each with the order ID
   * and its associated order lines (menuId, menuName, price, temp). Order items are loaded for all
   * returned orders and grouped by order ID to preserve the original order grouping.
   *
   * @param phone the phone number to look up orders for
   * @return a PhoneOrdersResponse containing up to five recent orders and their items
   * @throws CustomException if no orders are found for the provided phone (PhoneOrderErrorCode.PHONE_ORDER_NOT_FOUND)
   */
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
          .map(oi -> new PhoneOrdersResponse.OrderLine(
              oi.getMenuId(),
              oi.getMenuName(),
              oi.getPrice(),
              oi.getTemp()
          ))
          .toList();

      blocks.add(new PhoneOrdersResponse.OrderBlock(o.getId(), lines));
    }

    return new PhoneOrdersResponse(blocks);
  }

  /**
   * Retrieves top-ordered menus for the customer identified by the given phone number.
   *
   * Queries order items to compute top menu statistics for that phone and returns them
   * as a TopMenusResponse containing MenuStat entries (menuId, menuName, orderCount).
   *
   * @param phone the customer's phone number to query top menus for
   * @return a TopMenusResponse containing the list of menu statistics
   * @throws CustomException if no orders are found for the provided phone (PhoneOrderErrorCode.PHONE_ORDER_NOT_FOUND)
   */
  public TopMenusResponse getTopMenusByPhone(String phone) {
    List<TopMenuRow> rows = orderItemsRepository.findTopMenusByPhone(phone);

    if (rows.isEmpty()) {
      throw new CustomException(PhoneOrderErrorCode.PHONE_ORDER_NOT_FOUND);
    }

    var list = rows.stream()
        .map(r -> new TopMenusResponse.MenuStat(
            r.getMenuId(),
            r.getMenuName(),
            r.getOrderCount()
        ))
        .toList();

    return new TopMenusResponse(list);
  }
}