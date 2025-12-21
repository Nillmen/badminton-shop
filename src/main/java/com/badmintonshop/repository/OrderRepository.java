package com.badmintonshop.repository;

import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "items" })
    List<Order> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    // Tìm đơn hàng theo sessionId (cho Guest)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "items" })
    List<Order> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    // Tìm đơn theo mã đơn hàng (để tracking)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "items" })
    Optional<Order> findByOrderNumber(String orderNumber);

    // Lấy lịch sử đơn hàng của user
    Page<Order> findByUser_UserId(Long userId, Pageable pageable);

    // Tìm các đơn hàng theo trạng thái (VD: tìm đơn mới pending để duyệt)
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // Query tìm doanh thu (Ví dụ đơn giản)
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'DELIVERED'")
    Double calculateTotalRevenue();

    @Query("SELECT o FROM Order o JOIN FETCH o.user ORDER BY o.createdAt DESC")
    List<Order> findAllOrdersWithUser();

    // Trong OrderRepository.java
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.items oi " +
            "WHERE o.orderId = :orderId")
    Optional<Order> findDetailByIdWithItemsAndHistory(@Param("orderId") Long orderId);

}