package com.example.bidmart.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimitFilterTest {

    private AuthRateLimitFilter filter;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        filter = new AuthRateLimitFilter(
                meterRegistry,
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/verify-mfa",
                5, 60, 
                2, 600,
                5, 60
        );
    }

    @ParameterizedTest(name = "[{index}] Method: {0}, URI: {1}")
    @CsvSource({
            "POST, /api/auth/login",
            "POST, /api/auth/register",
            "POST, /api/auth/verify-mfa",
            "GET,  /api/listings"
    })
    void doFilterInternal_allowsRequestsWithinLimit(String method, String requestURI) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(requestURI);
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternal_blocksRequestWhenLimitExceeded() throws ServletException, IOException {
        String clientIp = "192.168.1.1";

        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod("POST");
            request.setRequestURI("/api/auth/register");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            MockFilterChain filterChain = new MockFilterChain(); 

            filter.doFilterInternal(request, response, filterChain);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletRequest blockedRequest = new MockHttpServletRequest();
        blockedRequest.setMethod("POST");
        blockedRequest.setRequestURI("/api/auth/register");
        blockedRequest.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        MockFilterChain blockedFilterChain = new MockFilterChain();

        filter.doFilterInternal(blockedRequest, blockedResponse, blockedFilterChain);

        assertEquals(429, blockedResponse.getStatus());
        assertEquals("application/json", blockedResponse.getContentType());
        assertTrue(blockedResponse.getContentAsString().contains("Too many requests"));
        assertTrue(blockedResponse.containsHeader("Retry-After"));

        assertEquals(1.0, meterRegistry.counter("bidmart.auth.ratelimit.blocked", "endpoint", "register").count());
    }

    @Test
    void doFilterInternal_usesXForwardedForHeader() throws ServletException, IOException {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod("POST");
            request.setRequestURI("/api/auth/register");
            request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.2"); 
            request.setRemoteAddr("127.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            MockFilterChain filterChain = new MockFilterChain();

            filter.doFilterInternal(request, response, filterChain);

            if (i < 2) {
                assertEquals(200, response.getStatus());
            } else {
                assertEquals(429, response.getStatus());
            }
        }
    }
}