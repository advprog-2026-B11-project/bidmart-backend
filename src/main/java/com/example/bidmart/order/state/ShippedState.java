package com.example.bidmart.order.state;

import com.example.bidmart.order.model.OrderStatus;

public class ShippedState implements OrderState {
    @Override
    public boolean canTransitionTo(OrderStatus nextStatus) {
        return nextStatus == OrderStatus.DELIVERED || nextStatus == OrderStatus.DISPUTED;
    }
}
