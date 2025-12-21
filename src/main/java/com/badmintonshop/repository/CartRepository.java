package com.badmintonshop.repository;

import com.badmintonshop.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = { "items" })
    Optional<Cart> findByUser_UserId(Long userId);

    @EntityGraph(attributePaths = { "items" })
    java.util.List<Cart> findBySessionId(String sessionId);

    // Xóa Cart hết hạn
    int deleteByExpiresAtBefore(LocalDateTime now);
}