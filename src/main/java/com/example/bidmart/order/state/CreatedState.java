package com.example.bidmart.order.state;

import com.example.bidmart.order.model.OrderStatus;

public class CreatedState implements OrderState {
    @Override
    public boolean canTransitionTo(OrderStatus nextStatus) {
        return nextStatus == OrderStatus.SHIPPED || nextStatus == OrderStatus.CANCELLED;
    }
}
