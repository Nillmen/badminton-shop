package com.badmintonshop.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankTransferRequest {

    // Order ID mà người dùng muốn xác nhận thanh toán
    private Long orderId;

    // Tên ngân hàng người dùng đã chuyển (ví dụ: Vietcombank, Techcombank)
    private String sourceBankName;

    // Số tài khoản người dùng đã chuyển
    private String sourceAccountNumber;

    // Tham chiếu (nội dung chuyển khoản)
    private String transferReference;

    // URL của ảnh/minh chứng chuyển khoản (hoặc Base64 String)
    // Giả định bro có cơ chế upload ảnh lên S3/Cloud Storage trước và gửi URL về
    private String transferProofUrl;
}