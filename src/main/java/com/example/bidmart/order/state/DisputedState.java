package com.example.bidmart.order.state;

import com.example.bidmart.order.model.OrderStatus;

public class DisputedState implements OrderState {
    @Override
    public boolean canTransitionTo(OrderStatus nextStatus) {
        return nextStatus == OrderStatus.DELIVERED || nextStatus == OrderStatus.CANCELLED;
    }
}
