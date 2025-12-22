package com.badmintonshop.repository;

import com.badmintonshop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findAllByStatus(com.badmintonshop.entity.enums.OrderStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.variant WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findAllByStatusWithDetails(com.badmintonshop.entity.enums.OrderStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.variant ORDER BY o.createdAt DESC")
    List<Order> findAllWithDetails();
}
