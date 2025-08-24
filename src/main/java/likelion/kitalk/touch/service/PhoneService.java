package likelion.kitalk.touch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.kitalk.global.exception.CustomException;
import likelion.kitalk.touch.dto.CartItemDetail;
import likelion.kitalk.touch.dto.request.PhoneChoiceRequest;
import likelion.kitalk.touch.dto.request.PhoneInputRequest;
import likelion.kitalk.touch.dto.response.OrderCompleteResponse;
import likelion.kitalk.touch.dto.response.PhoneResponse;
import likelion.kitalk.touch.exception.PhoneErrorCode;
import likelion.kitalk.touch.util.CartUtils;
import likelion.kitalk.touch.validator.PhoneValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CartUtils cartUtils;
    private final PhoneValidator phoneValidator;
    private final DataSource dataSource;

    // Redis 키 상수들
    private static final String CART_KEY_PREFIX = "touch_cart:";
    private static final String PACKAGING_KEY_PREFIX = "touch_packaging:";
    private static final String PHONE_KEY_PREFIX = "touch_phone:";
    private static final String SESSION_COMPLETED_KEY_PREFIX = "touch_session_completed:";
    private static final long CART_EXPIRE_HOURS = 2;

    // 전화번호 입력 여부 선택 처리
    public Map<String, Object> processPhoneChoice(String sessionId, Boolean wantsPhone) {
        log.info("전화번호 선택 처리 시작 - sessionId: {}, wantsPhone: {}", sessionId, wantsPhone);

        try {
            // 요청 검증
            PhoneChoiceRequest request = PhoneChoiceRequest.builder()
                    .wants_phone(wantsPhone)
                    .build();
            phoneValidator.validatePhoneChoiceRequest(sessionId, request);

            // 세션 상태 검증
            validateSessionForPhoneProcessing(sessionId);

            if (Boolean.TRUE.equals(wantsPhone)) {
                log.debug("전화번호 입력 선택 - sessionId: {}", sessionId);
                
                PhoneResponse response = PhoneResponse.builder()
                        .message("전화번호를 입력해주세요.")
                        .next_step("전화번호 입력")
                        .build();

                log.info("전화번호 선택 처리 완료 - sessionId: {}, 결과: 입력 단계로 이동", sessionId);
                return convertToMap(response);
            } else {
                log.debug("전화번호 입력 건너뛰기 선택 - sessionId: {}", sessionId);
                
                // 전화번호 입력 안하겠다고 선택 → 바로 완료
                log.info("전화번호 선택 처리 완료 - sessionId: {}, 결과: 바로 주문 완료", sessionId);
                return completeOrder(sessionId);
            }

        } catch (CustomException e) {
            log.warn("전화번호 선택 처리 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("전화번호 선택 처리 중 예상치 못한 오류 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.PHONE_DATA_SAVE_FAILED);
        }
    }

    // 전화번호 입력 처리
    public Map<String, Object> processPhoneInput(String sessionId, String phoneNumber) {
        log.info("전화번호 입력 처리 시작 - sessionId: {}, phoneNumber: {}", sessionId, 
            phoneNumber != null ? phoneNumber.replaceAll("\\d(?=\\d{4})", "*") : "null");

        try {
            // 요청 검증
            PhoneInputRequest request = PhoneInputRequest.builder()
                    .phone_number(phoneNumber)
                    .build();
            phoneValidator.validatePhoneInputRequest(sessionId, request);

            // 세션 상태 검증
            validateSessionForPhoneProcessing(sessionId);

            // 전화번호 정규화
            String normalizedPhone = normalizePhoneNumber(phoneNumber);
            log.debug("전화번호 정규화 완료 - sessionId: {}, 정규화된 번호: {}", 
                sessionId, normalizedPhone.replaceAll("\\d(?=\\d{4})", "*"));

            // Redis에 전화번호 저장
            savePhoneNumberToRedis(sessionId, normalizedPhone);

            log.info("전화번호 입력 처리 완료 - sessionId: {}", sessionId);

            // 바로 주문 완료 처리
            return completeOrder(sessionId);

        } catch (CustomException e) {
            log.warn("전화번호 입력 처리 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("전화번호 입력 처리 중 예상치 못한 오류 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.PHONE_DATA_SAVE_FAILED);
        }
    }

    // 주문 완료 처리
    public Map<String, Object> completeOrder(String sessionId) {
        log.info("주문 완료 처리 시작 - sessionId: {}", sessionId);

        try {
            // 요청 검증
            phoneValidator.validateCompleteOrderRequest(sessionId);

            // 세션 상태 검증
            validateSessionForPhoneProcessing(sessionId);

            // 이미 완료된 주문인지 확인
            if (isOrderAlreadyCompleted(sessionId)) {
                log.warn("이미 완료된 주문 - sessionId: {}", sessionId);
                throw new CustomException(PhoneErrorCode.ORDER_ALREADY_COMPLETED);
            }

            // 장바구니 데이터 가져오기 및 검증
            Map<String, Object> cartData = getCartDataWithValidation(sessionId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) cartData.get("items");

            if (items == null || items.isEmpty()) {
                log.warn("주문할 메뉴가 없음 - sessionId: {}", sessionId);
                throw new CustomException(PhoneErrorCode.NO_ITEMS_TO_ORDER);
            }

            // 주문 아이템들을 CartItemDetail로 변환
            List<CartItemDetail> orders = cartUtils.convertToCartItemDetails(items);
            log.debug("주문 아이템 변환 완료 - sessionId: {}, 아이템 수: {}", sessionId, orders.size());

            // 총 가격 계산
            int totalPrice = cartUtils.calculateTotalPrice(items);

            // 포장 방식 조회
            String packaging = getPackagingTypeWithValidation(sessionId);

            // 전화번호 조회
            String phoneNumber = getPhoneNumber(sessionId);

            // MySQL에 주문 저장
            int orderId = saveOrderToMySQL(orders, packaging, phoneNumber);

            // 세션 완료로 변경 (5분간 유지)
            updateSessionToCompleted(sessionId, orderId);

            log.info("주문 완료 처리 성공 - sessionId: {}, orderId: {}, totalPrice: {}원", 
                sessionId, orderId, totalPrice);

            OrderCompleteResponse response = OrderCompleteResponse.builder()
                    .message("주문이 완료되었습니다!")
                    .order_id(orderId)
                    .orders(orders)
                    .total_items(orders.size())
                    .total_price(totalPrice)
                    .packaging(packaging)
                    .phone_number(phoneNumber)
                    .next_step("주문 완료")
                    .build();

            return convertToMap(response);

        } catch (CustomException e) {
            log.warn("주문 완료 처리 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 완료 처리 중 예상치 못한 오류 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.ORDER_SAVE_FAILED);
        }
    }

    // 전화번호 정규화
    public String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String cleanPhone = phoneNumber.replace("-", "").replace(" ", "");

        if (cleanPhone.length() == 11 && cleanPhone.startsWith("010")) {
            return String.format("%s-%s-%s", 
                cleanPhone.substring(0, 3),
                cleanPhone.substring(3, 7),
                cleanPhone.substring(7));
        }

        log.warn("전화번호 정규화 실패 - 입력값: {}", phoneNumber);
        return phoneNumber; // 변환 실패시 원본 반환
    }

    // 전화번호 처리를 위한 세션 상태 검증
    private void validateSessionForPhoneProcessing(String sessionId) {
        try {
            String cartKey = CART_KEY_PREFIX + sessionId;
            
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(cartKey))) {
                log.warn("세션이 존재하지 않음 - sessionId: {}", sessionId);
                throw new CustomException(PhoneErrorCode.SESSION_EXPIRED_FOR_PHONE);
            }

            // 장바구니 데이터 유효성 체크
            String cartJson = redisTemplate.opsForValue().get(cartKey);
            if (cartJson == null) {
                log.warn("장바구니 데이터가 없음 - sessionId: {}", sessionId);
                throw new CustomException(PhoneErrorCode.SESSION_EXPIRED_FOR_PHONE);
            }

            log.debug("세션 상태 검증 통과 - sessionId: {}", sessionId);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("세션 상태 검증 중 오류 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.REDIS_CONNECTION_FAILED);
        }
    }

    // 이미 완료된 주문인지 확인
    private boolean isOrderAlreadyCompleted(String sessionId) {
        try {
            String completedKey = SESSION_COMPLETED_KEY_PREFIX + sessionId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(completedKey));
        } catch (Exception e) {
            log.warn("주문 완료 상태 확인 중 오류 - sessionId: {}", sessionId, e);
            return false; // 오류시 false 반환하여 처리 계속
        }
    }

    // 장바구니 데이터 조회 및 검증
    private Map<String, Object> getCartDataWithValidation(String sessionId) {
        try {
            String cartKey = CART_KEY_PREFIX + sessionId;
            String cartJson = redisTemplate.opsForValue().get(cartKey);

            if (cartJson == null) {
                log.warn("장바구니 데이터가 없음 - sessionId: {}", sessionId);
                throw new CustomException(PhoneErrorCode.SESSION_EXPIRED_FOR_PHONE);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> cartData = objectMapper.readValue(cartJson, Map.class);
            
            log.debug("장바구니 데이터 조회 성공 - sessionId: {}", sessionId);
            return cartData;

        } catch (CustomException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("장바구니 데이터 파싱 실패 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.PHONE_DATA_CORRUPTED);
        } catch (Exception e) {
            log.error("장바구니 데이터 조회 중 오류 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.REDIS_CONNECTION_FAILED);
        }
    }

    // 포장 방식 조회 및 검증
    private String getPackagingTypeWithValidation(String sessionId) {
        try {
            String packagingKey = PACKAGING_KEY_PREFIX + sessionId;
            String packagingJson = redisTemplate.opsForValue().get(packagingKey);
            
            if (packagingJson == null) {
                log.warn("포장 방식이 설정되지 않음 - sessionId: {}", sessionId);
                throw new CustomException(PhoneErrorCode.PACKAGING_TYPE_NOT_SET);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> packagingData = objectMapper.readValue(packagingJson, Map.class);
            String packagingType = (String) packagingData.get("packagingType");
            
            if (packagingType == null || packagingType.trim().isEmpty()) {
                log.warn("포장 방식 데이터가 유효하지 않음 - sessionId: {}", sessionId);
                throw new CustomException(PhoneErrorCode.PACKAGING_TYPE_NOT_SET);
            }
            
            log.debug("포장 방식 조회 성공 - sessionId: {}, packaging: {}", sessionId, packagingType);
            return packagingType;
            
        } catch (CustomException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("포장 방식 데이터 파싱 실패 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.PHONE_DATA_CORRUPTED);
        } catch (Exception e) {
            log.error("포장 방식 조회 중 오류 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.REDIS_CONNECTION_FAILED);
        }
    }

    // Redis에 전화번호 저장
    private void savePhoneNumberToRedis(String sessionId, String normalizedPhone) {
        try {
            String phoneKey = PHONE_KEY_PREFIX + sessionId;
            Map<String, Object> phoneData = new HashMap<>();
            phoneData.put("phone_number", normalizedPhone);
            phoneData.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String phoneJson = objectMapper.writeValueAsString(phoneData);
            redisTemplate.opsForValue().set(phoneKey, phoneJson, CART_EXPIRE_HOURS, TimeUnit.HOURS);

            log.debug("전화번호 Redis 저장 성공 - sessionId: {}", sessionId);

        } catch (JsonProcessingException e) {
            log.error("전화번호 데이터 직렬화 실패 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.PHONE_DATA_SAVE_FAILED);
        } catch (Exception e) {
            log.error("전화번호 Redis 저장 중 오류 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.REDIS_CONNECTION_FAILED);
        }
    }

    // Redis에서 전화번호 조회
    private String getPhoneNumber(String sessionId) {
        try {
            String phoneKey = PHONE_KEY_PREFIX + sessionId;
            String phoneJson = redisTemplate.opsForValue().get(phoneKey);
            
            if (phoneJson == null) {
                log.debug("전화번호가 설정되지 않음 - sessionId: {}", sessionId);
                return null; // 전화번호는 선택사항이므로 null 허용
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> phoneData = objectMapper.readValue(phoneJson, Map.class);
            String phoneNumber = (String) phoneData.get("phone_number");
            
            log.debug("전화번호 조회 성공 - sessionId: {}", sessionId);
            return phoneNumber;
            
        } catch (JsonProcessingException e) {
            log.error("전화번호 데이터 파싱 실패 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.PHONE_DATA_CORRUPTED);
        } catch (Exception e) {
            log.warn("전화번호 조회 중 오류 - sessionId: {}", sessionId, e);
            return null; // 전화번호는 선택사항이므로 오류시 null 반환
        }
    }

    // 세션을 완료 상태로 업데이트
    private void updateSessionToCompleted(String sessionId, int orderId) {
        try {
            String sessionKey = SESSION_COMPLETED_KEY_PREFIX + sessionId;
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("order_id", orderId);
            sessionData.put("completed_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String sessionJson = objectMapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(sessionKey, sessionJson, 5, TimeUnit.MINUTES); // 5분만 유지

            log.debug("세션 완료 상태 업데이트 성공 - sessionId: {}, orderId: {}", sessionId, orderId);

        } catch (Exception e) {
            log.warn("세션 완료 상태 업데이트 실패 - sessionId: {}, orderId: {}", sessionId, orderId, e);
            // 완료 상태 업데이트 실패는 주문 처리 자체를 막지 않음
        }
    }

    // MYSQL에 주문 저장
    private int saveOrderToMySQL(List<CartItemDetail> orders, String packagingType, String phoneNumber) {
        log.info("MySQL 주문 저장 시작 - packaging: {}, phone: {}, orders: {}", 
            packagingType, phoneNumber != null ? phoneNumber.replaceAll("\\d(?=\\d{4})", "*") : "null", orders.size());

        Connection connection = null;
        try {
            // 총 금액 계산
            int totalPrice = orders.stream()
                    .mapToInt(order -> order.getPrice() * order.getQuantity())
                    .sum();

            // MySQL 연결
            connection = dataSource.getConnection();
            connection.setAutoCommit(false); // 트랜잭션 시작

            // 1. orders 테이블에 메인 주문 정보 저장
            String orderSql = """
                INSERT INTO orders (phone_number, total_price, packaging_type, created_at, status)
                VALUES (?, ?, ?, ?, ?)
                """;

            int orderId;
            try (PreparedStatement orderStmt = connection.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                orderStmt.setString(1, phoneNumber);
                orderStmt.setInt(2, totalPrice);
                orderStmt.setString(3, packagingType);
                orderStmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                orderStmt.setString(5, "completed");

                int rowsAffected = orderStmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("주문 생성 실패, 영향받은 행이 없음");
                }

                // 생성된 order_id 가져오기
                try (ResultSet generatedKeys = orderStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        orderId = generatedKeys.getInt(1);
                        log.debug("주문 생성 완료 - orderId: {}", orderId);
                    } else {
                        throw new SQLException("주문 생성 실패, ID 가져오기 실패");
                    }
                }
            }

            // 2. order_items 테이블에 각 메뉴 저장
            String itemSql = """
                INSERT INTO order_items (order_id, menu_id, menu_name, price, quantity, temp)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement itemStmt = connection.prepareStatement(itemSql)) {
                for (CartItemDetail order : orders) {
                    itemStmt.setInt(1, orderId);
                    itemStmt.setLong(2, order.getMenu_id());
                    itemStmt.setString(3, order.getMenu_item());
                    itemStmt.setInt(4, order.getPrice());
                    itemStmt.setInt(5, order.getQuantity());
                    itemStmt.setString(6, order.getTemp());
                    
                    itemStmt.addBatch();
                    
                    log.debug("주문 아이템 추가 - orderId: {}, menuId: {}, menuName: {}, quantity: {}", 
                        orderId, order.getMenu_id(), order.getMenu_item(), order.getQuantity());
                }

                // 배치 실행
                int[] batchResults = itemStmt.executeBatch();
                log.debug("주문 아이템 배치 저장 완료 - 저장된 아이템 수: {}", batchResults.length);
            }

            // 트랜잭션 커밋
            connection.commit();

            log.info("MySQL 주문 저장 성공 - orderId: {}, totalPrice: {}원, 아이템 수: {}", 
                orderId, totalPrice, orders.size());

            return orderId;

        } catch (SQLException e) {
            log.error("MySQL 주문 저장 중 SQL 오류", e);
            
            // 롤백 시도
            if (connection != null) {
                try {
                    connection.rollback();
                    log.info("트랜잭션 롤백 완료");
                } catch (SQLException rollbackEx) {
                    log.error("트랜잭션 롤백 실패", rollbackEx);
                }
            }
            
            throw new CustomException(PhoneErrorCode.ORDER_SAVE_FAILED);
            
        } catch (Exception e) {
            log.error("MySQL 주문 저장 중 예상치 못한 오류", e);
            
            // 롤백 시도
            if (connection != null) {
                try {
                    connection.rollback();
                    log.info("트랜잭션 롤백 완료");
                } catch (SQLException rollbackEx) {
                    log.error("트랜잭션 롤백 실패", rollbackEx);
                }
            }
            
            throw new CustomException(PhoneErrorCode.DATABASE_CONNECTION_FAILED);
            
        } finally {
            // 연결 정리
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // 자동 커밋 복원
                    connection.close();
                    log.debug("MySQL 연결 정리 완료");
                } catch (SQLException e) {
                    log.error("MySQL 연결 정리 중 오류", e);
                }
            }
        }
    }

    // PhoneResponse를 Map으로 변환
    private Map<String, Object> convertToMap(PhoneResponse response) {
        Map<String, Object> map = new HashMap<>();
        map.put("message", response.getMessage());
        map.put("next_step", response.getNext_step());
        return map;
    }

    private Map<String, Object> convertToMap(OrderCompleteResponse response) {
        Map<String, Object> map = new HashMap<>();
        map.put("message", response.getMessage());
        map.put("order_id", response.getOrder_id());
        map.put("orders", response.getOrders());
        map.put("total_items", response.getTotal_items());
        map.put("total_price", response.getTotal_price());
        map.put("packaging", response.getPackaging());
        map.put("phone_number", response.getPhone_number());
        map.put("next_step", response.getNext_step());
        return map;
    }

    public Map<String, Object> completeOrderWithoutPhone(String sessionId) {
        log.info("주문 완료(전화번호 재입력 없음) 처리 시작 - sessionId: {}", sessionId);
        try {
            phoneValidator.validateCompleteOrderRequest(sessionId);
            validateSessionForPhoneProcessing(sessionId);

            if (isOrderAlreadyCompleted(sessionId)) {
                log.warn("이미 완료된 주문 - sessionId: {}", sessionId);
                throw new CustomException(PhoneErrorCode.ORDER_ALREADY_COMPLETED);
            }

            Map<String, Object> cartData = getCartDataWithValidation(sessionId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) cartData.get("items");
            if (items == null || items.isEmpty()) {
                log.warn("주문할 메뉴가 없음 - sessionId: {}", sessionId);
                throw new CustomException(PhoneErrorCode.NO_ITEMS_TO_ORDER);
            }

            List<CartItemDetail> orders = cartUtils.convertToCartItemDetails(items);
            int totalPrice = cartUtils.calculateTotalPrice(items);
            String packaging = getPackagingTypeWithValidation(sessionId);

            String phoneNumber = getPhoneNumber(sessionId);
            if (phoneNumber == null || phoneNumber.isBlank()) {
                throw new CustomException(PhoneErrorCode.PHONE_NUMBER_REQUIRED);
            }

            int orderId = saveOrderToMySQL(orders, packaging, phoneNumber);
            updateSessionToCompleted(sessionId, orderId);

            log.info("주문 완료 처리 성공 - sessionId: {}, orderId: {}, totalPrice: {}원",
                sessionId, orderId, totalPrice);

            OrderCompleteResponse response = OrderCompleteResponse.builder()
                .message("주문이 완료되었습니다!")
                .order_id(orderId)
                .orders(orders)
                .total_items(orders.size())
                .total_price(totalPrice)
                .packaging(packaging)
                .phone_number(phoneNumber)
                .next_step("주문 완료")
                .build();

            return convertToMap(response);

        } catch (CustomException e) {
            log.warn("주문 완료 처리 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 완료 처리 중 예상치 못한 오류 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.ORDER_SAVE_FAILED);
        }
    }

    public Map<String, Object> savePhone(String sessionId, String phone) {
        log.info("전화번호 저장 처리 시작 - sessionId: {}, phone(masked): {}",
            sessionId, phone != null ? phone.replaceAll("\\d(?=\\d{4})", "*") : "null");

        try {
            // 1) 요청/형식 검증 (세션ID 형식 + 전화번호 형식만 검사)
            PhoneInputRequest req = PhoneInputRequest.builder()
                .phone_number(phone)
                .build();
            phoneValidator.validatePhoneInputRequest(sessionId, req);

            // 2) 장바구니 키 없으면 생성 (여기서 세션 실체화)
            ensureCartKeyExists(sessionId);

            // 3) 전화번호 정규화 + Redis 저장
            String normalizedPhone = normalizePhoneNumber(phone);
            savePhoneNumberToRedis(sessionId, normalizedPhone);

            // 4) 응답
            PhoneResponse response = PhoneResponse.builder()
                .message("전화번호가 저장되었습니다.")
                .next_step("주문 완료")
                .build();

            log.info("전화번호 저장 처리 완료 - sessionId: {}", sessionId);
            return convertToMap(response);

        } catch (CustomException e) {
            log.warn("전화번호 저장 처리 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("전화번호 저장 처리 중 예상치 못한 오류 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.PHONE_DATA_SAVE_FAILED);
        }
    }

    private void ensureCartKeyExists(String sessionId) {
        try {
            String cartKey = CART_KEY_PREFIX + sessionId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cartKey))) {
                return;
            }

            Map<String, Object> emptyCart = cartUtils.createEmptyCart();
            emptyCart.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String cartJson = objectMapper.writeValueAsString(emptyCart);
            redisTemplate.opsForValue().set(cartKey, cartJson, CART_EXPIRE_HOURS, TimeUnit.HOURS);

            log.info("장바구니 키가 없어 새로 생성 - sessionId: {}", sessionId);

        } catch (Exception e) {
            log.error("장바구니 키 생성 실패 - sessionId: {}", sessionId, e);
            throw new CustomException(PhoneErrorCode.REDIS_CONNECTION_FAILED);
        }
    }
}
