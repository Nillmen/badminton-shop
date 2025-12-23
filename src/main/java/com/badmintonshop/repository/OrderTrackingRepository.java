package com.badmintonshop.repository;

import com.badmintonshop.entity.OrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {

    @Query("SELECT t FROM OrderTracking t WHERE t.order.orderId = :orderId")
    Optional<OrderTracking> findByOrderId(@Param("orderId") Long orderId);

    Optional<OrderTracking> findByTrackingNumber(String trackingNumber);
}
