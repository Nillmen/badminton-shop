package com.badmintonshop.service;

import com.badmintonshop.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled task to clean up expired carts
 * Carts expire after 7 days of inactivity
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CartCleanupTask {

    private final CartRepository cartRepository;

    /**
     * Run every day at 3 AM to clean up expired carts
     * Cron: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredCarts() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Starting cart cleanup task at {}", now);
        
        try {
            // Find and delete carts that have expired
            long deletedCount = cartRepository.findByExpiresAtBefore(now).size();
            cartRepository.deleteByExpiresAtBefore(now);
            
            log.info("Cart cleanup completed. Deleted {} expired carts", deletedCount);
        } catch (Exception e) {
            log.error("Error during cart cleanup: {}", e.getMessage(), e);
        }
    }
}
