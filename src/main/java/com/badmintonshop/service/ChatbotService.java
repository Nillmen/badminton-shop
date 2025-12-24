package com.badmintonshop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service để gọi OpenRouter AI API
 * API key được lưu trong application-secrets.properties để bảo mật
 * Sử dụng kiến thức động từ KnowledgeGeneratorService
 */
@Service
@Slf4j
public class ChatbotService {

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String apiUrl;

    @Value("${openrouter.model:meta-llama/llama-3.2-3b-instruct:free}")
    private String model;

    private final RestTemplate restTemplate;
    private final KnowledgeGeneratorService knowledgeGeneratorService;

    public ChatbotService(KnowledgeGeneratorService knowledgeGeneratorService) {
        this.restTemplate = new RestTemplate();
        this.knowledgeGeneratorService = knowledgeGeneratorService;
    }

    /**
     * Lấy kiến thức động từ KnowledgeGeneratorService
     */
    private String getKnowledgeBase() {
        return knowledgeGeneratorService.getKnowledge();
    }

    /**
     * Check if API is configured
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_API_KEY");
    }

    /**
     * Gọi OpenRouter API để lấy response từ AI
     */
    public String chat(String userMessage) {
        if (!isConfigured()) {
            log.warn("OpenRouter API key not configured");
            return getFallbackResponse(userMessage);
        }

        try {
            // Lấy kiến thức động từ database
            String knowledgeBase = getKnowledgeBase();

            // Build system prompt
            String systemPrompt = String.format("""
                    Bạn là trợ lý AI thân thiện của Badminton Shop - cửa hàng cầu lông chính hãng.

                    THÔNG TIN CỬA HÀNG (CẬP NHẬT THỜI GIAN THỰC):
                    %s

                    HƯỚNG DẪN:
                    - Trả lời bằng tiếng Việt, thân thiện và chuyên nghiệp
                    - Sử dụng emoji phù hợp để tạo sự thân thiện
                    - Nếu không biết câu trả lời, hướng dẫn liên hệ hotline 1900 1234
                    - Trả lời ngắn gọn, dễ hiểu (tối đa 150 từ)
                    - Ưu tiên giới thiệu sản phẩm và khuyến mãi đang có
                    """, knowledgeBase);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userMessage));
            requestBody.put("messages", messages);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", "https://badmintonshop.com");
            headers.set("X-Title", "Badminton Shop Chatbot");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Calling OpenRouter API for message: {}",
                    userMessage.substring(0, Math.min(50, userMessage.length())));

            // Call API
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class);

            // Parse response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        String content = (String) message.get("content");
                        log.info("OpenRouter API response received successfully");
                        return content;
                    }
                }
            }

            log.warn("Invalid response from OpenRouter API");
            return getFallbackResponse(userMessage);

        } catch (Exception e) {
            log.error("Error calling OpenRouter API: {}", e.getMessage());
            return getFallbackResponse(userMessage);
        }
    }

    /**
     * Fallback response khi API không hoạt động
     */
    private String getFallbackResponse(String message) {
        String lowerMessage = message.toLowerCase();

        Map<String, String> responses = Map.ofEntries(
                Map.entry("đặt hàng",
                        "Để đặt hàng, bạn có thể:\n• Đặt hàng online 24/7 qua website\n• Thanh toán: COD, chuyển khoản, Momo, ZaloPay 📱"),
                Map.entry("giao hàng",
                        "Chính sách giao hàng:\n• Nội thành: 1-2 ngày\n• Tỉnh khác: 2-5 ngày\n• Miễn phí ship đơn từ 500.000đ 🚚"),
                Map.entry("ship", "Phí ship:\n• Miễn phí cho đơn từ 500.000đ\n• Đơn dưới 500.000đ: phí 30.000đ 🚀"),
                Map.entry("đổi trả", "Đổi trả miễn phí trong 7 ngày nếu lỗi nhà sản xuất ✅"),
                Map.entry("bảo hành", "Bảo hành:\n• Vợt: 6-12 tháng\n• Giày: 3 tháng\n• Túi: 6 tháng 🛡️"),
                Map.entry("liên hệ", "Hotline: 1900 1234\nEmail: support@badmintonshop.com 📞"),
                Map.entry("xin chào", "Xin chào! 👋 Tôi có thể giúp gì cho bạn?"),
                Map.entry("hello", "Xin chào! 👋 Tôi là trợ lý AI của Badminton Shop."));

        for (Map.Entry<String, String> entry : responses.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "Cảm ơn bạn đã liên hệ! 😊\n\nĐể được hỗ trợ nhanh nhất:\n• Hotline: 1900 1234\n• Email: support@badmintonshop.com";
    }
}
