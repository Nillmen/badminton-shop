package com.badmintonshop.repository;

import com.badmintonshop.entity.PaymentMethodConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentMethodConfigRepository extends JpaRepository<PaymentMethodConfig, Long> {
    Optional<PaymentMethodConfig> findByCode(String code);

    // Lấy danh sách phương thức thanh toán đang hoạt động, sắp xếp theo thứ tự hiển
    // thị
    java.util.List<PaymentMethodConfig> findAllByIsActiveTrueOrderByDisplayOrderAsc();
}
