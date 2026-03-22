package com.example.restaurant.service;

import com.example.restaurant.entity.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastOrderCreated(Order order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "ORDER_CREATED");
        payload.put("orderId", order.getId());
        payload.put("tableNumber", order.getTableNumber());
        payload.put("status", order.getStatus());
        payload.put("message", "New order for " + order.getTableNumber());
        messagingTemplate.convertAndSend("/topic/orders", payload);
        messagingTemplate.convertAndSend("/topic/roles/KITCHEN", payload);
        messagingTemplate.convertAndSend("/topic/roles/WAITER", payload);
        messagingTemplate.convertAndSend("/topic/roles/CASHIER", payload);
        messagingTemplate.convertAndSend("/topic/tables/" + order.getTableNumber(), payload);
    }

    public void broadcastOrderUpdated(Order order, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "ORDER_UPDATED");
        payload.put("orderId", order.getId());
        payload.put("tableNumber", order.getTableNumber());
        payload.put("status", order.getStatus());
        payload.put("message", message);
        messagingTemplate.convertAndSend("/topic/orders", payload);
        messagingTemplate.convertAndSend("/topic/roles/CUSTOMER", payload);
        messagingTemplate.convertAndSend("/topic/roles/WAITER", payload);
        messagingTemplate.convertAndSend("/topic/roles/KITCHEN", payload);
        messagingTemplate.convertAndSend("/topic/roles/CASHIER", payload);
        messagingTemplate.convertAndSend("/topic/tables/" + order.getTableNumber(), payload);
        messagingTemplate.convertAndSend("/topic/order/" + order.getId(), payload);
    }
}
