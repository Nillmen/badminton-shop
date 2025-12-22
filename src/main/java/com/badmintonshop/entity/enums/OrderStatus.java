package com.badmintonshop.entity.enums;

public enum OrderStatus {
    PENDING, // Chờ xác nhận
    CONFIRMED, // Đã xác nhận
    PROCESSING, // Đang xử lý
    STRINGING, // Đang đan vợt
    READY_TO_SHIP, // Sẵn sàng giao
    SHIPPING, // Đang giao hàng
    SHIPPED, // Đã giao cho vận chuyển
    DELIVERED, // Đã giao thành công
    CANCELLED, // Đã hủy
    RETURNED, // Trả hàng/Hoàn tiền
    REFUNDED // Đã hoàn tiền
}
