package com.badmintonshop.repository;

import com.badmintonshop.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

        // Find cart by user
        Optional<Cart> findByUserUserId(Long userId);

        // Find cart by session (for guest)
        Optional<Cart> findBySessionId(String sessionId);

        // Find cart by user or session
        @Query("SELECT c FROM Cart c WHERE c.user.userId = :userId OR c.sessionId = :sessionId")
        Optional<Cart> findByUserIdOrSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);

        // Find cart with items (eagerly fetch all associations needed for display)
        @Query("SELECT DISTINCT c FROM Cart c " +
                        "LEFT JOIN FETCH c.items i " +
                        "LEFT JOIN FETCH i.product " +
                        "LEFT JOIN FETCH i.variant " +
                        "LEFT JOIN FETCH i.stringingService " +
                        "LEFT JOIN FETCH i.stringProduct " +
                        "WHERE c.user.userId = :userId")
        Optional<Cart> findByUserWithItems(@Param("userId") Long userId);

        @Query("SELECT DISTINCT c FROM Cart c " +
                        "LEFT JOIN FETCH c.items i " +
                        "LEFT JOIN FETCH i.product " +
                        "LEFT JOIN FETCH i.variant " +
                        "LEFT JOIN FETCH i.stringingService " +
                        "LEFT JOIN FETCH i.stringProduct " +
                        "WHERE c.sessionId = :sessionId")
        Optional<Cart> findBySessionWithItems(@Param("sessionId") String sessionId);

        // Find expired carts for cleanup
        List<Cart> findByExpiresAtBefore(LocalDateTime dateTime);

        // Delete expired carts
        void deleteByExpiresAtBefore(LocalDateTime dateTime);

        // Check if user has a cart
        boolean existsByUserUserId(Long userId);

        // Check if session has a cart
        boolean existsBySessionId(String sessionId);
}
