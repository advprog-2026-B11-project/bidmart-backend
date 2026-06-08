package com.example.bidmart.order.state;

import com.example.bidmart.order.model.OrderStatus;

public class DeliveredState implements OrderState {
    @Override
    public boolean canTransitionTo(OrderStatus nextStatus) {
        return nextStatus == OrderStatus.DISPUTED;
    }
}
