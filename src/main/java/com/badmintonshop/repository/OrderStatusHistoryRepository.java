package com.badmintonshop.repository;

import com.badmintonshop.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    @Query("SELECT h FROM OrderStatusHistory h WHERE h.order.orderId = :orderId ORDER BY h.changedAt DESC")
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtDesc(@Param("orderId") Long orderId);
}
