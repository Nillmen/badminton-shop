package com.badmintonshop.controller;

import com.badmintonshop.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class VNPayReturnController {

    private final VNPayService vnPayService;

    @GetMapping("/api/payment/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> params, HttpServletRequest request) {
        log.info("VNPay Return URL called with params: {}", params);

        // Debug: Log complete request URL to check encoding issues
        try {
            // Note: Spring already decodes parameters in @RequestParam map
            // We pass them to service for verification
            boolean signatureValid = vnPayService.verifySignature(params);
            
            if (!signatureValid) {
                log.error("VNPay Return: Invalid Signature");
                return "redirect:/checkout/failure?reason=invalid_signature"; 
                // Or /checkout/success with error flag if failure page not exists
            }

            String responseCode = params.get("vnp_ResponseCode");
            String txnRef = params.get("vnp_TxnRef");
            
            if ("00".equals(responseCode)) {
                log.info("VNPay Return: Payment Success for order {}", txnRef);
                // In production, you might want to call transaction check here 
                // but usually we rely on IPN for status update. 
                // For UI, we just show success.
                return "redirect:/checkout/success?orderNumber=" + txnRef;
            } else {
                log.warn("VNPay Return: Payment Failed for order {}, code: {}", txnRef, responseCode);
                return "redirect:/checkout/success?orderNumber=" + txnRef + "&error=payment_failed&code=" + responseCode;
            }

        } catch (Exception e) {
            log.error("Error processing VNPay return", e);
            return "redirect:/checkout?error=system_error";
        }
    }
}
