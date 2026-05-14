package com.example.bidmart.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    private final FixedWindowRateLimiter loginLimiter;
    private final FixedWindowRateLimiter registerLimiter;

    public AuthRateLimitFilter(
            @Value("${app.rate-limit.login.max-requests:5}") int loginMaxRequests,
            @Value("${app.rate-limit.login.window-seconds:60}") long loginWindowSeconds,
            @Value("${app.rate-limit.register.max-requests:3}") int registerMaxRequests,
            @Value("${app.rate-limit.register.window-seconds:600}") long registerWindowSeconds) {
        this.loginLimiter = new FixedWindowRateLimiter(loginMaxRequests, Duration.ofSeconds(loginWindowSeconds));
        this.registerLimiter = new FixedWindowRateLimiter(registerMaxRequests, Duration.ofSeconds(registerWindowSeconds));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (HttpMethod.POST.matches(request.getMethod())) {
            String path = request.getRequestURI();
            if (LOGIN_PATH.equals(path)) {
                handleRateLimit(request, response, filterChain, loginLimiter);
                return;
            }
            if (REGISTER_PATH.equals(path)) {
                handleRateLimit(request, response, filterChain, registerLimiter);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void handleRateLimit(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            FixedWindowRateLimiter limiter) throws IOException, ServletException {
        String key = resolveClientKey(request);
        RateLimitDecision decision = limiter.tryConsume(key);
        if (!decision.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"message\":\"Too many requests. Please try again later.\",\"timestamp\":\""
                            + Instant.now()
                            + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class FixedWindowRateLimiter {
        private final int maxRequests;
        private final long windowMillis;
        private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

        private FixedWindowRateLimiter(int maxRequests, Duration window) {
            this.maxRequests = Math.max(1, maxRequests);
            this.windowMillis = Math.max(1000, window.toMillis());
        }

        private RateLimitDecision tryConsume(String key) {
            Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(maxRequests, windowMillis));
            return bucket.tryConsume();
        }
    }

    private static final class Bucket {
        private final int maxRequests;
        private final long windowMillis;
        private int remaining;
        private long windowStartMillis;

        private Bucket(int maxRequests, long windowMillis) {
            this.maxRequests = maxRequests;
            this.windowMillis = windowMillis;
            this.remaining = maxRequests;
            this.windowStartMillis = System.currentTimeMillis();
        }

        private synchronized RateLimitDecision tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStartMillis >= windowMillis) {
                windowStartMillis = now;
                remaining = maxRequests;
            }

            if (remaining > 0) {
                remaining--;
                return RateLimitDecision.allow();
            }

            long retryAfterSeconds = Math.max(1, (windowMillis - (now - windowStartMillis) + 999) / 1000);
            return RateLimitDecision.block(retryAfterSeconds);
        }
    }

    private record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
        private static RateLimitDecision allow() {
            return new RateLimitDecision(true, 0);
        }

        private static RateLimitDecision block(long retryAfterSeconds) {
            return new RateLimitDecision(false, retryAfterSeconds);
        }
    }
}
