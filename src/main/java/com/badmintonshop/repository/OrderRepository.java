package com.badmintonshop.repository;

import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.User;
import com.badmintonshop.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find by user
    Page<Order> findByUser(User user, Pageable pageable);
    
    Page<Order> findByUserUserId(Long userId, Pageable pageable);

    // Find by order number
    Optional<Order> findByOrderNumber(String orderNumber);

    // Find by status
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByStatusIn(List<OrderStatus> statuses, Pageable pageable);

    // Count by status
    long countByStatus(OrderStatus status);

    // Admin: Find all orders with optional status filter
    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status) ORDER BY o.createdAt DESC")
    Page<Order> findAllWithStatusFilter(@Param("status") OrderStatus status, Pageable pageable);

    // Admin: Complex filter query
    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) AND " +
           "(:fromDate IS NULL OR o.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR o.createdAt <= :toDate) AND " +
           "(:search IS NULL OR o.orderNumber LIKE %:search% OR o.user.email LIKE %:search% OR o.user.fullName LIKE %:search%)")
    Page<Order> findAllWithFilters(
            @Param("status") OrderStatus status,
            @Param("paymentStatus") com.badmintonshop.entity.enums.PaymentStatus paymentStatus,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("search") String search,
            Pageable pageable);

    // Find order with all details eagerly fetched
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product " +
           "LEFT JOIN FETCH i.variant " +
           "LEFT JOIN FETCH o.tracking " +
           "WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product " +
           "LEFT JOIN FETCH i.variant " +
           "LEFT JOIN FETCH o.tracking " +
           "WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithDetails(@Param("orderNumber") String orderNumber);

    // Get today's order count for order number generation
    @Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.createdAt) = :date")
    long countByCreatedDate(@Param("date") LocalDate date);
    
    // Find orders with stringing items pending
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i " +
           "WHERE i.hasStringingService = true AND i.stringingStatus = 'PENDING'")
    List<Order> findOrdersWithPendingStringing();

    // Find expired VNPay orders for cleanup
    // Orders with VNPAY payment method, PENDING status, created before timeout
    // and no successful payment (either no payment record or payment still pending)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items " +
           "WHERE o.paymentMethod = 'VNPAY' " +
           "AND o.status = 'PENDING' " +
           "AND o.paymentStatus = 'PENDING' " +
           "AND o.createdAt < :timeout")
    List<Order> findExpiredVNPayOrders(@Param("timeout") LocalDateTime timeout);
}
