package com.example.bidmart.order.state;

import com.example.bidmart.order.model.OrderStatus;

public interface OrderState {
    boolean canTransitionTo(OrderStatus nextStatus);
}
