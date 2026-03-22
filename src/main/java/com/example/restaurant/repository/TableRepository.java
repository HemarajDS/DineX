package com.example.restaurant.repository;

import com.example.restaurant.entity.Table;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TableRepository extends MongoRepository<Table, String> {

    Optional<Table> findByTableNumberIgnoreCase(String tableNumber);

    boolean existsByTableNumberIgnoreCaseAndIdNot(String tableNumber, String id);
}
