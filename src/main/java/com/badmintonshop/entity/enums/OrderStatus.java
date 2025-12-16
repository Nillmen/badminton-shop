package com.badmintonshop.entity.enums;

public enum OrderStatus {
    PENDING,        // Chờ xác nhận#
    WAITING_FOR_PAYMENT_VERIFICATION, // Chờ Admin xác minh chuyển khoản
    CONFIRMED,      // Đã xác nhận
    PROCESSING,     // Đang xử lý
    STRINGING,      // Đang đan vợt
    READY_TO_SHIP,  // Sẵn sàng giao
    SHIPPED,        // Đã giao cho vận chuyển
    DELIVERED,      // Đã giao thành công
    CANCELLED,      // Đã hủy
    REFUNDED        // Đã hoàn tiền
}
