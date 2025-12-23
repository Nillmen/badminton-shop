package com.badmintonshop.repository;

import com.badmintonshop.entity.PaymentMethodConfig;
import com.badmintonshop.entity.enums.PaymentMethodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PaymentMethodConfig entity
 */
@Repository
public interface PaymentMethodConfigRepository extends JpaRepository<PaymentMethodConfig, Long> {

    // Find active payment methods
    List<PaymentMethodConfig> findByIsActiveTrueOrderByDisplayOrderAsc();

    // Find by code
    Optional<PaymentMethodConfig> findByCode(String code);

    // Check if code exists
    boolean existsByCode(String code);

    // Find by type
    List<PaymentMethodConfig> findByType(PaymentMethodType type);

    // Find by type and active
    List<PaymentMethodConfig> findByTypeAndIsActiveTrue(PaymentMethodType type);
}
