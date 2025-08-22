package likelion.kitalk.touch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  // 세션 키 prefix
  private static final String SESSION_KEY_PREFIX = "session:";

  // 새 세션 생성
  public String createSession(int expireMinutes) {
    String sessionId = UUID.randomUUID().toString();

    try {
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime expiresAt = now.plusMinutes(expireMinutes);

      // 세션 데이터 구조
      Map<String, Object> sessionData = new HashMap<>();
      sessionData.put("created_at", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      sessionData.put("expires_at", expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      sessionData.put("step", "started");

      // data 객체
      Map<String, Object> data = new HashMap<>();
      data.put("menu_item", null);
      data.put("quantity", null);
      data.put("packaging_type", null);
      sessionData.put("data", data);

      // JSON으로 직렬화 후 Redis에 저장
      String sessionJson = objectMapper.writeValueAsString(sessionData);
      String sessionKey = SESSION_KEY_PREFIX + sessionId;

      redisTemplate.opsForValue().set(sessionKey, sessionJson, expireMinutes * 60L, TimeUnit.SECONDS);

      log.info("세션 생성 완료: {}", sessionId);
      return sessionId;

    } catch (JsonProcessingException e) {
      log.error("세션 생성 실패: {}", e.getMessage());
      throw new RuntimeException("세션 생성 실패", e);
    }
  }

  // 세션 조회
  public Map<String, Object> getSession(String sessionId) {
    try {
      String sessionKey = SESSION_KEY_PREFIX + sessionId;
      String sessionJson = redisTemplate.opsForValue().get(sessionKey);

      if (sessionJson == null) {
        log.warn("세션 없음 또는 만료: {}", sessionId);
        return null;
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> sessionData = objectMapper.readValue(sessionJson, Map.class);
      log.debug("세션 조회 성공: {}", sessionId);
      return sessionData;

    } catch (JsonProcessingException e) {
      log.error("세션 조회 실패: {}", e.getMessage());
      return null;
    }
  }

  // 세션 삭제
  public boolean deleteSession(String sessionId) {
    try {
      String sessionKey = SESSION_KEY_PREFIX + sessionId;
      Boolean result = redisTemplate.delete(sessionKey);

      if (Boolean.TRUE.equals(result)) {
        log.info("세션 삭제 완료: {}", sessionId);
        return true;
      } else {
        log.warn("삭제할 세션 없음: {}", sessionId);
        return false;
      }

    } catch (Exception e) {
      log.error("세션 삭제 실패: {}", e.getMessage());
      return false;
    }
  }

  // 세션 만료 시간 연장
  public boolean extendSession(String sessionId, int expireMinutes) {
    try {
      String sessionKey = SESSION_KEY_PREFIX + sessionId;
      Boolean result = redisTemplate.expire(sessionKey, expireMinutes * 60L, TimeUnit.SECONDS);

      if (Boolean.TRUE.equals(result)) {
        log.info("세션 만료시간 연장: {} (+{}분)", sessionId, expireMinutes);
        return true;
      } else {
        log.warn("연장할 세션 없음: {}", sessionId);
        return false;
      }

    } catch (Exception e) {
      log.error("세션 연장 실패: {}", e.getMessage());
      return false;
    }
  }

  // 모든 세션 조회
  public Map<String, Map<String, Object>> getAllSessions() {
    try {
      Set<String> sessionKeys = redisTemplate.keys(SESSION_KEY_PREFIX + "*");
      Map<String, Map<String, Object>> sessions = new HashMap<>();

      if (sessionKeys != null) {
        for (String key : sessionKeys) {
          String sessionId = key.replace(SESSION_KEY_PREFIX, "");
          Map<String, Object> sessionData = getSession(sessionId);
          if (sessionData != null) {
            sessions.put(sessionId, sessionData);
          }
        }
      }

      log.info("전체 세션 조회: {}개", sessions.size());
      return sessions;

    } catch (Exception e) {
      log.error("전체 세션 조회 실패: {}", e.getMessage());
      return new HashMap<>();
    }
  }

  // 만료된 세션 정리
  public int cleanupExpiredSessions() {
    try {
      // Redis TTL로 자동 만료되므로 실제로는 불필요
      // 하지만 수동 정리가 필요한 경우를 위해 구현
      Set<String> sessionKeys = redisTemplate.keys(SESSION_KEY_PREFIX + "*");
      int expiredCount = 0;

      if (sessionKeys != null) {
        for (String key : sessionKeys) {
          Long ttl = redisTemplate.getExpire(key);
          if (ttl != null && ttl == -2) {  // 키가 존재하지 않음 (만료됨)
            expiredCount++;
          }
        }
      }

      log.info("만료된 세션 정리 완료: {}개", expiredCount);
      return expiredCount;

    } catch (Exception e) {
      log.error("세션 정리 실패: {}", e.getMessage());
      return 0;
    }
  }

  // 세션 통계 정보
  public Map<String, Object> getSessionStats() {
    try {
      Set<String> sessionKeys = redisTemplate.keys(SESSION_KEY_PREFIX + "*");
      int totalSessions = sessionKeys != null ? sessionKeys.size() : 0;

      // 단계별 세션 수 계산
      Map<String, Integer> stepCounts = new HashMap<>();
      Map<String, Map<String, Object>> sessions = getAllSessions();

      for (Map<String, Object> sessionData : sessions.values()) {
        String step = (String) sessionData.getOrDefault("step", "unknown");
        stepCounts.put(step, stepCounts.getOrDefault(step, 0) + 1);
      }

      // Redis 정보
      Map<String, Object> redisInfo = new HashMap<>();
      redisInfo.put("connected", true);  // 연결되어 있다면 이 메서드가 실행됨
      // memory usage는 Java에서 직접 조회가 복잡하므로 생략

      Map<String, Object> stats = new HashMap<>();
      stats.put("total_sessions", totalSessions);
      stats.put("step_distribution", stepCounts);
      stats.put("redis_info", redisInfo);

      log.info("세션 통계: {}", stats);
      return stats;

    } catch (Exception e) {
      log.error("세션 통계 조회 실패: {}", e.getMessage());
      Map<String, Object> errorStats = new HashMap<>();
      errorStats.put("error", e.getMessage());
      return errorStats;
    }
  }

  // 세션 유효성 확인
  public boolean isValidSession(String sessionId) {
    if (sessionId == null || sessionId.trim().isEmpty()) {
      return false;
    }

    String sessionKey = SESSION_KEY_PREFIX + sessionId;
    return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
  }
}