package com.badmintonshop.entity.enums;

public enum OrderStatus {
    PENDING, // Chờ xác nhận
    CONFIRMED, // Đã xác nhận
    PROCESSING, // Đang xử lý
    STRINGING, // Đang đan vợt
    READY_TO_SHIP, // Sẵn sàng giao
    SHIPPED, // Đã giao cho vận chuyển
    DELIVERED, // Đã giao thành công
    CANCELLED, // Đã hủy
    REFUNDED // Đã hoàn tiền
}
