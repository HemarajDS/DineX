package com.example.restaurant.controller;

import com.example.restaurant.dto.BillSummaryRequest;
import com.example.restaurant.dto.BillSummaryResponse;
import com.example.restaurant.dto.CreateOrderRequest;
import com.example.restaurant.dto.OrderResponse;
import com.example.restaurant.dto.TableBillResponse;
import com.example.restaurant.dto.UpdateBillingStatusRequest;
import com.example.restaurant.dto.UpdateOrderItemStatusRequest;
import com.example.restaurant.dto.UpdateOrderStatusRequest;
import com.example.restaurant.entity.User;
import com.example.restaurant.service.AccessControlService;
import com.example.restaurant.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final AccessControlService accessControlService;

    public OrderController(OrderService orderService, AccessControlService accessControlService) {
        this.orderService = orderService;
        this.accessControlService = accessControlService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, user));
    }

    @PostMapping("/guest")
    public ResponseEntity<OrderResponse> createGuestOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createGuestOrder(request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        return ResponseEntity.ok(orderService.getOrders(user));
    }

    @GetMapping("/open")
    public ResponseEntity<OrderResponse> getActiveOrderForTable(
            @RequestParam String tableNumber,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        return ResponseEntity.ok(orderService.getActiveOrderForTable(tableNumber, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable String id,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        return ResponseEntity.ok(orderService.getOrderById(id, user));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus(), user));
    }

    @PatchMapping("/{orderId}/items/{itemId}/status")
    public ResponseEntity<OrderResponse> updateOrderItemStatus(
            @PathVariable String orderId,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateOrderItemStatusRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        return ResponseEntity.ok(orderService.updateOrderItemStatus(orderId, itemId, request.getStatus(), user));
    }

    @GetMapping("/table-bill")
    public ResponseEntity<TableBillResponse> getTableBill(
            @RequestParam String tableNumber,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        return ResponseEntity.ok(orderService.getTableBillSummary(tableNumber, user));
    }

    @PostMapping("/billing/preview")
    public ResponseEntity<BillSummaryResponse> previewBill(
            @Valid @RequestBody BillSummaryRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        return ResponseEntity.ok(orderService.generateBillSummary(request, user));
    }

    @PostMapping("/billing/close")
    public ResponseEntity<BillSummaryResponse> closeBill(
            @Valid @RequestBody BillSummaryRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        return ResponseEntity.ok(orderService.closeTableBill(request, user));
    }

    @PatchMapping("/{id}/billing")
    public ResponseEntity<OrderResponse> updateBillingStatus(
            @PathVariable String id,
            @RequestBody UpdateBillingStatusRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        return ResponseEntity.ok(orderService.updateBillingStatus(id, request.isBilled(), user));
    }

    @GetMapping("/guest/{id}")
    public ResponseEntity<OrderResponse> getGuestOrderById(@PathVariable String id, @RequestParam String tableNumber) {
        TableBillResponse summary = orderService.getPublicTableBillSummary(tableNumber);
        return ResponseEntity.ok(summary.getOrders().stream()
                .filter(order -> order.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new com.example.restaurant.exception.ResourceNotFoundException("Order not found for this table")));
    }

    @GetMapping("/guest/table-bill")
    public ResponseEntity<TableBillResponse> getGuestTableBill(@RequestParam String tableNumber) {
        return ResponseEntity.ok(orderService.getPublicTableBillSummary(tableNumber));
    }
}
