package com.badmintonshop.api.controller.admin;

import com.badmintonshop.service.PaymentService;
import com.badmintonshop.entity.Payment;
import com.badmintonshop.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.badmintonshop.dto.PaymentResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/payments")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentService paymentService;
    private final com.badmintonshop.service.PaymentMethodConfigService paymentMethodConfigService;

    // API 1: Lấy danh sách tất cả các giao dịch (Admin Dashboard)
    // GET /admin/api/payments
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> listAllPayments() {
        List<PaymentResponse> payments = paymentService.findAllPayments();
        return ResponseEntity.ok(payments);
    }

    // API 2: Xác minh giao dịch chuyển khoản ngân hàng (Logic quan trọng)
    // PUT /admin/api/payments/{paymentId}/verify?success=true
    @PutMapping("/{paymentId}/verify")
    public ResponseEntity<?> verifyBankTransfer(
            @PathVariable Long paymentId,
            @RequestParam boolean success) {
        try {
            // Gọi service để xử lý xác minh, cập nhật trạng thái Order và Payment
            Payment updatedPayment = paymentService.verifyPayment(paymentId, success);

            String message = success
                    ? "Xác minh thanh toán thành công. Đơn hàng đã được CONFIRMED."
                    : "Xác minh thanh toán thất bại. Đơn hàng đã bị CANCELLED và tồn kho được hoàn lại.";

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", message,
                    "paymentId", updatedPayment.getPaymentId(),
                    "orderStatus", updatedPayment.getOrder().getStatus().toString()));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "FAILURE", "message", e.getMessage()));
        } catch (IllegalStateException e) {
            // Xử lý trường hợp giao dịch đã được xử lý (tránh xử lý trùng)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "FAILURE", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "FAILURE", "message", "Lỗi xử lý xác minh: " + e.getMessage()));
        }
    }

    // API 3: Quản lý cấu hình phương thức thanh toán
    // GET /admin/api/payments/methods
    @GetMapping("/methods")
    public ResponseEntity<List<com.badmintonshop.entity.PaymentMethodConfig>> getAllPaymentMethods() {
        return ResponseEntity.ok(paymentMethodConfigService.getAllConfigs());
    }

    // PUT /admin/api/payments/methods/{id}
    @PutMapping("/methods/{id}")
    public ResponseEntity<com.badmintonshop.entity.PaymentMethodConfig> updatePaymentMethod(
            @PathVariable Long id,
            @RequestBody com.badmintonshop.entity.PaymentMethodConfig config) {
        return ResponseEntity.ok(paymentMethodConfigService.updateConfig(id, config));
    }
}