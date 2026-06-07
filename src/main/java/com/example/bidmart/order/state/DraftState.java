package com.example.bidmart.order.state;

import com.example.bidmart.order.model.OrderStatus;

public class DraftState implements OrderState {
    @Override
    public boolean canTransitionTo(OrderStatus nextStatus) {
        return nextStatus == OrderStatus.CREATED || nextStatus == OrderStatus.CANCELLED;
    }
}
