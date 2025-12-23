package com.badmintonshop.service;

import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.Payment;
import com.badmintonshop.entity.enums.CancelledBy;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scheduled job to clean up pending/expired VNPay payments and orders
 * Handles cases where:
 * - User selected VNPay but never clicked to pay (no Payment record)
 * - User started VNPay payment but abandoned/closed browser
 * - VNPay IPN was never received due to network issues
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
     * Run every 15 minutes to clean up:
     * 1. Pending VNPay Payment records that exceeded timeout
     * 2. VNPay Orders WITHOUT Payment records (user never clicked to pay)
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // Every 15 minutes
    @Transactional
    public void cleanupExpiredVNPayOrders() {
        log.info("Starting VNPay cleanup job...");
        
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        Set<Long> processedOrderIds = new HashSet<>();
        int paymentCleanupCount = 0;
        int orderCleanupCount = 0;

        // ========== Part 1: Clean up pending Payment records ==========
        List<Payment> pendingPayments = paymentRepository.findPendingVNPayPaymentsOlderThan(timeout);
        log.info("Found {} expired VNPay payments to process", pendingPayments.size());

        for (Payment payment : pendingPayments) {
            try {
                processExpiredPayment(payment);
                processedOrderIds.add(payment.getOrder().getOrderId());
                paymentCleanupCount++;
            } catch (Exception e) {
                log.error("Error processing expired payment {}: {}", 
                        payment.getPaymentId(), e.getMessage(), e);
            }
        }

        // ========== Part 2: Clean up VNPay orders WITHOUT Payment records ==========
        // These are orders where user selected VNPay but never clicked to pay
        List<Order> expiredOrders = orderRepository.findExpiredVNPayOrders(timeout);
        log.info("Found {} expired VNPay orders (without payment) to process", expiredOrders.size());

        for (Order order : expiredOrders) {
            // Skip if already processed via Payment cleanup
            if (processedOrderIds.contains(order.getOrderId())) {
                continue;
            }
            
            try {
                processExpiredOrder(order);
                orderCleanupCount++;
            } catch (Exception e) {
                log.error("Error processing expired order {}: {}", 
                        order.getOrderNumber(), e.getMessage(), e);
            }
        }

        log.info("VNPay cleanup job completed. Processed {} payments, {} orders.", 
                paymentCleanupCount, orderCleanupCount);
    }

    /**
     * Process a single expired payment (Payment record exists)
     */
    private void processExpiredPayment(Payment payment) {
        Order order = payment.getOrder();
        String orderNumber = order.getOrderNumber();

        log.info("Processing expired payment for order: {}", orderNumber);

        // 1. Mark payment as FAILED (timeout)
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailedAt(LocalDateTime.now());
        payment.setGatewayResponse("{\"status\":\"TIMEOUT\",\"reason\":\"Payment expired - timeout after " + PAYMENT_TIMEOUT_MINUTES + " minutes\"}");
        paymentRepository.save(payment);

        // 2. Restore inventory
        restoreInventorySafe(order);

        // 3. Update order status
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledBy(CancelledBy.SYSTEM);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledReason("Auto-cancelled: VNPay payment expired after " + PAYMENT_TIMEOUT_MINUTES + " minutes");
        orderRepository.save(order);

        log.info("Order {} cancelled due to payment expiry", orderNumber);
    }

    /**
     * Process a single expired order (no Payment record exists)
     * This happens when user selected VNPay but never clicked to pay
     */
    private void processExpiredOrder(Order order) {
        String orderNumber = order.getOrderNumber();

        log.info("Processing expired VNPay order (no payment record): {}", orderNumber);

        // 1. Restore inventory
        restoreInventorySafe(order);

        // 2. Update order status
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledBy(CancelledBy.SYSTEM);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledReason("Auto-cancelled: No payment initiated within " + PAYMENT_TIMEOUT_MINUTES + " minutes");
        orderRepository.save(order);

        log.info("Order {} cancelled - user never initiated VNPay payment", orderNumber);
    }

    /**
     * Safely restore inventory, logging any errors
     */
    private void restoreInventorySafe(Order order) {
        try {
            inventoryService.restoreStockForOrder(order);
            log.info("Restored inventory for expired order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to restore inventory for order {}: {}", 
                    order.getOrderNumber(), e.getMessage());
        }
    }

    /**
     * Manual trigger for cleanup (can be called from admin endpoint)
     * @return number of orders processed
     */
    @Transactional
    public int manualCleanup() {
        log.info("Manual VNPay cleanup triggered...");
        
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        Set<Long> processedOrderIds = new HashSet<>();
        int count = 0;

        // Part 1: Process payments
        List<Payment> pendingPayments = paymentRepository.findPendingVNPayPaymentsOlderThan(timeout);
        for (Payment payment : pendingPayments) {
            try {
                processExpiredPayment(payment);
                processedOrderIds.add(payment.getOrder().getOrderId());
                count++;
            } catch (Exception e) {
                log.error("Error in manual cleanup for payment {}", payment.getPaymentId(), e);
            }
        }

        // Part 2: Process orders without payment records
        List<Order> expiredOrders = orderRepository.findExpiredVNPayOrders(timeout);
        for (Order order : expiredOrders) {
            if (processedOrderIds.contains(order.getOrderId())) {
                continue;
            }
            try {
                processExpiredOrder(order);
                count++;
            } catch (Exception e) {
                log.error("Error in manual cleanup for order {}", order.getOrderNumber(), e);
            }
        }

        log.info("Manual cleanup completed. Processed {} items.", count);
        return count;
    }
}
