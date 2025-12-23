package com.badmintonshop.entity.enums;

public enum IssueType {
    FRAME_CRACK("Nứt khung vợt"),
    SHAFT_BREAK("Gãy thân vợt"),
    PAINT_PEEL("Tróc sơn"),
    STRING_BREAK("Đứt cước"),
    OTHER("Lỗi khác");

    private final String displayName;

    IssueType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
