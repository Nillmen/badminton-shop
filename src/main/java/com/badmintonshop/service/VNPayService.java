package com.badmintonshop.service;

import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.Payment;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import com.badmintonshop.repository.OrderRepository;
import com.badmintonshop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for VNPay payment integration
 * Handles payment URL creation, callback/IPN processing with interruption handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    @Value("${vnpay.tmnCode:VNPAY_TMN}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret:VNPAY_SECRET}")
    private String vnpHashSecret;

    @Value("${vnpay.payUrl:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${vnpay.returnUrl:http://localhost:8080/api/payment/vnpay-return}")
    private String vnpReturnUrl;

    @Value("${vnpay.version:2.1.0}")
    private String vnpVersion;

    @Value("${vnpay.command:pay}")
    private String vnpCommand;

    /**
     * Create VNPay payment URL
     */
    public String createPaymentUrl(Long orderId, String ipAddress) throws UnsupportedEncodingException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Create payment record
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(PaymentMethod.VNPAY)
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        // Build VNPay parameters
        String vnpTxnRef = order.getOrderNumber(); // Use order number as transaction ref
        long amount = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue(); // VNPay requires amount * 100

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", vnpCommand);
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderNumber());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", ipAddress != null ? ipAddress : "127.0.0.1");

        // Create date in VNPay format
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        // Expire time (15 minutes)
        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // Build query string
        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();
        Iterator<Map.Entry<String, String>> itr = vnpParams.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                // Build hash data
                hashData.append(key);
                hashData.append('=');
                hashData.append(URLEncoder.encode(value, StandardCharsets.US_ASCII.toString()));
                
                // Build query
                query.append(URLEncoder.encode(key, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(value, StandardCharsets.US_ASCII.toString()));
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        // Calculate secure hash
        String vnpSecureHash = hmacSHA512(vnpHashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnpSecureHash);

        String paymentUrl = vnpPayUrl + "?" + query.toString();
        log.info("Created VNPay payment URL for order {}", order.getOrderNumber());

        return paymentUrl;
    }

    /**
     * Verify VNPay signature
     */
    public boolean verifySignature(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null) return false;
        
        Map<String, String> paramsToHash = new HashMap<>(params);
        paramsToHash.remove("vnp_SecureHash");
        paramsToHash.remove("vnp_SecureHashType");

        // Rebuild hash
        StringBuilder hashData = new StringBuilder();
        List<String> fieldNames = new ArrayList<>(paramsToHash.keySet());
        Collections.sort(fieldNames);
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = paramsToHash.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        String calculatedHash = hmacSHA512(vnpHashSecret, hashData.toString());
        return calculatedHash.equals(vnpSecureHash);
    }

    /**
     * Process VNPay callback (return URL) - for display only
     * @return true if payment successful
     */
    public boolean processCallback(Map<String, String> params) {
        log.info("Processing VNPay callback: {}", params);

        // Verify signature
        if (!verifySignature(params)) {
            log.error("VNPay signature verification failed");
            return false;
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        // Just return result for display - actual update happens in IPN
        return "00".equals(responseCode) && "00".equals(transactionStatus);
    }

    /**
     * Process VNPay IPN (Instant Payment Notification) - CRITICAL
     * This is the source of truth for payment status
     * @return IPN response for VNPay
     */
    @Transactional
    public Map<String, String> processIPN(Map<String, String> params) {
        log.info("Processing VNPay IPN: {}", params);
        Map<String, String> response = new HashMap<>();

        try {
            // 1. Verify checksum from VNPay
            if (!verifySignature(params)) {
                log.error("VNPay IPN: Invalid checksum");
                response.put("RspCode", "97");
                response.put("Message", "Invalid checksum");
                return response;
            }

            String txnRef = params.get("vnp_TxnRef"); // Order number
            String responseCode = params.get("vnp_ResponseCode");
            String transactionNo = params.get("vnp_TransactionNo");

            // 2. Find order
            Order order = orderRepository.findByOrderNumber(txnRef).orElse(null);
            if (order == null) {
                log.error("VNPay IPN: Order not found for TxnRef: {}", txnRef);
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return response;
            }

            // 3. Find payment
            Payment payment = paymentRepository.findFirstByOrderOrderIdOrderByPaymentIdDesc(order.getOrderId())
                    .orElse(null);
            if (payment == null) {
                log.error("VNPay IPN: Payment not found for order: {}", txnRef);
                response.put("RspCode", "01");
                response.put("Message", "Payment not found");
                return response;
            }

            // 4. Check if already processed (idempotent)
            if (payment.getStatus() == PaymentStatus.PAID) {
                log.info("VNPay IPN: Payment already processed for order: {}", txnRef);
                response.put("RspCode", "02");
                response.put("Message", "Already processed");
                return response;
            }

            // 5. Process based on response code
            if ("00".equals(responseCode)) {
                // SUCCESS - Payment completed
                handlePaymentSuccess(payment, order, transactionNo, params.toString());
                log.info("VNPay IPN: Payment SUCCESS for order {}", txnRef);
            } else {
                // FAILED - Payment failed or cancelled
                String failureReason = getFailureReason(responseCode);
                handlePaymentFailure(payment, order, responseCode, failureReason, params.toString());
                log.warn("VNPay IPN: Payment FAILED for order {}. Code: {}, Reason: {}", 
                        txnRef, responseCode, failureReason);
            }

            response.put("RspCode", "00");
            response.put("Message", "Success");

        } catch (Exception e) {
            log.error("VNPay IPN: Unknown error", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Handle successful payment
     */
    private void handlePaymentSuccess(Payment payment, Order order, String transactionNo, String gatewayResponse) {
        // Update payment
        payment.markAsPaid(transactionNo);
        payment.setGatewayResponse(gatewayResponse);
        paymentRepository.save(payment);

        // Update order
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.CONFIRMED); // Auto confirm on payment success
        orderRepository.save(order);
    }

    /**
     * Handle failed payment - restore inventory
     */
    private void handlePaymentFailure(Payment payment, Order order, String responseCode, String reason, String gatewayResponse) {
        // Update payment
        payment.markAsFailed();
        payment.setGatewayResponse(gatewayResponse + " | Reason: " + reason);
        paymentRepository.save(payment);

        // Restore inventory - important for interrupted payments
        try {
            inventoryService.restoreStockForOrder(order);
            log.info("Restored inventory for failed payment, order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to restore inventory for order: {}", order.getOrderNumber(), e);
        }

        // Update order status
        order.setPaymentStatus(PaymentStatus.FAILED);
        // Note: Order stays PENDING - user can retry payment
        orderRepository.save(order);
    }

    /**
     * Get human-readable failure reason from VNPay response code
     */
    public String getFailureReason(String responseCode) {
        return switch (responseCode) {
            case "07" -> "Trừ tiền thành công nhưng giao dịch bị nghi ngờ (liên hệ VNPay)";
            case "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ Internet Banking";
            case "10" -> "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Đã hết hạn chờ thanh toán (timeout 15 phút)";
            case "12" -> "Thẻ/Tài khoản bị khóa";
            case "13" -> "Sai mật khẩu xác thực giao dịch (OTP)";
            case "24" -> "Khách hàng hủy giao dịch";
            case "51" -> "Tài khoản không đủ số dư";
            case "65" -> "Tài khoản vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định";
            default -> "Lỗi không xác định (code: " + responseCode + ")";
        };
    }

    /**
     * HMAC SHA512 hash
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] hash = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error calculating HMAC SHA512", e);
            return "";
        }
    }
}

