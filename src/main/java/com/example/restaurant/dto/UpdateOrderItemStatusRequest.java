package com.example.restaurant.dto;

import com.example.restaurant.entity.OrderItemStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateOrderItemStatusRequest {

    @NotNull(message = "Status is required")
    private OrderItemStatus status;

    public OrderItemStatus getStatus() {
        return status;
    }

    public void setStatus(OrderItemStatus status) {
        this.status = status;
    }
}
