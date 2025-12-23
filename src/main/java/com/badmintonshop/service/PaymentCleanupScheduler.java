package com.badmintonshop.service;

import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.Payment;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentStatus;
import com.badmintonshop.repository.OrderRepository;
import com.badmintonshop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to clean up pending/expired payments
 * Handles cases where:
 * - VNPay IPN was never received (rare)
 * - User abandoned payment without completing
 * - Network issues prevented callback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCleanupScheduler {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    // Payment timeout in minutes (VNPay has 15min, we wait 30min to be safe)
    private static final int PAYMENT_TIMEOUT_MINUTES = 30;

    /**
     * Run every 15 minutes to clean up pending VNPay payments
     * that have exceeded the timeout period
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // Every 15 minutes
    @Transactional
    public void cleanupExpiredVNPayPayments() {
        log.info("Starting VNPay payment cleanup job...");

        LocalDateTime timeout = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<Payment> pendingPayments = paymentRepository.findPendingVNPayPaymentsOlderThan(timeout);

        if (pendingPayments.isEmpty()) {
            log.info("No expired VNPay payments found.");
            return;
        }

        log.info("Found {} expired VNPay payments to process", pendingPayments.size());

        for (Payment payment : pendingPayments) {
            try {
                processExpiredPayment(payment);
            } catch (Exception e) {
                log.error("Error processing expired payment {}: {}", 
                        payment.getPaymentId(), e.getMessage(), e);
            }
        }

        log.info("VNPay payment cleanup job completed.");
    }

    /**
     * Process a single expired payment
     */
    private void processExpiredPayment(Payment payment) {
        Order order = payment.getOrder();
        String orderNumber = order.getOrderNumber();

        log.info("Processing expired payment for order: {}", orderNumber);

        // 1. Mark payment as EXPIRED
        payment.setStatus(PaymentStatus.EXPIRED);
        payment.setFailedAt(LocalDateTime.now());
        payment.setGatewayResponse("Payment expired - timeout after " + PAYMENT_TIMEOUT_MINUTES + " minutes");
        paymentRepository.save(payment);

        // 2. Restore inventory
        try {
            inventoryService.restoreStockForOrder(order);
            log.info("Restored inventory for expired order: {}", orderNumber);
        } catch (Exception e) {
            log.error("Failed to restore inventory for order {}: {}", orderNumber, e.getMessage());
        }

        // 3. Update order status
        order.setPaymentStatus(PaymentStatus.EXPIRED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setAdminNotes("Auto-cancelled: Payment expired after " + PAYMENT_TIMEOUT_MINUTES + " minutes");
        orderRepository.save(order);

        log.info("Order {} cancelled due to payment expiry", orderNumber);
    }

    /**
     * Manual trigger for cleanup (can be called from admin endpoint)
     */
    public int manualCleanup() {
        log.info("Manual payment cleanup triggered...");
        
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<Payment> pendingPayments = paymentRepository.findPendingVNPayPaymentsOlderThan(timeout);

        int count = 0;
        for (Payment payment : pendingPayments) {
            try {
                processExpiredPayment(payment);
                count++;
            } catch (Exception e) {
                log.error("Error in manual cleanup for payment {}", payment.getPaymentId(), e);
            }
        }

        log.info("Manual cleanup completed. Processed {} payments.", count);
        return count;
    }
}
