package com.badmintonshop.service;

import com.badmintonshop.dto.request.StringProductRequest;
import com.badmintonshop.dto.response.StringProductResponse;
import com.badmintonshop.entity.Brand;
import com.badmintonshop.entity.StringProduct;
import com.badmintonshop.entity.enums.StringType;
import com.badmintonshop.exception.ResourceNotFoundException;
import com.badmintonshop.repository.BrandRepository;
import com.badmintonshop.repository.OrderItemRepository;
import com.badmintonshop.repository.StringProductRepository;
import com.badmintonshop.security.Auditable;
import com.badmintonshop.entity.enums.ActivityAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StringProductService {

    private final StringProductRepository stringProductRepository;
    private final BrandRepository brandRepository;
    private final OrderItemRepository orderItemRepository;

    public List<StringProductResponse> getAll() {
        return stringProductRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<StringProductResponse> getActiveStrings() {
        return stringProductRepository.findAll().stream()
                .filter(StringProduct::getIsActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public StringProductResponse getById(Long id) {
        StringProduct stringProduct = stringProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("String product not found with id: " + id));
        return mapToResponse(stringProduct);
    }

    @Transactional
    @Auditable(entityType = "StringProduct", action = ActivityAction.CREATE, description = "Created string product: {0}")
    public StringProductResponse create(StringProductRequest request) {
        StringProduct stringProduct = mapToEntity(request);
        StringProduct saved = stringProductRepository.save(stringProduct);
        return mapToResponse(saved);
    }

    @Transactional
    @Auditable(entityType = "StringProduct", action = ActivityAction.UPDATE, description = "Updated string product ID: {0}")
    public StringProductResponse update(Long id, StringProductRequest request) {
        StringProduct existing = stringProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("String product not found with id: " + id));

        updateEntity(existing, request);
        StringProduct saved = stringProductRepository.save(existing);
        return mapToResponse(saved);
    }

    /**
     * Soft delete string product - blocks if has OrderItem references
     */
    @Transactional
    @Auditable(entityType = "StringProduct", action = ActivityAction.DELETE, description = "Soft deleted string product ID: {0}")
    public void delete(Long id) {
        StringProduct stringProduct = stringProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cước vợt với ID: " + id));

        // Check for OrderItem references
        long orderItemCount = orderItemRepository.countByStringProductStringId(id);
        if (orderItemCount > 0) {
            throw new IllegalArgumentException(
                    String.format("Không thể xóa cước '%s' vì có %d đơn hàng đang sử dụng. " +
                            "Vui lòng đợi đến khi các đơn hàng hoàn thành hoặc liên hệ quản trị viên.",
                            stringProduct.getName(), orderItemCount));
        }

        // Soft delete
        stringProduct.setDeletedAt(LocalDateTime.now());
        stringProduct.setIsActive(false);
        stringProductRepository.save(stringProduct);
        log.info("Soft deleted string product: {} (ID: {})", stringProduct.getName(), id);
    }

    @Transactional
    public void toggleStatus(Long id) {
        StringProduct existing = stringProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("String product not found with id: " + id));
        existing.setIsActive(!existing.getIsActive());
        stringProductRepository.save(existing);
    }

    // ===== Trash Operations =====

    /**
     * Get all soft-deleted strings for trash
     */
    public List<StringProductResponse> getDeletedStrings() {
        List<Object[]> deletedRows = stringProductRepository.findDeleted();
        List<StringProductResponse> result = new ArrayList<>();

        for (Object[] row : deletedRows) {
            result.add(mapDeletedRowToResponse(row));
        }

        return result;
    }

    /**
     * Restore string from trash - blocks if Brand is soft-deleted
     */
    @Transactional
    @Auditable(entityType = "StringProduct", action = ActivityAction.UPDATE, description = "Restored string product ID: {0}")
    public void restoreString(Long id) {
        StringProduct stringProduct = stringProductRepository.findByIdIncludingDeleted(id)
                .orElseThrow(
                        () -> new IllegalArgumentException("Không tìm thấy cước vợt trong thùng rác với ID: " + id));

        if (stringProduct.getDeletedAt() == null) {
            throw new IllegalArgumentException("Cước vợt này chưa bị xóa.");
        }

        // Check if brand is still active (not soft-deleted)
        if (stringProduct.getBrand() != null) {
            Brand brand = brandRepository.findById(stringProduct.getBrand().getBrandId()).orElse(null);
            if (brand == null || brand.getDeletedAt() != null) {
                throw new IllegalArgumentException(
                        String.format("Không thể khôi phục cước '%s' vì thương hiệu '%s' đã bị xóa. " +
                                "Vui lòng khôi phục thương hiệu trước.",
                                stringProduct.getName(),
                                stringProduct.getBrand() != null ? stringProduct.getBrand().getName()
                                        : "không xác định"));
            }
        }

        // Restore
        stringProduct.setDeletedAt(null);
        stringProduct.setIsActive(true);
        stringProductRepository.save(stringProduct);
        log.info("Restored string product: {} (ID: {})", stringProduct.getName(), id);
    }

    /**
     * Hard delete string from trash - blocks if has OrderItem references
     */
    @Transactional
    @Auditable(entityType = "StringProduct", action = ActivityAction.DELETE, description = "Hard deleted string product ID: {0}")
    public void hardDeleteString(Long id) {
        StringProduct stringProduct = stringProductRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cước vợt với ID: " + id));

        // Check for OrderItem references
        long orderItemCount = orderItemRepository.countByStringProductStringId(id);
        if (orderItemCount > 0) {
            throw new IllegalArgumentException(
                    String.format("Không thể xóa vĩnh viễn cước '%s' vì có %d đơn hàng đang liên kết. " +
                            "Dữ liệu lịch sử đơn hàng yêu cầu giữ lại thông tin cước này.",
                            stringProduct.getName(), orderItemCount));
        }

        stringProductRepository.delete(stringProduct);
        log.info("Hard deleted string product: {} (ID: {})", stringProduct.getName(), id);
    }

    // ===== Private Mappers =====

    private StringProductResponse mapToResponse(StringProduct entity) {
        return StringProductResponse.builder()
                .stringId(entity.getStringId())
                .name(entity.getName())
                .sku(entity.getSku())
                .description(entity.getDescription())
                .brandId(entity.getBrand() != null ? entity.getBrand().getBrandId() : null)
                .brandName(entity.getBrand() != null ? entity.getBrand().getName() : null)
                .stringType(entity.getStringType())
                .gauge(entity.getGauge())
                .material(entity.getMaterial())
                .durabilityRating(entity.getDurabilityRating())
                .repulsionRating(entity.getRepulsionRating())
                .controlRating(entity.getControlRating())
                .hittingSoundRating(entity.getHittingSoundRating())
                .recommendedTensionMin(entity.getRecommendedTensionMin())
                .recommendedTensionMax(entity.getRecommendedTensionMax())
                .tensionRange(entity.getTensionRange())
                .retailPrice(entity.getRetailPrice())
                .lengthPerRoll(entity.getLengthPerRoll())
                .quantityInStock(entity.getQuantityInStock())
                .color(entity.getColor())
                .imageUrl(entity.getImageUrl())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    /**
     * Map native query result row to response (for deleted strings)
     */
    private StringProductResponse mapDeletedRowToResponse(Object[] row) {
        // Row structure from findDeleted() native query:
        // string_id, name, sku, brand_id, string_type, retail_price, image_url,
        // is_active, deleted_at, brand_name

        return StringProductResponse.builder()
                .stringId(row[0] != null ? ((Number) row[0]).longValue() : null)
                .name((String) row[1])
                .sku((String) row[2])
                .brandId(row[3] != null ? ((Number) row[3]).longValue() : null)
                .stringType(row[4] != null ? StringType.valueOf(row[4].toString().toUpperCase()) : null)
                .retailPrice(row[5] != null ? new BigDecimal(row[5].toString()) : null)
                .imageUrl((String) row[6])
                .isActive(row[7] != null && Boolean.TRUE.equals(row[7]))
                .deletedAt(row[8] != null ? ((java.sql.Timestamp) row[8]).toLocalDateTime() : null)
                .brandName((String) row[9])
                .build();
    }

    private StringProduct mapToEntity(StringProductRequest request) {
        StringProduct.StringProductBuilder builder = StringProduct.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .stringType(request.getStringType())
                .gauge(request.getGauge())
                .material(request.getMaterial())
                .durabilityRating(request.getDurabilityRating())
                .repulsionRating(request.getRepulsionRating())
                .controlRating(request.getControlRating())
                .hittingSoundRating(request.getHittingSoundRating())
                .recommendedTensionMin(request.getRecommendedTensionMin())
                .recommendedTensionMax(request.getRecommendedTensionMax())
                .retailPrice(request.getRetailPrice())
                .lengthPerRoll(request.getLengthPerRoll())
                .quantityInStock(request.getQuantityInStock())
                .color(request.getColor())
                .imageUrl(request.getImageUrl())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true);

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));
            builder.brand(brand);
        }

        return builder.build();
    }

    private void updateEntity(StringProduct entity, StringProductRequest request) {
        entity.setName(request.getName());
        entity.setSku(request.getSku());
        entity.setDescription(request.getDescription());
        entity.setStringType(request.getStringType());
        entity.setGauge(request.getGauge());
        entity.setMaterial(request.getMaterial());
        entity.setDurabilityRating(request.getDurabilityRating());
        entity.setRepulsionRating(request.getRepulsionRating());
        entity.setControlRating(request.getControlRating());
        entity.setHittingSoundRating(request.getHittingSoundRating());
        entity.setRecommendedTensionMin(request.getRecommendedTensionMin());
        entity.setRecommendedTensionMax(request.getRecommendedTensionMax());
        entity.setRetailPrice(request.getRetailPrice());
        entity.setLengthPerRoll(request.getLengthPerRoll());
        entity.setQuantityInStock(request.getQuantityInStock());
        entity.setColor(request.getColor());
        entity.setImageUrl(request.getImageUrl());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));
            entity.setBrand(brand);
        } else {
            entity.setBrand(null);
        }
    }
}
