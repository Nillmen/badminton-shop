package com.badmintonshop.repository;

import com.badmintonshop.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Hiện tại chưa cần query gì phức tạp, để trống là đủ dùng các hàm có sẵn
}