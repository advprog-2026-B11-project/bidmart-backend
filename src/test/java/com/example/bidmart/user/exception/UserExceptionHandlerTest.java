package com.example.bidmart.user.exception;

import com.example.bidmart.common.exception.ErrorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserExceptionHandlerTest {

    private final UserExceptionHandler handler = new UserExceptionHandler();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handleIllegalArgument_shouldReturnBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(new IllegalArgumentException("Error Input"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error Input", response.getBody().message());
    }

    @Test
    void handleUsernameNotFound_shouldReturnNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleUsernameNotFound(new UsernameNotFoundException("User missing"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User missing", response.getBody().message());
    }

    @Test
    void handleAccessDenied_withAuthenticatedUser_shouldReturnForbidden() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("Denied"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Denied", response.getBody().message());
    }

    @Test
    void handleAccessDenied_withAnonymousUser_shouldReturnUnauthorized() {
        SecurityContextHolder.clearContext();
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("Denied"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Authentication required.", response.getBody().message());
    }

    @Test
    void handleUnexpected_shouldReturnInternalServerError() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new Exception("System failure"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("System failure", response.getBody().message());
    }

    @Test
    void handleValidation_shouldReturnBadRequest() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "must not be blank");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("field: must not be blank", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }
}
