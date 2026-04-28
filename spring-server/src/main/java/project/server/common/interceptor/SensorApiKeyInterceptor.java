package project.server.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import project.server.common.exception.UserException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static project.server.common.response.status.BaseExceptionResponseStatus.INVALID_SENSOR_API_KEY;

@Slf4j
@Component
public class SensorApiKeyInterceptor implements HandlerInterceptor {

    @Value("${app.sensor-api-key:}")
    private String expectedKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/sensors")) {
            return true;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (expectedKey == null || expectedKey.isBlank()) {
            log.warn("[SensorApiKeyInterceptor] SENSOR_API_KEY not set; ingestion is unauthenticated.");
            return true;
        }
        String provided = request.getHeader("X-Api-Key");
        if (!expectedKey.equals(provided)) {
            throw new UserException(INVALID_SENSOR_API_KEY);
        }
        return true;
    }
}
