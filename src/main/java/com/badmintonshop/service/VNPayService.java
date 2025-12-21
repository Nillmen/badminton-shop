package com.badmintonshop.service;

import com.badmintonshop.config.VNPayConfig;
import com.badmintonshop.entity.Order;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig vnpayConfig;

    /**
     * Tạo URL thanh toán VNPay
     * @param order Đối tượng Order cần thanh toán
     * @param clientIp IP của client
     * @return URL thanh toán
     */
    public String createPaymentUrl(Order order, String clientIp) throws UnsupportedEncodingException {

        // VNPay tính bằng VND (nhân 100). Đảm bảo TotalAmount không null.
        long amount = order.getTotalAmount().longValue() * 100;
        String vnp_TxnRef = order.getOrderNumber();
        String vnp_TmnCode = vnpayConfig.getVnp_TmnCode();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnpayConfig.getVnp_Version());
        vnp_Params.put("vnp_Command", vnpayConfig.getVnp_Command());
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_BankCode", ""); // Để trống, cho phép khách chọn ngân hàng
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", vnpayConfig.getVnp_OrderType());
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpayConfig.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", clientIp);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build URL
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                hashData.append(fieldName).append('=').append(fieldValue);
                query.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                hashData.append('&');
                query.append('&');
            }
        }

        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
            query.setLength(query.length() - 1);
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = vnpayConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnpayConfig.getVnp_PayUrl() + "?" + queryUrl;
    }

    /**
     * Chuyển Map<String, String[]> (từ request.getParameterMap()) thành Map<String, String>
     * @param parameterMap Map từ HttpServletRequest
     * @return Map<String, String>
     */
    public Map<String, String> getVNPayParams(Map<String, String[]> parameterMap) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue().length > 0) {
                params.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        return params;
    }

    /**
     * Xác thực phản hồi từ VNPay bằng Secure Hash
     * @param params Các tham số VNPay trả về
     * @return True nếu hash hợp lệ, False nếu không
     */
    public boolean validateVNPayResponse(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        if (vnp_SecureHash == null) {
            return false;
        }

        Map<String, String> hashParams = new HashMap<>(params);
        hashParams.remove("vnp_SecureHash");

        List<String> fieldNames = new ArrayList<>(hashParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = hashParams.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                hashData.append(fieldName).append('=').append(fieldValue);
                hashData.append('&');
            }
        }

        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }

        String calculatedHash = vnpayConfig.hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData.toString());
        return calculatedHash != null && calculatedHash.equalsIgnoreCase(vnp_SecureHash);
    }
}