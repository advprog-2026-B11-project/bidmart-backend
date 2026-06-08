package com.example.bidmart.order.state;

import com.example.bidmart.order.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateTest {

    @Test
    void draftState_transitions() {
        DraftState state = new DraftState();
        assertTrue(state.canTransitionTo(OrderStatus.CREATED));
        assertTrue(state.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(state.canTransitionTo(OrderStatus.SHIPPED));
    }

    @Test
    void createdState_transitions() {
        CreatedState state = new CreatedState();
        assertTrue(state.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(state.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(state.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void shippedState_transitions() {
        ShippedState state = new ShippedState();
        assertTrue(state.canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(state.canTransitionTo(OrderStatus.DISPUTED));
        assertFalse(state.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void deliveredState_transitions() {
        DeliveredState state = new DeliveredState();
        assertTrue(state.canTransitionTo(OrderStatus.DISPUTED));
        assertFalse(state.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void disputedState_transitions() {
        DisputedState state = new DisputedState();
        assertTrue(state.canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(state.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(state.canTransitionTo(OrderStatus.SHIPPED));
    }

    @Test
    void cancelledState_transitions() {
        CancelledState state = new CancelledState();
        for (OrderStatus status : OrderStatus.values()) {
            assertFalse(state.canTransitionTo(status));
        }
    }
}
