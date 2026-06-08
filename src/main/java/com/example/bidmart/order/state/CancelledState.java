package com.example.bidmart.order.state;

import com.example.bidmart.order.model.OrderStatus;

public class CancelledState implements OrderState {
    @Override
    public boolean canTransitionTo(OrderStatus nextStatus) {
        return false;
    }
}
