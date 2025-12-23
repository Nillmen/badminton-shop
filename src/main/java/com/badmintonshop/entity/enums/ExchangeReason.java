package com.badmintonshop.entity.enums;

public enum ExchangeReason {
    WRONG_SIZE("Sai size/kích cỡ"),
    DEFECTIVE("Sản phẩm bị lỗi"),
    DAMAGED_ON_ARRIVAL("Hư hỏng khi nhận hàng"),
    OTHER("Lý do khác");

    private final String displayName;

    ExchangeReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
