package com.badmintonshop.api.controller;

import com.badmintonshop.service.OrderService;
import com.badmintonshop.service.PaymentService; // <-- [1] THÊM IMPORT NÀY
import com.badmintonshop.service.VNPayService;
import com.badmintonshop.dto.BankTransferRequest; // <-- [1] THÊM IMPORT NÀY
import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.Payment; // <-- THÊM NẾU CHƯA CÓ
import com.badmintonshop.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final VNPayService vnpayService;
    private final PaymentService paymentService;
    private final com.badmintonshop.service.PaymentMethodConfigService paymentMethodConfigService; // <-- [2] THÊM
                                                                                                   // DEPENDENCY NÀY

    // ... (Giữ nguyên API /create và /vnpay-return) ...

    // API Public: Lấy danh sách phương thức thanh toán (Cho trang Checkout)
    // GET /api/payment/methods
    @GetMapping("/methods")
    public ResponseEntity<?> getActivePaymentMethods() {
        return ResponseEntity.ok(paymentMethodConfigService.getActiveMethods());
    }

    // Helper để lấy IP
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    // <-- [3] THÊM API BANK TRANSFER CONFIRM Ở ĐÂY

    /**
     * POST /api/payment/bank-transfer/confirm
     * API User gửi minh chứng chuyển khoản (Ảnh/URL)
     */
    @PostMapping("/bank-transfer/confirm")
    public ResponseEntity<?> confirmBankTransfer(@RequestBody BankTransferRequest request) {
        try {
            // Gọi Service để lưu thông tin chuyển khoản và cập nhật trạng thái Order
            Payment updatedPayment = paymentService.confirmBankTransferProof(request);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Minh chứng chuyển khoản đã được gửi. Đơn hàng đang chờ Admin xác minh thanh toán.",
                    "paymentId", updatedPayment.getPaymentId()));
        } catch (RuntimeException e) {
            // Xử lý các lỗi như Order không tồn tại, Payment đã được xử lý...
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "FAILURE", "message", e.getMessage()));
        }
    }

    /**
     * 1. POST /api/payment/vnpay/create/{orderId}
     * API để khách nhấn "Thanh toán VNPay", nhận về URL thanh toán
     */
    @PostMapping("/vnpay/create/{orderId}")
    public ResponseEntity<?> createVNPayUrl(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            // Lấy thông tin đơn hàng
            Order order = orderService.getOrderById(orderId);

            // Lấy IP client
            String clientIp = getClientIp(request);

            // Gọi service tạo URL
            String paymentUrl = vnpayService.createPaymentUrl(order, clientIp);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "paymentUrl", paymentUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "FAILURE", "message", e.getMessage()));
        }
    }

    /**
     * 2. GET /api/payment/vnpay/callback
     * VNPay sẽ gọi về API này sau khi khách thực hiện thanh toán xong
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<?> vnpayCallback(HttpServletRequest request) {
        Map<String, String> fields = vnpayService.getVNPayParams(request.getParameterMap());

        // Kiểm tra chữ ký bảo mật (Checksum)
        boolean isValidHash = vnpayService.validateVNPayResponse(fields);

        if (isValidHash) {
            String vnp_ResponseCode = fields.get("vnp_ResponseCode");
            String orderNumber = fields.get("vnp_TxnRef");

            // Mã "00" nghĩa là khách đã trả tiền thành công
            if ("00".equals(vnp_ResponseCode)) {
                // Gọi service xử lý logic nghiệp vụ (cập nhật DB, hoàn tồn kho nếu fail...)
                String result = orderService.handleVNPayReturn(orderNumber, vnp_ResponseCode,
                        fields.get("vnp_TransactionStatus"));

                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "orderNumber", orderNumber,
                        "message", result));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "FAILURE",
                        "message", "Thanh toán thất bại, mã lỗi: " + vnp_ResponseCode));
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chữ ký VNPay không hợp lệ");
    }

    /**
     * 3. GET /api/payment/vnpay/ipn
     * API Server-to-Server để VNPay gọi ngầm báo kết quả (Quan Trọng)
     */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<?> vnpayIpn(HttpServletRequest request) {
        Map<String, String> fields = vnpayService.getVNPayParams(request.getParameterMap());

        // 1. Validate Checksum
        boolean isValidHash = vnpayService.validateVNPayResponse(fields);

        if (isValidHash) {
            String orderNumber = fields.get("vnp_TxnRef");
            String vnp_ResponseCode = fields.get("vnp_ResponseCode");
            String vnp_TransactionStatus = fields.get("vnp_TransactionStatus");
            // String vnp_Amount = fields.get("vnp_Amount"); // Check amount if needed

            // 2. Process Order Status
            try {
                // Gọi chung logic với Callback nhưng đây là kênh Server-to-Server
                // Cần đảm bảo hàm handleVNPayReturn xử lý Idempotency (không update lại nếu đã
                // PAID) -> Đã check trong OrderService
                String result = orderService.handleVNPayReturn(orderNumber, vnp_ResponseCode, vnp_TransactionStatus);

                // 3. Return RspCode cho VNPay (Bat buoc format JSON: {RspCode: '00', Message:
                // 'Confirm Success'})
                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
            } catch (Exception e) {
                return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknow error"));
            }
        } else {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
        }
    }

}