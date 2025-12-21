package com.badmintonshop.service;

import com.badmintonshop.dto.BankTransferRequest;
import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.Payment;
import com.badmintonshop.entity.enums.CancelledBy;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import com.badmintonshop.exception.ResourceNotFoundException;
import com.badmintonshop.repository.OrderRepository;
import com.badmintonshop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.badmintonshop.dto.PaymentResponse;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final InventoryService inventoryService;

    // --- 1. HÀM TẠO PAYMENT KHI CHECKOUT (BANK TRANSFER) ---

    @Transactional
    public Payment createBankTransferPayment(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Phương thức thanh toán không phải Bank Transfer.");
        }

        paymentRepository.findByOrder_OrderId(order.getOrderId()).ifPresent(p -> {
            throw new IllegalStateException("Payment record đã tồn tại cho Order ID: " + order.getOrderId());
        });

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        return paymentRepository.save(payment);
    }

    // --- 2. HÀM USER GỬI MINH CHỨNG CHUYỂN KHOẢN (KHÔI PHỤC LOGIC BỊ THIẾU) ---

    @Transactional
    public Payment confirmBankTransferProof(BankTransferRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        Payment payment = paymentRepository.findByOrder_OrderId(order.getOrderId())
                .orElseGet(() -> createBankTransferPayment(order));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Thanh toán đã được xử lý hoặc đang ở trạng thái không thể cập nhật.");
        }

        payment.setBankName(request.getSourceBankName());
        payment.setBankAccountNumber(request.getSourceAccountNumber());
        payment.setTransferReference(request.getTransferReference());
        payment.setTransferProofUrl(request.getTransferProofUrl());

        // Cập nhật trạng thái Order: WAITING_FOR_PAYMENT_VERIFICATION
        order.updateStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        orderStatusHistoryService.logStatusChange(order, order.getStatus(),
                "Khách hàng đã gửi minh chứng chuyển khoản, chờ Admin xác minh.");

        return paymentRepository.save(payment);
    }

    // --- 3. HÀM ADMIN LIST PAYMENTS (Cho PaymentAdminController) ---

    /**
     * Lấy danh sách tất cả các giao dịch và chuyển đổi sang DTO
     */
    public List<PaymentResponse> findAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Hàm chuyển đổi từ Entity sang DTO (Ánh xạ thủ công)
    private PaymentResponse convertToDto(Payment payment) {

        // Ánh xạ từng trường thủ công (sử dụng Builder)
        PaymentResponse dto = PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .bankName(payment.getBankName())
                .bankAccountNumber(payment.getBankAccountNumber())
                .transferReference(payment.getTransferReference())
                .PAIDAt(payment.getPaidAt())
                .FAILEDAt(payment.getFailedAt())
                .REFUNDEDAt(payment.getRefundedAt())
                .build();

        // Xử lý các trường LAZY/Liên kết (Order) thủ công
        if (payment.getOrder() != null) {
            dto.setOrderId(payment.getOrder().getOrderId());
            // dto.setOrderNumber(payment.getOrder().getOrderNumber());
        }
        return dto;
    }

    // --- 4. HÀM ADMIN VERIFY PAYMENT (Cho PaymentAdminController) ---

    @Transactional
    public Payment verifyPayment(Long paymentId, boolean success) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        Order order = payment.getOrder();

        // Kiểm tra trạng thái Payment trước
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Giao dịch này đã được xử lý trước đó.");
        }

        if (success) {
            // Trường hợp 1: Xác minh thành công (PAID & CONFIRMED)
            payment.markAsPaid("ADMIN_VERIFIED_" + paymentId);

            order.updateStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            orderStatusHistoryService.logStatusChange(order, OrderStatus.CONFIRMED,
                    "Admin đã xác minh thanh toán chuyển khoản thành công.");

        } else {
            // Trường hợp 2: Xác minh thất bại (FAILED & CANCELLED)
            payment.markAsFailed();

            order.cancel(CancelledBy.ADMIN, "Thanh toán chuyển khoản không hợp lệ.");
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            // [QUAN TRỌNG] Hoàn lại tồn kho
            inventoryService.restoreStock(order.getItems());

            orderStatusHistoryService.logStatusChange(order, OrderStatus.CANCELLED,
                    "Admin hủy đơn do minh chứng chuyển khoản không hợp lệ. Đã hoàn lại tồn kho.");
        }
        return paymentRepository.save(payment);
    }

    @Transactional
    public void processVNPaySuccess(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // 1. Cập nhật trạng thái đơn hàng
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // 2. Tìm payment tương ứng và cập nhật
        Payment payment = paymentRepository.findByOrder_OrderId(order.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }
}