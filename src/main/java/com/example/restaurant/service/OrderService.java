package com.example.restaurant.service;

import com.example.restaurant.dto.BillSummaryRequest;
import com.example.restaurant.dto.BillSummaryResponse;
import com.example.restaurant.dto.CreateOrderRequest;
import com.example.restaurant.dto.OrderItemRequest;
import com.example.restaurant.dto.OrderItemResponse;
import com.example.restaurant.dto.OrderResponse;
import com.example.restaurant.dto.ReceiptLineResponse;
import com.example.restaurant.dto.TableBillResponse;
import com.example.restaurant.entity.MenuItem;
import com.example.restaurant.entity.Order;
import com.example.restaurant.entity.OrderItem;
import com.example.restaurant.entity.OrderItemStatus;
import com.example.restaurant.entity.OrderStatus;
import com.example.restaurant.entity.Role;
import com.example.restaurant.entity.User;
import com.example.restaurant.exception.BadRequestException;
import com.example.restaurant.exception.ResourceNotFoundException;
import com.example.restaurant.exception.UnauthorizedException;
import com.example.restaurant.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemService menuItemService;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final TableService tableService;

    public OrderService(OrderRepository orderRepository, MenuItemService menuItemService, AuthService authService, NotificationService notificationService, TableService tableService) {
        this.orderRepository = orderRepository;
        this.menuItemService = menuItemService;
        this.authService = authService;
        this.notificationService = notificationService;
        this.tableService = tableService;
    }

    public OrderResponse createOrder(CreateOrderRequest request, User loggedInUser) {
        User orderOwner = resolveOrderOwner(request.getUserId(), loggedInUser);
        return createOrderRecord(request, orderOwner.getId(), orderOwner.getName());
    }

    public OrderResponse createGuestOrder(CreateOrderRequest request) {
        String normalizedTable = request.getTableNumber().trim().toUpperCase();
        User guestCustomer = authService.createGuestCustomer(request.getCustomerName(), normalizedTable);
        return createOrderRecord(request, guestCustomer.getId(), guestCustomer.getName());
    }

    private OrderResponse createOrderRecord(CreateOrderRequest request, String userId, String customerName) {
        List<OrderItem> incomingItems = buildOrderItems(request.getItems());
        if (incomingItems.isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        String tableNumber = request.getTableNumber().trim().toUpperCase();
        boolean isNewOrder = false;
        Order order = orderRepository.findFirstByTableNumberAndBilledFalseOrderByCreatedAtDesc(tableNumber).orElse(null);
        if (order == null) {
            isNewOrder = true;
            order = new Order();
            order.setUserId(userId);
            order.setCustomerName(customerName);
            order.setTableNumber(tableNumber);
            order.setOrderCode(generateOrderCode());
            order.setStatus(OrderStatus.PENDING);
            order.setBilled(false);
            order.setCreatedAt(LocalDateTime.now());
            order.setOrderItems(new ArrayList<>());
            order.setTotalAmount(BigDecimal.ZERO);
        }

        order.getOrderItems().addAll(incomingItems);
        recalculateOrder(order);
        Order savedOrder = orderRepository.save(order);
        if (isNewOrder) {
            notificationService.broadcastOrderCreated(savedOrder);
        } else {
            notificationService.broadcastOrderUpdated(savedOrder, "New dishes added to " + savedOrder.getOrderCode());
        }
        return mapToResponse(savedOrder);
    }

    public List<OrderResponse> getOrders(User loggedInUser) {
        List<Order> orders;
        if (loggedInUser.getRole() == Role.ADMIN || loggedInUser.getRole() == Role.CASHIER) {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        } else if (loggedInUser.getRole() == Role.CUSTOMER) {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(loggedInUser.getId());
        } else if (loggedInUser.getRole() == Role.WAITER) {
            orders = orderRepository.findByTableNumberInOrderByCreatedAtAsc(loggedInUser.getAssignedTableNumbers());
        } else if (loggedInUser.getRole() == Role.KITCHEN) {
            orders = orderRepository.findByStatusInOrderByCreatedAtAsc(List.of(OrderStatus.PENDING, OrderStatus.PREPARING));
        } else {
            orders = List.of();
        }

        return orders.stream()
                .sorted(resolveSort(loggedInUser.getRole()))
                .map(this::mapToResponse)
                .toList();
    }

    public OrderResponse getOrderById(String id, User loggedInUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (!canAccessOrder(order, loggedInUser)) {
            throw new UnauthorizedException("You can only view your own orders");
        }

        return mapToResponse(order);
    }

    public OrderResponse getActiveOrderForTable(String tableNumber, User loggedInUser) {
        String normalizedTable = tableNumber.trim().toUpperCase();

        if (loggedInUser.getRole() == Role.CUSTOMER) {
            throw new UnauthorizedException("Customers cannot view live service orders by table");
        }

        if (loggedInUser.getRole() == Role.WAITER
                && !loggedInUser.getAssignedTableNumbers().contains(normalizedTable)) {
            throw new UnauthorizedException("You can only view live orders for your assigned tables");
        }

        Order order = orderRepository.findFirstByTableNumberAndBilledFalseOrderByCreatedAtDesc(normalizedTable).orElse(null);
        return order == null ? null : mapToResponse(order);
    }

    public OrderResponse updateOrderStatus(String id, OrderStatus status, User loggedInUser) {
        if (loggedInUser.getRole() != Role.ADMIN
                && loggedInUser.getRole() != Role.KITCHEN
                && loggedInUser.getRole() != Role.WAITER) {
            throw new UnauthorizedException("You do not have permission to update order status");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (loggedInUser.getRole() == Role.WAITER
                && !loggedInUser.getAssignedTableNumbers().contains(order.getTableNumber())) {
            throw new UnauthorizedException("You can only update orders for your assigned tables");
        }

        order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.COMPLETED || status == OrderStatus.COMPLETED)
                .forEach(item -> item.setStatus(mapOrderStatusToItemStatus(status)));
        recalculateOrder(order);
        Order savedOrder = orderRepository.save(order);
        notificationService.broadcastOrderUpdated(savedOrder, "Order " + savedOrder.getOrderCode() + " moved to " + savedOrder.getStatus());
        return mapToResponse(savedOrder);
    }

    public OrderResponse updateOrderItemStatus(String orderId, String itemId, OrderItemStatus status, User loggedInUser) {
        if (loggedInUser.getRole() != Role.ADMIN
                && loggedInUser.getRole() != Role.KITCHEN
                && loggedInUser.getRole() != Role.WAITER) {
            throw new UnauthorizedException("You do not have permission to update dish status");
        }

        if (loggedInUser.getRole() == Role.KITCHEN && status == OrderItemStatus.SERVED) {
            throw new UnauthorizedException("Kitchen cannot mark dishes as served");
        }

        if (loggedInUser.getRole() == Role.WAITER && status != OrderItemStatus.SERVED) {
            throw new UnauthorizedException("Waiters can only mark kitchen-complete dishes as served");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (loggedInUser.getRole() == Role.WAITER
                && !loggedInUser.getAssignedTableNumbers().contains(order.getTableNumber())) {
            throw new UnauthorizedException("You can only update orders for your assigned tables");
        }

        OrderItem orderItem = order.getOrderItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + itemId));

        if (status == OrderItemStatus.SERVED && orderItem.getStatus() != OrderItemStatus.COMPLETED) {
            throw new BadRequestException("A dish can be marked served only after kitchen marks it done");
        }

        orderItem.setStatus(status);
        recalculateOrder(order);
        Order savedOrder = orderRepository.save(order);
        notificationService.broadcastOrderUpdated(savedOrder, orderItem.getMenuItemName() + " moved to " + status);
        return mapToResponse(savedOrder);
    }

    public TableBillResponse getTableBillSummary(String tableNumber, User loggedInUser) {
        List<Order> orders = getOrdersForTable(tableNumber);

        if (loggedInUser.getRole() == Role.CUSTOMER) {
            boolean ownsAnyOrder = orders.stream().anyMatch(order -> order.getUserId().equals(loggedInUser.getId()));
            if (!ownsAnyOrder) {
                throw new UnauthorizedException("You cannot view the bill for this table");
            }
        }

        if (loggedInUser.getRole() == Role.WAITER
                && !loggedInUser.getAssignedTableNumbers().contains(tableNumber.trim().toUpperCase())) {
            throw new UnauthorizedException("You cannot view the bill for this table");
        }

        return buildTableBillResponse(tableNumber, orders);
    }

    public TableBillResponse getPublicTableBillSummary(String tableNumber) {
        return buildTableBillResponse(tableNumber, getOrdersForTable(tableNumber));
    }

    public OrderResponse updateBillingStatus(String id, boolean billed, User loggedInUser) {
        if (loggedInUser.getRole() != Role.ADMIN
                && loggedInUser.getRole() != Role.CASHIER
                && loggedInUser.getRole() != Role.WAITER) {
            throw new UnauthorizedException("You do not have permission to update billing");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (loggedInUser.getRole() == Role.WAITER
                && !loggedInUser.getAssignedTableNumbers().contains(order.getTableNumber())) {
            throw new UnauthorizedException("You can only update billing for your assigned tables");
        }

        order.setBilled(billed);
        Order savedOrder = orderRepository.save(order);
        notificationService.broadcastOrderUpdated(savedOrder, "Billing updated for " + savedOrder.getTableNumber());
        return mapToResponse(savedOrder);
    }

    public boolean hasOpenOrdersForTable(String tableNumber) {
        return orderRepository.existsByTableNumberAndBilledFalse(tableNumber.trim().toUpperCase());
    }

    public BillSummaryResponse generateBillSummary(BillSummaryRequest request, User loggedInUser) {
        validateBillingRole(loggedInUser, request.getTableNumber());
        List<Order> orders = getUnbilledOrdersForTable(request.getTableNumber());
        return buildBillSummary(request, orders);
    }

    public BillSummaryResponse closeTableBill(BillSummaryRequest request, User loggedInUser) {
        validateBillingRole(loggedInUser, request.getTableNumber());
        List<Order> orders = getUnbilledOrdersForTable(request.getTableNumber());
        BillSummaryResponse summary = buildBillSummary(request, orders);

        for (Order order : orders) {
            order.setBilled(true);
            orderRepository.save(order);
            notificationService.broadcastOrderUpdated(order, "Bill closed for " + order.getTableNumber());
        }

        tableService.updateTableStatusByNumber(request.getTableNumber(), false);
        return summary;
    }

    private User resolveOrderOwner(String requestedUserId, User loggedInUser) {
        if (requestedUserId == null || requestedUserId.equals(loggedInUser.getId())) {
            return loggedInUser;
        }

        if (loggedInUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("You cannot create orders for another user");
        }

        return authService.getUserById(requestedUserId);
    }

    private List<Order> getOrdersForTable(String tableNumber) {
        List<Order> orders = orderRepository.findByTableNumberOrderByCreatedAtDesc(tableNumber.trim().toUpperCase());
        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No orders found for table: " + tableNumber);
        }
        return orders;
    }

    private List<Order> getUnbilledOrdersForTable(String tableNumber) {
        List<Order> orders = orderRepository.findByTableNumberOrderByCreatedAtDesc(tableNumber.trim().toUpperCase()).stream()
                .filter(order -> !order.isBilled())
                .sorted(Comparator.comparing(Order::getCreatedAt))
                .toList();
        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No open bill found for table: " + tableNumber);
        }
        return orders;
    }

    private TableBillResponse buildTableBillResponse(String tableNumber, List<Order> orders) {
        BigDecimal totalAmount = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TableBillResponse response = new TableBillResponse();
        response.setTableNumber(tableNumber.trim().toUpperCase());
        response.setOrderCount(orders.size());
        response.setTotalAmount(totalAmount);
        response.setOrders(orders.stream().map(this::mapToResponse).toList());
        return response;
    }

    private BillSummaryResponse buildBillSummary(BillSummaryRequest request, List<Order> orders) {
        BigDecimal subtotal = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountPercent = safePercent(request.getDiscountPercent());
        BigDecimal gstPercent = safePercent(request.getGstPercent());
        BigDecimal additionalTaxPercent = safePercent(request.getAdditionalTaxPercent());
        BigDecimal serviceChargePercent = safePercent(request.getServiceChargePercent());

        BigDecimal percentDiscountAmount = subtotal.multiply(discountPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal manualDiscountAmount = safeMoney(request.getDiscountAmount());
        BigDecimal discountAmount = manualDiscountAmount.add(percentDiscountAmount).min(subtotal);
        BigDecimal taxableAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        BigDecimal gstAmount = taxableAmount.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal additionalTaxAmount = taxableAmount.multiply(additionalTaxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal serviceChargeAmount = taxableAmount.multiply(serviceChargePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = taxableAmount.add(gstAmount).add(additionalTaxAmount).add(serviceChargeAmount);

        Map<String, List<OrderItem>> groupedItems = orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .collect(Collectors.groupingBy(OrderItem::getMenuItemName));

        List<ReceiptLineResponse> lines = groupedItems.entrySet().stream()
                .map(entry -> mapReceiptLine(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ReceiptLineResponse::getItemName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        BillSummaryResponse response = new BillSummaryResponse();
        response.setReceiptNumber("RCPT-" + LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-" + request.getTableNumber().trim().toUpperCase());
        response.setTableNumber(request.getTableNumber().trim().toUpperCase());
        response.setOrderCount(orders.size());
        response.setGuestCount(orders.stream().map(Order::getCustomerName).collect(Collectors.toSet()).size());
        response.setSubtotal(subtotal);
        response.setDiscountAmount(discountAmount);
        response.setTaxableAmount(taxableAmount);
        response.setGstPercent(gstPercent);
        response.setGstAmount(gstAmount);
        response.setAdditionalTaxPercent(additionalTaxPercent);
        response.setAdditionalTaxAmount(additionalTaxAmount);
        response.setServiceChargePercent(serviceChargePercent);
        response.setServiceChargeAmount(serviceChargeAmount);
        response.setGrandTotal(grandTotal);
        response.setLines(lines);
        return response;
    }

    private ReceiptLineResponse mapReceiptLine(String itemName, List<OrderItem> items) {
        int quantity = items.stream().mapToInt(OrderItem::getQuantity).sum();
        BigDecimal unitPrice = items.get(0).getPrice();
        ReceiptLineResponse response = new ReceiptLineResponse();
        response.setItemName(itemName);
        response.setQuantity(quantity);
        response.setUnitPrice(unitPrice);
        response.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        return response;
    }

    private void validateBillingRole(User loggedInUser, String tableNumber) {
        if (loggedInUser.getRole() != Role.ADMIN
                && loggedInUser.getRole() != Role.CASHIER
                && loggedInUser.getRole() != Role.WAITER) {
            throw new UnauthorizedException("You do not have permission to manage billing");
        }

        if (loggedInUser.getRole() == Role.WAITER
                && !loggedInUser.getAssignedTableNumbers().contains(tableNumber.trim().toUpperCase())) {
            throw new UnauthorizedException("You cannot manage billing for this table");
        }
    }

    private BigDecimal safePercent(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }

    private OrderResponse mapToResponse(Order order) {
        ensureOrderCode(order);
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderCode(order.getOrderCode());
        response.setUserId(order.getUserId());
        response.setCustomerName(order.getCustomerName());
        response.setTableNumber(order.getTableNumber());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setBilled(order.isBilled());
        response.setCreatedAt(order.getCreatedAt());
        response.setItems(order.getOrderItems().stream().map(this::mapOrderItemResponse).toList());
        return response;
    }

    private boolean canAccessOrder(Order order, User loggedInUser) {
        if (loggedInUser.getRole() == Role.ADMIN || loggedInUser.getRole() == Role.CASHIER) {
            return true;
        }
        if (loggedInUser.getRole() == Role.CUSTOMER) {
            return order.getUserId().equals(loggedInUser.getId());
        }
        if (loggedInUser.getRole() == Role.WAITER) {
            return loggedInUser.getAssignedTableNumbers().contains(order.getTableNumber());
        }
        if (loggedInUser.getRole() == Role.KITCHEN) {
            return true;
        }
        return false;
    }

    private Comparator<Order> resolveSort(Role role) {
        return role == Role.KITCHEN || role == Role.WAITER
                ? Comparator.comparing(Order::getCreatedAt)
                : Comparator.comparing(Order::getCreatedAt).reversed();
    }

    private void ensureOrderCode(Order order) {
        if (order.getOrderCode() == null || order.getOrderCode().isBlank()) {
            order.setOrderCode(generateOrderCode(order.getCreatedAt() == null ? LocalDate.now() : order.getCreatedAt().toLocalDate()));
            orderRepository.save(order);
        }
    }

    private String generateOrderCode() {
        return generateOrderCode(LocalDate.now());
    }

    private String generateOrderCode(LocalDate orderDate) {
        LocalDateTime start = orderDate.atStartOfDay();
        LocalDateTime end = orderDate.plusDays(1).atStartOfDay();
        long sequence = orderRepository.countByCreatedAtBetween(start, end) + 1;
        return "ORD-" + orderDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", sequence);
    }

    private OrderItemResponse mapOrderItemResponse(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(orderItem.getId());
        response.setMenuItemId(orderItem.getMenuItemId());
        response.setMenuItemName(orderItem.getMenuItemName());
        response.setQuantity(orderItem.getQuantity());
        response.setPrice(orderItem.getPrice());
        response.setLineTotal(orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        response.setStatus(orderItem.getStatus() == null ? OrderItemStatus.PENDING : orderItem.getStatus());
        return response;
    }

    private List<OrderItem> buildOrderItems(List<OrderItemRequest> requests) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequest itemRequest : requests) {
            MenuItem menuItem = menuItemService.getMenuItemEntity(itemRequest.getMenuItemId());
            if (!menuItem.isAvailable()) {
                throw new BadRequestException(menuItem.getName() + " is currently unavailable");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setId(UUID.randomUUID().toString());
            orderItem.setMenuItemId(menuItem.getId());
            orderItem.setMenuItemName(menuItem.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(menuItem.getPrice());
            orderItem.setStatus(OrderItemStatus.PENDING);
            orderItems.add(orderItem);
        }
        return orderItems;
    }

    private void recalculateOrder(Order order) {
        BigDecimal totalAmount = order.getOrderItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);
        order.setStatus(deriveOrderStatus(order.getOrderItems()));
    }

    private OrderStatus deriveOrderStatus(List<OrderItem> orderItems) {
        boolean anyPreparingOrReady = orderItems.stream()
                .anyMatch(item -> item.getStatus() == OrderItemStatus.PREPARING
                        || item.getStatus() == OrderItemStatus.COMPLETED
                        || item.getStatus() == OrderItemStatus.SERVED);
        boolean allServed = orderItems.stream()
                .allMatch(item -> item.getStatus() == OrderItemStatus.SERVED);

        if (allServed && !orderItems.isEmpty()) {
            return OrderStatus.COMPLETED;
        }
        if (anyPreparingOrReady) {
            return OrderStatus.PREPARING;
        }
        return OrderStatus.PENDING;
    }

    private OrderItemStatus mapOrderStatusToItemStatus(OrderStatus status) {
        return switch (status) {
            case PENDING -> OrderItemStatus.PENDING;
            case PREPARING -> OrderItemStatus.PREPARING;
            case COMPLETED -> OrderItemStatus.SERVED;
        };
    }
}
