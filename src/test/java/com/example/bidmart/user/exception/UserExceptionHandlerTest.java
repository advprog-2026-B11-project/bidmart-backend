package com.example.bidmart.user.exception;

import com.example.bidmart.user.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

    @Test
    void handleIllegalArgument_shouldReturnBadRequest() {
        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalArgument(new IllegalArgumentException("Error Input"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error Input", response.getBody().message());
    }

    @Test
    void handleUsernameNotFound_shouldReturnNotFound() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUsernameNotFound(new UsernameNotFoundException("User missing"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User missing", response.getBody().message());
    }

    @Test
    void handleAccessDenied_shouldReturnForbidden() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("Denied"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Denied", response.getBody().message());
    }

    @Test
    void handleUnexpected_shouldReturnInternalServerError() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(new Exception("System failure"));
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

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("field: must not be blank", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }
}