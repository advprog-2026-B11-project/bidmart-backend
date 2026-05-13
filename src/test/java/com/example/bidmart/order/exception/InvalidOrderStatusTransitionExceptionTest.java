package com.example.bidmart.order.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvalidOrderStatusTransitionExceptionTest {

    @Test
    void testExceptionMessageFormatting() {
        String currentStatus = "CREATED";
        String nextStatus = "DELIVERED";
        
        InvalidOrderStatusTransitionException exception = 
                new InvalidOrderStatusTransitionException(currentStatus, nextStatus);
        
        assertTrue(exception.getMessage().contains(currentStatus));
        assertTrue(exception.getMessage().contains(nextStatus));
        assertEquals("Transisi status tidak valid: dari CREATED ke DELIVERED", exception.getMessage());
    }
}