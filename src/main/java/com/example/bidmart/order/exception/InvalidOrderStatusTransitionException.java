package com.example.bidmart.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(String currentStatus, String nextStatus) {
        super(String.format("Transisi status tidak valid: dari %s ke %s", currentStatus, nextStatus));
    }
}