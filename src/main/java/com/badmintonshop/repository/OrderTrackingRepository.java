package com.badmintonshop.repository;

import com.badmintonshop.entity.OrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {

    /**
     * Tìm kiếm thông tin tracking theo ID của Order.
     * Cần thiết khi Admin muốn update hoặc tạo mới tracking cho một Order cụ thể.
     */
    Optional<OrderTracking> findByOrder_OrderId(Long orderId);
}