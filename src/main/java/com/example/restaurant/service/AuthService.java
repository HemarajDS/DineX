package com.example.restaurant.service;

import com.example.restaurant.dto.AuthRequest;
import com.example.restaurant.dto.LoginResponse;
import com.example.restaurant.dto.RegisterRequest;
import com.example.restaurant.dto.UpdateUserRequest;
import com.example.restaurant.dto.UserResponse;
import com.example.restaurant.entity.Role;
import com.example.restaurant.entity.User;
import com.example.restaurant.exception.BadRequestException;
import com.example.restaurant.exception.ResourceNotFoundException;
import com.example.restaurant.exception.UnauthorizedException;
import com.example.restaurant.repository.UserRepository;
import com.example.restaurant.security.JwtService;
import com.example.restaurant.security.JwtUserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        return registerInternal(request);
    }

    public UserResponse registerStaff(RegisterRequest request) {
        if (request.getRole() == null || request.getRole() == Role.CUSTOMER) {
            throw new BadRequestException("Please choose a staff role for this account");
        }
        return registerInternal(request);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getRole).thenComparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapToUserResponse)
                .toList();
    }

    public UserResponse updateStaffUser(String id, UpdateUserRequest request, String actingUserId) {
        User user = getUserById(id);
        if (user.getId().equals(actingUserId) && request.getRole() != user.getRole()) {
            throw new BadRequestException("You cannot change your own admin role from this screen");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new BadRequestException("Email is already registered");
        }

        if (request.getRole() == null || request.getRole() == Role.CUSTOMER) {
            throw new BadRequestException("Please choose a valid staff role");
        }

        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }
        user.setRole(request.getRole());
        user.setAssignedTableNumbers(request.getRole() == Role.WAITER
                ? normalizeAssignedTables(request.getAssignedTableNumbers())
                : List.of());

        return mapToUserResponse(userRepository.save(user));
    }

    public void deleteStaffUser(String id, String actingUserId) {
        User user = getUserById(id);
        if (user.getId().equals(actingUserId)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        userRepository.delete(user);
    }

    private UserResponse registerInternal(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() == null ? Role.CUSTOMER : request.getRole());
        user.setAssignedTableNumbers(user.getRole() == Role.WAITER
                ? normalizeAssignedTables(request.getAssignedTableNumbers())
                : List.of());

        return mapToUserResponse(userRepository.save(user));
    }

    public LoginResponse login(AuthRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        JwtUserPrincipal principal = new JwtUserPrincipal(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getAssignedTableNumbers()
        );
        return new LoginResponse("Login successful", jwtService.generateToken(principal), mapToUserResponse(user));
    }

    public User createGuestCustomer(String name, String tableNumber) {
        User guest = new User();
        guest.setId("guest-" + java.util.UUID.randomUUID());
        guest.setName((name == null || name.isBlank()) ? "Guest at " + tableNumber : name.trim());
        guest.setEmail("");
        guest.setPassword("");
        guest.setRole(Role.CUSTOMER);
        guest.setAssignedTableNumbers(List.of(tableNumber));
        return guest;
    }

    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    public UserResponse mapToUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getAssignedTableNumbers());
    }

    private List<String> normalizeAssignedTables(List<String> assignedTableNumbers) {
        if (assignedTableNumbers == null) {
            return List.of();
        }

        return assignedTableNumbers.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}
