package com.example.restaurant.controller;

import com.example.restaurant.dto.AuthRequest;
import com.example.restaurant.dto.LoginResponse;
import com.example.restaurant.dto.RegisterRequest;
import com.example.restaurant.dto.UserResponse;
import com.example.restaurant.entity.User;
import com.example.restaurant.service.AccessControlService;
import com.example.restaurant.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccessControlService accessControlService;

    public AuthController(AuthService authService, AccessControlService accessControlService) {
        this.authService = authService;
        this.accessControlService = accessControlService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        User user = accessControlService.getAuthenticatedUser();
        return ResponseEntity.ok(authService.mapToUserResponse(user));
    }
}
