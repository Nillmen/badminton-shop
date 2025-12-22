package com.badmintonshop.controller.api;

import com.badmintonshop.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API Controller cho Chatbot
 * Frontend sẽ gọi API này thay vì gọi trực tiếp OpenRouter
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Gửi tin nhắn và nhận phản hồi từ AI
     * POST /api/chatbot/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Message is required"));
        }

        try {
            String response = chatbotService.chat(message.trim());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "response", response));
        } catch (Exception e) {
            log.error("Chatbot error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "response", "Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau hoặc liên hệ hotline 1900 1234. 📞"));
        }
    }

    /**
     * Kiểm tra trạng thái chatbot
     * GET /api/chatbot/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "configured", chatbotService.isConfigured(),
                "status", chatbotService.isConfigured() ? "ready" : "not_configured"));
    }
}
