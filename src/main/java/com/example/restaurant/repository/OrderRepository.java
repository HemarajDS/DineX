package com.example.restaurant.repository;

import com.example.restaurant.entity.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Order> findByTableNumberOrderByCreatedAtDesc(String tableNumber);

    List<Order> findByTableNumberInOrderByCreatedAtAsc(List<String> tableNumbers);

    List<Order> findByStatusInOrderByCreatedAtAsc(List<com.example.restaurant.entity.OrderStatus> statuses);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    java.util.Optional<Order> findFirstByTableNumberAndBilledFalseOrderByCreatedAtDesc(String tableNumber);

    boolean existsByTableNumberAndBilledFalse(String tableNumber);
}
