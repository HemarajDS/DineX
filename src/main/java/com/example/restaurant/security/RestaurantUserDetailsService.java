package com.example.restaurant.security;

import com.example.restaurant.entity.User;
import com.example.restaurant.exception.ResourceNotFoundException;
import com.example.restaurant.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RestaurantUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public RestaurantUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        return map(user);
    }

    public JwtUserPrincipal loadUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return map(user);
    }

    private JwtUserPrincipal map(User user) {
        return new JwtUserPrincipal(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getAssignedTableNumbers()
        );
    }
}
