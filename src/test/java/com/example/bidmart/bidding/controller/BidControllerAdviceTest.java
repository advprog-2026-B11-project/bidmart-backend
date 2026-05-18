package com.example.bidmart.bidding.controller;

import com.example.bidmart.bidding.exception.BidConflictException;
import com.example.bidmart.bidding.exception.BidValidationException;
import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.common.exception.ErrorResponse;
import com.example.bidmart.wallet.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class BidControllerAdviceTest {

    private final BidControllerAdvice advice = new BidControllerAdvice();

    @Test
    void handleConflict_returns409WithConflictError() {
        ResponseEntity<ErrorResponse> response = advice.handleConflict(new BidConflictException("conflict"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("CONFLICT");
        assertThat(response.getBody().message()).isEqualTo("conflict");
    }

    @Test
    void handleNotFound_returns404WithNotFoundError() {
        ResponseEntity<ErrorResponse> response = advice.handleNotFound(new ResourceNotFoundException("not found"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
    }

    @Test
    void handleValidation_returns400WithBadRequestError() {
        ResponseEntity<ErrorResponse> response = advice.handleValidation(new BidValidationException("invalid"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void handleInsufficientBalance_returns422WithInsufficientBalanceError() {
        ResponseEntity<ErrorResponse> response =
                advice.handleInsufficientBalance(new InsufficientBalanceException("saldo kurang"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody().status()).isEqualTo(422);
        assertThat(response.getBody().error()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(response.getBody().message()).isEqualTo("saldo kurang");
    }

    @Test
    void handleAccessDenied_returns403WithForbiddenError() {
        ResponseEntity<ErrorResponse> response = advice.handleAccessDenied(new AccessDeniedException("forbidden"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().error()).isEqualTo("FORBIDDEN");
    }
}
