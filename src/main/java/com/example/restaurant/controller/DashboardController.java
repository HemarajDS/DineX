package com.example.restaurant.controller;

import com.example.restaurant.entity.User;
import com.example.restaurant.service.AccessControlService;
import com.example.restaurant.service.MenuItemService;
import com.example.restaurant.service.OrderService;
import com.example.restaurant.repository.OrderRepository;
import com.example.restaurant.repository.TableRepository;
import com.example.restaurant.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AccessControlService accessControlService;
    private final MenuItemService menuItemService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final TableRepository tableRepository;
    private final OrderRepository orderRepository;

    public DashboardController(
            AccessControlService accessControlService,
            MenuItemService menuItemService,
            OrderService orderService,
            UserRepository userRepository,
            TableRepository tableRepository,
            OrderRepository orderRepository
    ) {
        this.accessControlService = accessControlService;
        this.menuItemService = menuItemService;
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        var menuItems = menuItemService.getAllMenuItems("", "");
        var orders = orderRepository.findAllByOrderByCreatedAtDesc();
        var tables = tableRepository.findAll();
        var users = userRepository.findAll();

        BigDecimal totalRevenue = orders.stream()
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();

        List<com.example.restaurant.dto.OrderResponse> mappedOrders = orders.stream().map(order -> orderService.getOrderById(order.getId(), user)).toList();

        BigDecimal todayRevenue = orders.stream()
                .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().toLocalDate().equals(today))
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long todayOrderCount = orders.stream()
                .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().toLocalDate().equals(today))
                .count();

        long customerCount = orders.stream()
                .map(order -> order.getUserId() == null ? order.getId() : order.getUserId())
                .distinct()
                .count();

        BigDecimal averageOrderValue = orders.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(orders.size()), 2, java.math.RoundingMode.HALF_UP);

        Map<String, Long> statusCounts = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getStatus().name(), LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> categoryCounts = menuItems.stream()
                .collect(Collectors.groupingBy(menuItem -> menuItem.getCategory(), LinkedHashMap::new, Collectors.counting()));

        Map<LocalDate, BigDecimal> revenueByDate = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount(),
                                BigDecimal::add
                        )
                ));

        List<Map<String, Object>> revenueTrend = revenueByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("date", entry.getKey().toString());
                    item.put("revenue", entry.getValue());
                    return item;
                })
                .toList();

        Map<YearMonth, BigDecimal> revenueByMonth = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> YearMonth.from(order.getCreatedAt()),
                        LinkedHashMap::new,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount(),
                                BigDecimal::add
                        )
                ));

        List<Map<String, Object>> monthlyRevenueTrend = revenueByMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("month", entry.getKey().toString());
                    item.put("revenue", entry.getValue());
                    return item;
                })
                .toList();

        Map<Integer, Long> todayHourTrendMap = orders.stream()
                .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().toLocalDate().equals(today))
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().getHour(), LinkedHashMap::new, Collectors.counting()));

        List<Map<String, Object>> todayHourTrend = java.util.stream.IntStream.range(0, 24)
                .mapToObj(hour -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("hour", String.format("%02d:00", hour));
                    item.put("count", todayHourTrendMap.getOrDefault(hour, 0L));
                    return item;
                })
                .toList();

        List<Map<String, Object>> menuCategoryBreakdown = categoryCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("category", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .toList();

        List<Map<String, Object>> statusBreakdown = new ArrayList<>();
        for (String status : List.of("PENDING", "PREPARING", "COMPLETED")) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", status);
            item.put("count", statusCounts.getOrDefault(status, 0L));
            statusBreakdown.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("menuItemCount", menuItems.size());
        response.put("orderCount", orders.size());
        response.put("tableCount", tables.size());
        response.put("occupiedTableCount", tables.stream().filter(table -> table.isOccupied()).count());
        response.put("staffCount", users.stream().filter(entry -> entry.getRole() != null && entry.getRole() != com.example.restaurant.entity.Role.CUSTOMER).count());
        response.put("customerCount", customerCount);
        response.put("todayOrderCount", todayOrderCount);
        response.put("todayRevenue", todayRevenue);
        response.put("currentMonth", currentMonth.toString());
        response.put("totalRevenue", totalRevenue);
        response.put("averageOrderValue", averageOrderValue);
        response.put("statusBreakdown", statusBreakdown);
        response.put("menuCategoryBreakdown", menuCategoryBreakdown);
        response.put("revenueTrend", revenueTrend);
        response.put("monthlyRevenueTrend", monthlyRevenueTrend);
        response.put("todayHourTrend", todayHourTrend);
        response.put("recentOrders", mappedOrders.stream().limit(5).toList());
        return ResponseEntity.ok(response);
    }
}
