package com.example.bidmart.order.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void canTransitionTo_validTransitions() {
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DISPUTED));
        assertTrue(OrderStatus.DISPUTED.canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CREATED));
    }

    @Test
    void canTransitionTo_invalidTransitions() {
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CREATED));
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.DRAFT));
    }

    @Test
    void fromString_validString() {
        assertEquals(OrderStatus.CREATED, OrderStatus.fromString("CREATED"));
        assertEquals(OrderStatus.SHIPPED, OrderStatus.fromString("shipped"));
    }

    @Test
    void fromString_invalidString_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.fromString("INVALID_STATUS"));
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.fromString(null));
    }
}