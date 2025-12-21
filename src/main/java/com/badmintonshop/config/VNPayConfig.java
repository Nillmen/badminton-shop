package com.badmintonshop.config;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Formatter;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class VNPayConfig {

    // Đọc các giá trị từ application.properties
    @Value("${vnpay.payUrl}")
    private String vnp_PayUrl;

    @Value("${vnpay.returnUrl}")
    private String vnp_ReturnUrl;

    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;

    // Hằng số cố định cho VNPay
    private final String vnp_Version = "2.1.0";
    private final String vnp_Command = "pay";
    private final String vnp_OrderType = "other"; // Loại hàng hóa

    /**
     * Hàm Hash dữ liệu theo chuẩn SHA512 (sử dụng HMAC)
     * @param key SecretKey
     * @param data Dữ liệu cần hash
     * @return Chuỗi hash (String)
     */
    public String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA512");
            hmacSHA512.init(secretKey);

            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] hashBytes = hmacSHA512.doFinal(dataBytes);

            // Chuyển byte array sang Hex String
            Formatter formatter = new Formatter();
            for (byte b : hashBytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();

        } catch (Exception e) {
            // Log lỗi (nếu cần)
            return null;
        }
    }
}