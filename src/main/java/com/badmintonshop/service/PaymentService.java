package com.badmintonshop.service;

import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.enums.PaymentStatus;
import com.badmintonshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;

    public String createVnpayPaymentUrl(Order order) {
        // Mock VNPay URL generation
        // In real app: Build params, sign checksum, return URL
        return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?mock_token=" + order.getOrderNumber();
    }

    public void handleVnpayCallback(String vnpOrderInfo, String responseCode) {
        if ("00".equals(responseCode)) {
            // vnp_OrderInfo expected to be orderNumber based on createVnpayPaymentUrl
            Order order = orderRepository.findByOrderNumber(vnpOrderInfo)
                    .orElseThrow(() -> new RuntimeException("Order in callback not found: " + vnpOrderInfo));

            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setPaidAt(java.time.LocalDateTime.now());
                if (order.getStatus() == com.badmintonshop.entity.enums.OrderStatus.PENDING) {
                    order.setStatus(com.badmintonshop.entity.enums.OrderStatus.CONFIRMED);
                }
                orderRepository.save(order);
            }
        }
    }
}
