package com.example.restaurant.repository;

import com.example.restaurant.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, String id);

    Optional<User> findByEmailIgnoreCase(String email);
}
