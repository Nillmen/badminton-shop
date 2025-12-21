package com.badmintonshop.controller;

import com.badmintonshop.entity.Order;
import com.badmintonshop.service.OrderService;
import com.badmintonshop.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    @PostMapping("/vnpay/create")
    public ResponseEntity<Map<String, String>> createPayment(@RequestParam Long orderId) {
        Order order = orderService.getOrder(orderId);
        String url = paymentService.createVnpayPaymentUrl(order);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/vnpay/callback")
    public ResponseEntity<Void> vnpayCallback(@RequestParam Map<String, String> params) {
        // In real app, verify signature and redirect to frontend result page
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_OrderInfo = params.get("vnp_OrderInfo"); // Assuming it contains orderNumber
        paymentService.handleVnpayCallback(vnp_OrderInfo, vnp_ResponseCode);

        // Redirect to order history or success page
        return ResponseEntity.status(302).header("Location", "/orders").build();
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Void> vnpayIpn(@RequestParam Map<String, String> params) {
        // Server-to-server update
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_OrderInfo = params.get("vnp_OrderInfo");
        paymentService.handleVnpayCallback(vnp_OrderInfo, vnp_ResponseCode);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/methods")
    public ResponseEntity<Object> getPaymentMethods() {
        // Stub: Return list of PaymentMethodConfig
        return ResponseEntity.ok(java.util.List.of(
                Map.of("code", "COD", "name", "Cash on Delivery", "enabled", true),
                Map.of("code", "VNPAY", "name", "VNPay", "enabled", true),
                Map.of("code", "BANK_TRANSFER", "name", "Bank Transfer", "enabled", true)));
    }

    @PostMapping("/bank-transfer/confirm")
    public ResponseEntity<Void> confirmBankTransfer(@RequestBody Map<String, String> payload) {
        // Stub: Handle proof upload or confirmation
        // Long orderId = Long.parseLong(payload.get("orderId"));
        // String proofUrl = payload.get("proofUrl");
        return ResponseEntity.ok().build();
    }
}
