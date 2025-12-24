package com.badmintonshop.service;

import com.badmintonshop.dto.product.ProductVariantDTO;
import com.badmintonshop.entity.Inventory;
import com.badmintonshop.entity.Product;
import com.badmintonshop.entity.ProductVariant;
import com.badmintonshop.entity.enums.VariantStatus;
import com.badmintonshop.repository.InventoryRepository;
import com.badmintonshop.repository.ProductRepository;
import com.badmintonshop.repository.ProductVariantRepository;
import com.badmintonshop.security.Auditable;
import com.badmintonshop.entity.enums.ActivityAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Product Variant operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * Get variants for a product
     */
    public List<ProductVariantDTO> getVariantsByProductId(Long productId) {
        return productVariantRepository.findByProductProductIdOrderByVariantIdAsc(productId).stream()
                .map(ProductVariantDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get active variants for a product
     */
    public List<ProductVariantDTO> getActiveVariantsByProductId(Long productId) {
        return productVariantRepository.findActiveVariantsByProductId(productId).stream()
                .map(ProductVariantDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get variant by ID
     */
    public Optional<ProductVariantDTO> getVariantById(Long variantId) {
        return productVariantRepository.findById(variantId)
                .map(ProductVariantDTO::fromEntity);
    }

    /**
     * Get variant by SKU
     */
    public Optional<ProductVariantDTO> getVariantBySku(String sku) {
        return productVariantRepository.findBySku(sku)
                .map(ProductVariantDTO::fromEntity);
    }

    /**
     * Create new variant and its corresponding inventory record.
     * Note: The variant status is kept as provided by the user (default ACTIVE).
     * We do NOT call checkAndUpdateVariantStatus here because initial inventory is
     * 0,
     * which would incorrectly set the status to INACTIVE.
     * Admin should update inventory quantity separately after creating the variant.
     */
    @Transactional
    @Auditable(entityType = "ProductVariant", action = ActivityAction.CREATE, description = "Created variant for product ID: {0}")
    public ProductVariantDTO createVariant(Long productId, ProductVariantDTO dto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + productId));

        // Check SKU uniqueness
        if (productVariantRepository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("SKU đã tồn tại: " + dto.getSku());
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(dto.getSku())
                .barcode(dto.getBarcode())
                .attributes(dto.getAttributes())
                .variantName(dto.getVariantName())
                .priceAdjustment(dto.getPriceAdjustment() != null ? dto.getPriceAdjustment() : BigDecimal.ZERO)
                .imageUrl(dto.getImageUrl())
                .status(dto.getStatus() != null ? dto.getStatus() : VariantStatus.ACTIVE)
                .build();

        variant = productVariantRepository.save(variant);

        // Create corresponding inventory record with default quantity 0
        Inventory inventory = Inventory.builder()
                .product(product)
                .variant(variant)
                .quantityAvailable(0)
                .quantityReserved(0)
                .quantitySold(0)
                .lowStockThreshold(10)
                .reorderPoint(20)
                .warehouseLocation("main")
                .updatedAt(LocalDateTime.now())
                .build();
        inventoryRepository.save(inventory);

        // Set hasVariants = true on product so it shows on public pages
        if (!Boolean.TRUE.equals(product.getHasVariants())) {
            product.setHasVariants(true);
            productRepository.save(product);
            log.info("Set hasVariants=true for product {}", productId);
        }

        log.info("Created variant {} for product {} with inventory record (quantity: 0, status: {})",
                variant.getSku(), productId, variant.getStatus());
        return ProductVariantDTO.fromEntity(variant);
    }

    /**
     * Update variant
     */
    @Transactional
    @Auditable(entityType = "ProductVariant", action = ActivityAction.UPDATE, description = "Updated variant ID: {1} for product ID: {0}")
    public ProductVariantDTO updateVariant(Long productId, Long variantId, ProductVariantDTO dto) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể: " + variantId));

        if (!variant.getProduct().getProductId().equals(productId)) {
            throw new IllegalArgumentException("Biến thể không thuộc sản phẩm này");
        }

        // Check SKU if changed
        if (dto.getSku() != null && !variant.getSku().equals(dto.getSku())) {
            if (productVariantRepository.existsBySku(dto.getSku())) {
                throw new IllegalArgumentException("SKU đã tồn tại: " + dto.getSku());
            }
            variant.setSku(dto.getSku());
        }

        if (dto.getBarcode() != null)
            variant.setBarcode(dto.getBarcode());
        if (dto.getAttributes() != null)
            variant.setAttributes(dto.getAttributes());
        if (dto.getVariantName() != null)
            variant.setVariantName(dto.getVariantName());
        if (dto.getPriceAdjustment() != null)
            variant.setPriceAdjustment(dto.getPriceAdjustment());
        if (dto.getImageUrl() != null)
            variant.setImageUrl(dto.getImageUrl());
        if (dto.getStatus() != null)
            variant.setStatus(dto.getStatus());

        variant = productVariantRepository.save(variant);
        log.info("Updated variant {} for product {}", variant.getSku(), productId);
        return ProductVariantDTO.fromEntity(variant);
    }

    /**
     * Delete variant (soft delete)
     */
    @Transactional
    @Auditable(entityType = "ProductVariant", action = ActivityAction.DELETE, description = "Soft deleted variant ID: {1} from product ID: {0}")
    public void deleteVariant(Long productId, Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể: " + variantId));

        if (!variant.getProduct().getProductId().equals(productId)) {
            throw new IllegalArgumentException("Biến thể không thuộc sản phẩm này");
        }

        variant.setDeletedAt(LocalDateTime.now());
        variant.setStatus(VariantStatus.INACTIVE);
        productVariantRepository.save(variant);
        log.info("Soft deleted variant {} from product {}", variantId, productId);
    }

    /**
     * Hard delete variant - also deletes associated inventory
     */
    @Transactional
    @Auditable(entityType = "ProductVariant", action = ActivityAction.DELETE, description = "Hard deleted variant ID: {1} from product ID: {0}")
    public void hardDeleteVariant(Long productId, Long variantId) {
        if (!productVariantRepository.existsByVariantIdAndProductProductId(variantId, productId)) {
            throw new IllegalArgumentException("Biến thể không thuộc sản phẩm này");
        }
        // Delete associated inventory first
        inventoryRepository.deleteByVariantVariantId(variantId);
        productVariantRepository.deleteById(variantId);
        log.info("Hard deleted variant {} from product {} (including inventory)", variantId, productId);
    }

    // ==================== TRASH FUNCTIONALITY ====================

    /**
     * Get deleted variants for trash page
     */
    public List<ProductVariantDTO> getDeletedVariants() {
        return productVariantRepository.findDeleted().stream()
                .map(ProductVariantDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Restore variant from trash with safeguard
     * Check if parent product is not soft-deleted
     */
    @Transactional
    @Auditable(entityType = "ProductVariant", action = ActivityAction.UPDATE, description = "Restored variant ID: {0}")
    public void restoreVariant(Long variantId) {
        ProductVariant variant = productVariantRepository.findByIdIncludingDeleted(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể: " + variantId));

        // Check if parent product is soft-deleted
        Long productId = variant.getProduct().getProductId();
        Optional<Product> productOpt = productRepository.findByIdIncludingDeleted(productId);
        if (productOpt.isPresent() && productOpt.get().getDeletedAt() != null) {
            throw new IllegalArgumentException(
                    String.format("Không thể khôi phục biến thể '%s' vì sản phẩm '%s' đã bị xóa. " +
                            "Vui lòng khôi phục sản phẩm trước.",
                            variant.getVariantName() != null ? variant.getVariantName() : variant.getSku(),
                            productOpt.get().getName()));
        }

        variant.setDeletedAt(null);
        variant.setStatus(VariantStatus.ACTIVE);
        productVariantRepository.save(variant);
        log.info("Restored variant {} (inventory is implicitly restored via variant relationship)", variantId);
    }

    /**
     * Hard delete variant from trash - also deletes associated inventory
     */
    @Transactional
    @Auditable(entityType = "ProductVariant", action = ActivityAction.DELETE, description = "Hard deleted variant ID: {0}")
    public void hardDeleteVariantFromTrash(Long variantId) {
        ProductVariant variant = productVariantRepository.findByIdIncludingDeleted(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể: " + variantId));

        // Delete associated inventory first
        inventoryRepository.deleteByVariantVariantId(variantId);
        productVariantRepository.delete(variant);
        log.info("Hard deleted variant {} from trash (including inventory)", variantId);
    }
}
