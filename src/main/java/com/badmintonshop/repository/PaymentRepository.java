package com.badmintonshop.repository;

import com.badmintonshop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Tìm kiếm bản ghi Payment dựa trên Order ID.
     * Cần thiết để tránh tạo trùng Payment hoặc cập nhật trạng thái
     */
    Optional<Payment> findByOrder_OrderId(Long orderId);

    // Có thể thêm các phương thức khác cho Admin Management sau này (findByStatus, etc.)
}