package com.badmintonshop.service;

import com.badmintonshop.entity.PaymentMethodConfig;
import com.badmintonshop.repository.PaymentMethodConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodConfigService {

    private final PaymentMethodConfigRepository paymentMethodConfigRepository;

    public List<PaymentMethodConfig> getAllConfigs() {
        return paymentMethodConfigRepository.findAll();
    }

    public List<PaymentMethodConfig> getActiveMethods() {
        return paymentMethodConfigRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional
    public PaymentMethodConfig updateConfig(Long id, PaymentMethodConfig request) {
        PaymentMethodConfig config = paymentMethodConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));

        // Chỉ cho phép update một số trường
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setIsActive(request.getIsActive());
        config.setDisplayOrder(request.getDisplayOrder());

        // Cập nhật cấu hình JSON (nếu có)
        if (request.getConfig() != null) {
            config.setConfig(request.getConfig());
        }

        return paymentMethodConfigRepository.save(config);
    }
}
