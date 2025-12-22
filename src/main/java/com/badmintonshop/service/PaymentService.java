package com.badmintonshop.service;

import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.enums.PaymentStatus;
import com.badmintonshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;

    public String createVnpayPaymentUrl(Order order) {
        log.info("Generating VNPay URL for order: {}", order.getOrderNumber());
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_TmnCode = "TESTCODE"; // Use ENV variable in prod
            String vnp_HashSecret = "TESTSECRET"; // Use ENV variable in prod
            String vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

            java.util.Map<String, String> vnp_Params = new java.util.HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount",
                    String.valueOf(order.getTotalAmount().multiply(new java.math.BigDecimal(100)).longValue()));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", order.getOrderNumber());
            vnp_Params.put("vnp_OrderInfo", "Payment for order " + order.getOrderNumber());
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", "http://localhost:8080/api/payments/vnpay/callback");
            vnp_Params.put("vnp_IpAddr", "127.0.0.1"); // Should get actual IP

            java.util.Calendar cld = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Etc/GMT+7"));
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            cld.add(java.util.Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            // Build data to hash
            String queryUrl = com.badmintonshop.utils.VnPayUtils.getPaymentURL(vnp_Params, true);
            String hashData = com.badmintonshop.utils.VnPayUtils.getPaymentURL(vnp_Params, false);
            String vnp_SecureHash = com.badmintonshop.utils.VnPayUtils.hmacSHA512(vnp_HashSecret, hashData);

            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            String finalUrl = vnp_Url + "?" + queryUrl;
            log.info("VNPay URL generated successfully: {}", finalUrl);
            return finalUrl;
        } catch (Exception e) {
            log.error("Error creating VNPay payment URL: ", e);
            throw new RuntimeException("Could not generate VNPay URL", e);
        }
    }

    public void handleVnpayCallback(java.util.Map<String, String> requestParams) {
        String vnp_HashSecret = "TESTSECRET"; // Must match create

        java.util.Map<String, String> fields = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, String> entry : requestParams.entrySet()) {
            if ((entry.getKey() != null) && (entry.getKey().length() > 0) &&
                    (entry.getKey().startsWith("vnp_"))) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }

        String vnp_SecureHash = requestParams.get("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }

        String signValue = com.badmintonshop.utils.VnPayUtils.hmacSHA512(vnp_HashSecret,
                com.badmintonshop.utils.VnPayUtils.getPaymentURL(fields, false));

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(requestParams.get("vnp_ResponseCode"))) {
                String vnpOrderInfo = requestParams.get("vnp_TxnRef"); // Order Number
                Order order = orderRepository.findByOrderNumber(vnpOrderInfo)
                        .orElseThrow(() -> new RuntimeException("Order not found"));

                if (order.getPaymentStatus() != PaymentStatus.PAID) {
                    order.setPaymentStatus(PaymentStatus.PAID);
                    order.setPaidAt(java.time.LocalDateTime.now());
                    if (order.getStatus() == com.badmintonshop.entity.enums.OrderStatus.PENDING) {
                        order.setStatus(com.badmintonshop.entity.enums.OrderStatus.CONFIRMED);
                    }
                    orderRepository.save(order);
                }
            }
        } else {
            // Handle invalid signature
            throw new RuntimeException("Invalid Signature");
        }
    }
}
