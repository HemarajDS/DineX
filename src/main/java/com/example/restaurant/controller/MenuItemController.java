package com.example.restaurant.controller;

import com.example.restaurant.dto.ApiResponse;
import com.example.restaurant.dto.MenuItemRequest;
import com.example.restaurant.dto.MenuItemResponse;
import com.example.restaurant.service.AccessControlService;
import com.example.restaurant.service.MenuItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu-items")
public class MenuItemController {

    private final MenuItemService menuItemService;
    private final AccessControlService accessControlService;

    public MenuItemController(MenuItemService menuItemService, AccessControlService accessControlService) {
        this.menuItemService = menuItemService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> getAllMenuItems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(menuItemService.getAllMenuItems(search, category));
    }

    @PostMapping
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @Valid @RequestBody MenuItemRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.requireAdmin(userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(menuItemService.createMenuItem(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable String id,
            @Valid @RequestBody MenuItemRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.requireAdmin(userId, role);
        return ResponseEntity.ok(menuItemService.updateMenuItem(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteMenuItem(
            @PathVariable String id,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.requireAdmin(userId, role);
        menuItemService.deleteMenuItem(id);
        return ResponseEntity.ok(new ApiResponse(true, "Menu item deleted successfully"));
    }
}
