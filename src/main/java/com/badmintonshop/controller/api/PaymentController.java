package com.badmintonshop.controller.api;

import com.badmintonshop.dto.payment.BankTransferConfirmRequest;
import com.badmintonshop.dto.payment.PaymentDTO;
import com.badmintonshop.dto.payment.PaymentMethodDTO;
import com.badmintonshop.repository.PaymentMethodConfigRepository;
import com.badmintonshop.security.CustomOAuth2User;
import com.badmintonshop.security.CustomUserDetails;
import com.badmintonshop.service.PaymentService;
import com.badmintonshop.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Customer Payment API Controller
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final VNPayService vnPayService;
    private final PaymentMethodConfigRepository paymentMethodConfigRepository;

    /**
     * Get active payment methods
     */
    @GetMapping("/methods")
    public ResponseEntity<List<PaymentMethodDTO>> getPaymentMethods() {
        List<PaymentMethodDTO> methods = paymentMethodConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(PaymentMethodDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(methods);
    }

    /**
     * Create VNPay payment URL
     */
    @PostMapping("/vnpay/create")
    public ResponseEntity<Map<String, String>> createVNPayPayment(
            @RequestParam Long orderId,
            HttpServletRequest request) {
        try {
            String ipAddress = getClientIp(request);
            String paymentUrl = vnPayService.createPaymentUrl(orderId, ipAddress);
            
            Map<String, String> response = new HashMap<>();
            response.put("paymentUrl", paymentUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating VNPay payment", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * VNPay Return URL callback
     */
    @GetMapping("/vnpay/callback")
    public String vnpayCallback(@RequestParam Map<String, String> params) {
        log.info("VNPay callback received: {}", params);
        
        boolean success = vnPayService.processCallback(params);
        String txnRef = params.get("vnp_TxnRef");
        
        if (success) {
            // Redirect to success page
            return "redirect:/checkout/success?orderNumber=" + txnRef + "&payment=success";
        } else {
            // Redirect to failure page
            return "redirect:/checkout/success?orderNumber=" + txnRef + "&payment=failed";
        }
    }

    /**
     * VNPay IPN callback (server-to-server) - CRITICAL
     * This is the source of truth for payment status
     */
    @PostMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIPN(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN received: {}", params);
        
        // Process IPN and get response directly from VNPayService
        Map<String, String> response = vnPayService.processIPN(params);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Confirm bank transfer with proof
     */
    @PostMapping("/bank-transfer/confirm")
    public ResponseEntity<PaymentDTO> confirmBankTransfer(@RequestBody BankTransferConfirmRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        PaymentDTO payment = paymentService.confirmBankTransfer(userId, request);
        return ResponseEntity.ok(payment);
    }

    /**
     * Get payments for an order
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentDTO>> getOrderPayments(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrder(orderId));
    }

    // ======================== Helper Methods ========================

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        } else if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getUserId();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Multiple proxies
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
