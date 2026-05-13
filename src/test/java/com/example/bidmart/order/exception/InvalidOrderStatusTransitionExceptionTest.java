package com.example.bidmart.order.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvalidOrderStatusTransitionExceptionTest {

    @Test
    void testExceptionMessage() {
        InvalidOrderStatusTransitionException exception = 
                new InvalidOrderStatusTransitionException("CREATED", "DELIVERED");
                
        assertTrue(exception.getMessage().contains("dari CREATED ke DELIVERED"));
    }
}