package com.example.bidmart.order.model;

import com.example.bidmart.order.state.*;

public enum OrderStatus {
    DRAFT(new DraftState()),
    CREATED(new CreatedState()),
    SHIPPED(new ShippedState()),
    DELIVERED(new DeliveredState()),
    DISPUTED(new DisputedState()),
    CANCELLED(new CancelledState());

    private final OrderState state;

    OrderStatus(OrderState state) {
        this.state = state;
    }

    public boolean canTransitionTo(OrderStatus nextStatus) {
        if (this == nextStatus) return true;
        return this.state.canTransitionTo(nextStatus);
    }
    
    public static OrderStatus fromString(String statusStr) {
        try {
            return OrderStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Status Order tidak valid: " + statusStr);
        }
    }
}