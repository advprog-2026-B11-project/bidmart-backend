package com.example.bidmart.order.model;

import java.util.Arrays;
import java.util.List;

public enum OrderStatus {
    DRAFT,
    CREATED,
    SHIPPED,
    DELIVERED,
    DISPUTED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus nextStatus) {
        if (this == nextStatus) return true;
        
        switch (this) {
            case DRAFT:
                return nextStatus == CREATED || nextStatus == CANCELLED;
            case CREATED:
                return nextStatus == SHIPPED || nextStatus == CANCELLED;
            case SHIPPED:
                return nextStatus == DELIVERED || nextStatus == DISPUTED;
            case DELIVERED:
                return nextStatus == DISPUTED;
            case DISPUTED:
                return nextStatus == DELIVERED || nextStatus == CANCELLED;
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }
    
    public static OrderStatus fromString(String statusStr) {
        try {
            return OrderStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Status Order tidak valid: " + statusStr);
        }
    }
}