package com.badmintonshop.service;

import com.badmintonshop.dto.payment.BankTransferConfirmRequest;
import com.badmintonshop.dto.payment.PaymentDTO;
import com.badmintonshop.dto.payment.VerifyPaymentRequest;
import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.Payment;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import com.badmintonshop.repository.OrderRepository;
import com.badmintonshop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for payment operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Create payment for order (COD or Bank Transfer)
     */
    @Transactional
    public PaymentDTO createPaymentForOrder(Long orderId, PaymentMethod method) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Check if payment already exists
        if (paymentRepository.existsByOrderOrderIdAndStatus(orderId, PaymentStatus.PAID)) {
            throw new RuntimeException("Order already has a paid payment");
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(method)
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Created {} payment for order {}", method, order.getOrderNumber());

        return PaymentDTO.fromEntity(payment);
    }

    /**
     * Confirm bank transfer with proof
     */
    @Transactional
    public PaymentDTO confirmBankTransfer(Long userId, BankTransferConfirmRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify ownership
        if (!order.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Không có quyền truy cập đơn hàng này");
        }

        // Get or create payment
        Payment payment = paymentRepository.findFirstByOrderOrderIdOrderByPaymentIdDesc(request.getOrderId())
                .orElseGet(() -> {
                    Payment newPayment = Payment.builder()
                            .order(order)
                            .paymentMethod(PaymentMethod.BANK_TRANSFER)
                            .amount(order.getTotalAmount())
                            .status(PaymentStatus.PENDING)
                            .build();
                    return paymentRepository.save(newPayment);
                });

        // Update bank transfer info
        payment.setBankName(request.getBankName());
        payment.setBankAccountNumber(request.getBankAccountNumber());
        payment.setTransferReference(request.getTransferReference());
        payment.setTransferProofUrl(request.getProofImageUrl());

        payment = paymentRepository.save(payment);
        log.info("Bank transfer confirmed for order {} by user {}", order.getOrderNumber(), userId);

        return PaymentDTO.fromEntity(payment);
    }

    /**
     * Admin: Verify bank transfer payment
     */
    @Transactional
    public PaymentDTO verifyBankTransfer(Long paymentId, VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new RuntimeException("Chỉ có thể xác nhận thanh toán chuyển khoản");
        }

        if (request.getApproved()) {
            payment.markAsPaid(request.getTransactionId());
            
            // Update order payment status
            Order order = payment.getOrder();
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            log.info("Bank transfer payment {} verified and approved", paymentId);
        } else {
            payment.markAsFailed();
            log.info("Bank transfer payment {} rejected", paymentId);
        }

        return PaymentDTO.fromEntity(paymentRepository.save(payment));
    }

    /**
     * Mark COD payment as paid (when order delivered)
     */
    @Transactional
    public PaymentDTO markCODPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = paymentRepository.findFirstByOrderOrderIdOrderByPaymentIdDesc(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order"));

        if (payment.getPaymentMethod() != PaymentMethod.COD) {
            throw new RuntimeException("Chỉ áp dụng cho thanh toán COD");
        }

        payment.markAsPaid("COD-" + order.getOrderNumber());
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        log.info("COD payment marked as paid for order {}", order.getOrderNumber());
        return PaymentDTO.fromEntity(paymentRepository.save(payment));
    }

    /**
     * Get payment by ID
     */
    public PaymentDTO getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(PaymentDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    /**
     * Get payments by order
     */
    public List<PaymentDTO> getPaymentsByOrder(Long orderId) {
        return paymentRepository.findByOrderOrderId(orderId).stream()
                .map(PaymentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get pending bank transfers (for admin)
     */
    public List<PaymentDTO> getPendingBankTransfers() {
        return paymentRepository.findPendingBankTransfers().stream()
                .map(PaymentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments with filters (admin)
     */
    public Page<PaymentDTO> getAllPayments(
            PaymentStatus status,
            PaymentMethod method,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        return paymentRepository.findWithFilters(status, method, fromDate, toDate, pageable)
                .map(PaymentDTO::fromEntity);
    }

    /**
     * Get payment statistics (admin)
     */
    public java.util.Map<String, Object> getPaymentStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("pending", paymentRepository.countByStatus(PaymentStatus.PENDING));
        stats.put("paid", paymentRepository.countByStatus(PaymentStatus.PAID));
        stats.put("failed", paymentRepository.countByStatus(PaymentStatus.FAILED));
        stats.put("refunded", paymentRepository.countByStatus(PaymentStatus.REFUNDED));
        stats.put("cod", paymentRepository.countByPaymentMethod(PaymentMethod.COD));
        stats.put("vnpay", paymentRepository.countByPaymentMethod(PaymentMethod.VNPAY));
        stats.put("bankTransfer", paymentRepository.countByPaymentMethod(PaymentMethod.BANK_TRANSFER));
        return stats;
    }
}
