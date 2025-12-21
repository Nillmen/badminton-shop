package com.badmintonshop.service;

import com.badmintonshop.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartCleanupService {

    private final CartRepository cartRepository;

    /**
     * Chạy mỗi ngày lúc 00:00:00
     * Xóa các giỏ hàng đã hết hạn (expires_at < now)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredCarts() {
        log.info("Starting scheduled cart cleanup...");

        LocalDateTime now = LocalDateTime.now();
        // Giả sử Repository có phương thức deleteByExpiresAtBefore
        // Nếu chưa có, cần thêm vào CartRepository: void
        // deleteByExpiresAtBefore(LocalDateTime now);
        // Hoặc dùng custom @Modifying query

        try {
            int deletedCount = cartRepository.deleteByExpiresAtBefore(now);
            log.info("Cleaning up carts expired before: {}. Deleted {} carts.", now, deletedCount);
        } catch (Exception e) {
            log.error("Error during cart cleanup: ", e);
        }

        log.info("Cart cleanup finished.");
    }
}
