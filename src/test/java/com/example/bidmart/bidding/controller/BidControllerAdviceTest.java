package com.example.bidmart.bidding.controller;

import com.example.bidmart.bidding.dto.ErrorResponse;
import com.example.bidmart.bidding.exception.BidConflictException;
import com.example.bidmart.bidding.exception.BidValidationException;
import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.wallet.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class BidControllerAdviceTest {

    private final BidControllerAdvice advice = new BidControllerAdvice();

    @Test
    void handleConflict_returns409WithConflictCode() {
        ResponseEntity<ErrorResponse> response = advice.handleConflict(new BidConflictException("conflict"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("CONFLICT");
        assertThat(response.getBody().message()).isEqualTo("conflict");
    }

    @Test
    void handleNotFound_returns404WithNotFoundCode() {
        ResponseEntity<ErrorResponse> response = advice.handleNotFound(new ResourceNotFoundException("not found"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void handleValidation_returns400WithBadRequestCode() {
        ResponseEntity<ErrorResponse> response = advice.handleValidation(new BidValidationException("invalid"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void handleInsufficientBalance_returns422WithInsufficientBalanceCode() {
        ResponseEntity<ErrorResponse> response =
                advice.handleInsufficientBalance(new InsufficientBalanceException("saldo kurang"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody().code()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(response.getBody().message()).isEqualTo("saldo kurang");
    }

    @Test
    void handleAccessDenied_returns403WithForbiddenCode() {
        ResponseEntity<ErrorResponse> response = advice.handleAccessDenied(new AccessDeniedException("forbidden"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void handleUnexpected_returns500WithInternalErrorCode() {
        ResponseEntity<ErrorResponse> response = advice.handleUnexpected(new RuntimeException("unexpected"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
