package likelion.kitalk.global.jwt;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.NoSuchElementException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    try {
      // 현재는 JWT 검증 로직 없이 그냥 통과
      // 나중에 실제 인증이 필요할 때 JWT 검증 로직 추가

      filterChain.doFilter(request, response);

    } catch (Exception e) {
      log.error("JWT 필터 처리 중 예외 발생", e);
      handleException(e, request, response);
    }
  }

  private void handleException(
      Throwable e, HttpServletRequest request, HttpServletResponse response) throws IOException {
    int statusCode;
    String message = e.getMessage();

    if (e instanceof IllegalArgumentException || e instanceof NoSuchElementException) {
      statusCode = HttpServletResponse.SC_NOT_FOUND; // 404
      if (message == null)
        message = "요청한 리소스를 찾을 수 없습니다.";
    } else if (e instanceof IllegalStateException) {
      statusCode = HttpServletResponse.SC_BAD_REQUEST; // 400
      if (message == null)
        message = "잘못된 요청입니다.";
    } else if (e instanceof AccessDeniedException) {
      statusCode = HttpServletResponse.SC_FORBIDDEN; // 403
      if (message == null)
        message = "접근 권한이 없습니다.";
    } else if (e instanceof AuthenticationException) {
      statusCode = HttpServletResponse.SC_UNAUTHORIZED; // 401
      if (message == null)
        message = "인증에 실패했습니다.";
    } else if (e instanceof ExpiredJwtException) {
      statusCode = HttpServletResponse.SC_UNAUTHORIZED; // 401
      message = "만료된 토큰입니다.";
    } else if (e instanceof SignatureException || e instanceof MalformedJwtException) {
      statusCode = HttpServletResponse.SC_UNAUTHORIZED; // 401
      message = "유효하지 않은 토큰입니다.";
    } else {
      statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR; // 500
      message = "서버 내부 오류가 발생했습니다.";
    }

    log.error(
        "필터 처리 중 예외 발생 - URI: {}, Method: {}, Error: {}, ErrorType: {}",
        request.getRequestURI(),
        request.getMethod(),
        e.getMessage(),
        e.getClass().getSimpleName());

    sendErrorResponse(response, statusCode, message);
  }

  private void sendErrorResponse(HttpServletResponse response, int statusCode, String message)
      throws IOException {
    response.setStatus(statusCode);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(String.format("{\"isSuccess\":false,\"message\":\"%s\"}", message));
  }

  // 특정 경로는 필터 적용 제외 (예: 로그인, 회원가입)
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/api/auth/")
        || path.startsWith("/api/menu/")
        || path.equals("/juso/callback")
        || path.equals("/users/refresh-token")
        || path.startsWith("/api/public/")  // 🔧 추가: 공개 API
        || path.startsWith("/health")       // 🔧 추가: 헬스체크
        || path.startsWith("/actuator/");   // 🔧 추가: 액추에이터
  }
}