package com.example.bidmart.order.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void canTransitionTo_validTransitions() {
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DISPUTED));
        assertTrue(OrderStatus.DISPUTED.canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(OrderStatus.DISPUTED.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CREATED));
        assertTrue(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.DISPUTED));
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.CREATED));
    }

    @Test
    void canTransitionTo_invalidTransitions() {
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.DRAFT));
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.DISPUTED));
        
        assertFalse(OrderStatus.DRAFT.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.DRAFT.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.DRAFT.canTransitionTo(OrderStatus.DISPUTED));
        
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CREATED));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DRAFT));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));
        
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CREATED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.DRAFT));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED));
        
        assertFalse(OrderStatus.DISPUTED.canTransitionTo(OrderStatus.CREATED));
        assertFalse(OrderStatus.DISPUTED.canTransitionTo(OrderStatus.DRAFT));
        assertFalse(OrderStatus.DISPUTED.canTransitionTo(OrderStatus.SHIPPED));
        
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CREATED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DRAFT));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DISPUTED));
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