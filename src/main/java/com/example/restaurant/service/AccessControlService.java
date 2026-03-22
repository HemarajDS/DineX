package com.example.restaurant.service;

import com.example.restaurant.entity.Role;
import com.example.restaurant.entity.User;
import com.example.restaurant.exception.UnauthorizedException;
import com.example.restaurant.security.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AccessControlService {

    private final AuthService authService;

    public AccessControlService(AuthService authService) {
        this.authService = authService;
    }

    public User validateUser(String userIdHeader, String roleHeader) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof JwtUserPrincipal principal) {
            User authenticatedUser = authService.getUserById(principal.getId());
            if (userIdHeader != null && !userIdHeader.isBlank() && !authenticatedUser.getId().equals(userIdHeader)) {
                throw new UnauthorizedException("User identity does not match the logged in token");
            }
            if (roleHeader != null && !roleHeader.isBlank() && authenticatedUser.getRole() != parseRole(roleHeader)) {
                throw new UnauthorizedException("User role does not match the logged in token");
            }
            return authenticatedUser;
        }

        if (userIdHeader == null || roleHeader == null || roleHeader.isBlank()) {
            throw new UnauthorizedException("Please login first");
        }

        User user = authService.getUserById(userIdHeader);
        if (user.getRole() != parseRole(roleHeader)) {
            throw new UnauthorizedException("User role does not match");
        }

        return user;
    }

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof JwtUserPrincipal principal) {
            return authService.getUserById(principal.getId());
        }
        throw new UnauthorizedException("Please login first");
    }

    public User requireAdmin(String userIdHeader, String roleHeader) {
        User user = validateUser(userIdHeader, roleHeader);
        if (user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only admins can perform this action");
        }
        return user;
    }

    private Role parseRole(String roleHeader) {
        try {
            return Role.valueOf(roleHeader.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException("Invalid role header");
        }
    }
}
