package com.example.restaurant.controller;

import com.example.restaurant.dto.RegisterRequest;
import com.example.restaurant.dto.UpdateUserRequest;
import com.example.restaurant.dto.UserResponse;
import com.example.restaurant.service.AccessControlService;
import com.example.restaurant.service.AuthService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;
    private final AccessControlService accessControlService;

    public UserController(AuthService authService, AccessControlService accessControlService) {
        this.authService = authService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.requireAdmin(userId, role);
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserResponse> createStaffUser(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.requireAdmin(userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStaff(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateStaffUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.requireAdmin(userId, role);
        return ResponseEntity.ok(authService.updateStaffUser(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaffUser(
            @PathVariable String id,
            @RequestHeader(name = "X-USER-ID") String userId,
            @RequestHeader(name = "X-USER-ROLE") String role
    ) {
        accessControlService.requireAdmin(userId, role);
        authService.deleteStaffUser(id, userId);
        return ResponseEntity.noContent().build();
    }
}
