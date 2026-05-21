package com.example.bidmart.bidding.controller;

import com.example.bidmart.bidding.exception.BidConflictException;
import com.example.bidmart.bidding.exception.BidValidationException;
import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.common.exception.ErrorResponse;
import com.example.bidmart.wallet.exception.InsufficientBalanceException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = {BidController.class})
public class BidControllerAdvice {

    @ExceptionHandler(BidConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(BidConflictException ex) {
        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(BidValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(BidValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, "INSUFFICIENT_BALANCE", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), error, message, Instant.now().toString()));
    }
}
