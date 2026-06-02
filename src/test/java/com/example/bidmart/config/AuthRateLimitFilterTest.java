package com.example.bidmart.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthRateLimitFilterTest {

    private AuthRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthRateLimitFilter(
                new SimpleMeterRegistry(),
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/verify-mfa",
                5, 60, 3, 600, 5, 60
        );
    }

    @ParameterizedTest(name = "[{index}] Method: {0}, URI: {1}")
    @CsvSource({
            "POST, /api/auth/login",
            "POST, /api/auth/register",
            "POST, /api/auth/verify-mfa",
            "GET,  /api/listings"
    })
    void doFilterInternal_allowsRequests(String method, String requestURI) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(requestURI);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }
}