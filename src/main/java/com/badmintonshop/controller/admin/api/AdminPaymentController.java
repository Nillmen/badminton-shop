package com.badmintonshop.controller.admin.api;

import com.badmintonshop.dto.payment.PaymentDTO;
import com.badmintonshop.dto.payment.PaymentMethodDTO;
import com.badmintonshop.dto.payment.VerifyPaymentRequest;
import com.badmintonshop.entity.PaymentMethodConfig;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentMethodType;
import com.badmintonshop.entity.enums.PaymentStatus;
import com.badmintonshop.repository.PaymentMethodConfigRepository;
import com.badmintonshop.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Payment API Controller
 */
@RestController
@RequestMapping("/admin/api/payments")
@RequiredArgsConstructor
@Slf4j
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final PaymentMethodConfigRepository paymentMethodConfigRepository;

    // ======================== Payments ========================

    /**
     * Get all payments with filters
     */
    @GetMapping
    public ResponseEntity<Page<PaymentDTO>> getAllPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("paymentId").descending());
        Page<PaymentDTO> payments = paymentService.getAllPayments(status, method, fromDate, toDate, pageable);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get pending bank transfers
     */
    @GetMapping("/pending-bank-transfers")
    public ResponseEntity<List<PaymentDTO>> getPendingBankTransfers() {
        return ResponseEntity.ok(paymentService.getPendingBankTransfers());
    }

    /**
     * Verify bank transfer payment
     */
    @PutMapping("/{id}/verify")
    public ResponseEntity<PaymentDTO> verifyBankTransfer(
            @PathVariable Long id,
            @RequestBody VerifyPaymentRequest request) {
        PaymentDTO payment = paymentService.verifyBankTransfer(id, request);
        return ResponseEntity.ok(payment);
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    /**
     * Get payment statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPaymentStats() {
        return ResponseEntity.ok(paymentService.getPaymentStats());
    }

    // ======================== Payment Methods CRUD ========================

    /**
     * Get all payment method configs
     */
    @GetMapping("/methods")
    public ResponseEntity<List<PaymentMethodDTO>> getAllPaymentMethods() {
        List<PaymentMethodDTO> methods = paymentMethodConfigRepository.findAll().stream()
                .map(PaymentMethodDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(methods);
    }

    /**
     * Get payment method by ID
     */
    @GetMapping("/methods/{id}")
    public ResponseEntity<PaymentMethodDTO> getPaymentMethodById(@PathVariable Long id) {
        return paymentMethodConfigRepository.findById(id)
                .map(PaymentMethodDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create payment method
     */
    @PostMapping("/methods")
    public ResponseEntity<PaymentMethodDTO> createPaymentMethod(@RequestBody PaymentMethodDTO dto) {
        if (paymentMethodConfigRepository.existsByCode(dto.getCode())) {
            return ResponseEntity.badRequest().build();
        }

        PaymentMethodConfig config = PaymentMethodConfig.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .type(dto.getType() != null ? dto.getType() : PaymentMethodType.EWALLET)
                .logoUrl(dto.getLogoUrl())
                .description(dto.getDescription())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();

        config = paymentMethodConfigRepository.save(config);
        log.info("Created payment method: {}", config.getCode());

        return ResponseEntity.ok(PaymentMethodDTO.fromEntity(config));
    }

    /**
     * Update payment method
     */
    @PutMapping("/methods/{id}")
    public ResponseEntity<PaymentMethodDTO> updatePaymentMethod(
            @PathVariable Long id,
            @RequestBody PaymentMethodDTO dto) {
        
        return paymentMethodConfigRepository.findById(id)
                .map(config -> {
                    config.setName(dto.getName());
                    config.setLogoUrl(dto.getLogoUrl());
                    config.setDescription(dto.getDescription());
                    config.setIsActive(dto.getIsActive());
                    config.setDisplayOrder(dto.getDisplayOrder());
                    if (dto.getType() != null) {
                        config.setType(dto.getType());
                    }
                    
                    config = paymentMethodConfigRepository.save(config);
                    log.info("Updated payment method: {}", config.getCode());
                    return ResponseEntity.ok(PaymentMethodDTO.fromEntity(config));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete payment method
     */
    @DeleteMapping("/methods/{id}")
    public ResponseEntity<Void> deletePaymentMethod(@PathVariable Long id) {
        if (!paymentMethodConfigRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        paymentMethodConfigRepository.deleteById(id);
        log.info("Deleted payment method: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle payment method active status
     */
    @PutMapping("/methods/{id}/toggle")
    public ResponseEntity<PaymentMethodDTO> togglePaymentMethod(@PathVariable Long id) {
        return paymentMethodConfigRepository.findById(id)
                .map(config -> {
                    config.setIsActive(!config.getIsActive());
                    config = paymentMethodConfigRepository.save(config);
                    log.info("Toggled payment method {}: active={}", config.getCode(), config.getIsActive());
                    return ResponseEntity.ok(PaymentMethodDTO.fromEntity(config));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ======================== Payment Cleanup ========================

    private final com.badmintonshop.service.PaymentCleanupScheduler paymentCleanupScheduler;

    /**
     * Manually trigger VNPay order cleanup
     * Cleans up expired VNPay orders (both with and without payment records)
     */
    @PostMapping("/cleanup-expired")
    public ResponseEntity<Map<String, Object>> cleanupExpiredPayments() {
        log.info("Admin triggered manual VNPay cleanup");
        int count = paymentCleanupScheduler.manualCleanup();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Đã xử lý " + count + " đơn hàng VNPay hết hạn",
            "processedCount", count
        ));
    }
}
