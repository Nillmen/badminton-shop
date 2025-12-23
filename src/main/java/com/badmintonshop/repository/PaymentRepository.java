package com.badmintonshop.repository;

import com.badmintonshop.entity.Payment;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find by order
    List<Payment> findByOrderOrderId(Long orderId);
    
    Optional<Payment> findFirstByOrderOrderIdOrderByPaymentIdDesc(Long orderId);

    // Find by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);

    // Find by status
    List<Payment> findByStatus(PaymentStatus status);
    
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    // Find by payment method
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);

    // Find pending bank transfers
    @Query("SELECT p FROM Payment p WHERE p.paymentMethod = 'BANK_TRANSFER' AND p.status = 'PENDING' AND p.transferProofUrl IS NOT NULL")
    List<Payment> findPendingBankTransfers();

    // Admin: All payments with filters
    @Query("SELECT p FROM Payment p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:method IS NULL OR p.paymentMethod = :method) AND " +
           "(:fromDate IS NULL OR p.paidAt >= :fromDate) AND " +
           "(:toDate IS NULL OR p.paidAt <= :toDate)")
    Page<Payment> findWithFilters(
            @Param("status") PaymentStatus status,
            @Param("method") PaymentMethod method,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    // Count by status
    long countByStatus(PaymentStatus status);

    // Count by method
    long countByPaymentMethod(PaymentMethod paymentMethod);

    // Check if order has paid payment
    boolean existsByOrderOrderIdAndStatus(Long orderId, PaymentStatus status);

    // Find pending VNPay payments older than specified time (for cleanup)
    // Uses COALESCE to handle old records without createdAt (fallback to order.createdAt)
    @Query("SELECT p FROM Payment p JOIN FETCH p.order o LEFT JOIN FETCH o.items " +
           "WHERE p.status = 'PENDING' AND p.paymentMethod = 'VNPAY' " +
           "AND p.paidAt IS NULL AND COALESCE(p.createdAt, o.createdAt) < :timeout")
    List<Payment> findPendingVNPayPaymentsOlderThan(@Param("timeout") LocalDateTime timeout);
}
