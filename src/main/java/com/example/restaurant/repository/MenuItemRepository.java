package com.example.restaurant.repository;

import com.example.restaurant.entity.MenuItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MenuItemRepository extends MongoRepository<MenuItem, String> {

    List<MenuItem> findByNameContainingIgnoreCaseAndCategoryContainingIgnoreCase(String name, String category);
}
