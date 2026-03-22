package com.example.restaurant.controller;

import com.example.restaurant.dto.TableRequest;
import com.example.restaurant.dto.TableResponse;
import com.example.restaurant.dto.UpdateTableStatusRequest;
import com.example.restaurant.entity.Role;
import com.example.restaurant.entity.User;
import com.example.restaurant.exception.UnauthorizedException;
import com.example.restaurant.service.AccessControlService;
import com.example.restaurant.service.OrderService;
import com.example.restaurant.service.TableService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final TableService tableService;
    private final AccessControlService accessControlService;
    private final OrderService orderService;

    public TableController(TableService tableService, AccessControlService accessControlService, OrderService orderService) {
        this.tableService = tableService;
        this.accessControlService = accessControlService;
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<TableResponse>> getTables(
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.validateUser(userId, role);
        return ResponseEntity.ok(tableService.getTables());
    }

    @PostMapping
    public ResponseEntity<TableResponse> createTable(
            @Valid @RequestBody TableRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.requireAdmin(userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.createTable(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TableResponse> updateTable(
            @PathVariable String id,
            @Valid @RequestBody TableRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.requireAdmin(userId, role);
        return ResponseEntity.ok(tableService.updateTable(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TableResponse> updateTableStatus(
            @PathVariable String id,
            @RequestBody UpdateTableStatusRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        User user = accessControlService.validateUser(userId, role);
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.WAITER && user.getRole() != Role.CASHIER) {
            throw new UnauthorizedException("You do not have permission to update table status");
        }
        return ResponseEntity.ok(tableService.updateTableStatus(id, request.isOccupied()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(
            @PathVariable String id,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.requireAdmin(userId, role);
        TableResponse table = tableService.getTableById(id);
        tableService.deleteTable(id, orderService.hasOpenOrdersForTable(table.getTableNumber()));
        return ResponseEntity.noContent().build();
    }
}
