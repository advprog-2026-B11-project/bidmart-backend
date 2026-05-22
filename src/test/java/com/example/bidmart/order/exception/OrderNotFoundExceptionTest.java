package com.example.bidmart.order.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderNotFoundExceptionTest {

    @Test
    void testExceptionMessage() {
        String errorMessage = "Pesanan tidak ditemukan dengan ID: 123";
        OrderNotFoundException exception = new OrderNotFoundException(errorMessage);
        
        assertEquals(errorMessage, exception.getMessage());
    }
}