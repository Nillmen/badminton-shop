package com.badmintonshop.service;

import com.badmintonshop.dto.inventory.SupplierDTO;
import com.badmintonshop.entity.Product;
import com.badmintonshop.entity.ProductSupplier;
import com.badmintonshop.entity.Supplier;
import com.badmintonshop.repository.ProductRepository;
import com.badmintonshop.repository.ProductSupplierRepository;
import com.badmintonshop.repository.SupplierRepository;
import com.badmintonshop.security.Auditable;
import com.badmintonshop.entity.enums.ActivityAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Supplier operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final ProductRepository productRepository;

    /**
     * Get all suppliers
     */
    public Page<SupplierDTO> getAllSuppliers(Pageable pageable) {
        return supplierRepository.findAll(pageable)
                .map(SupplierDTO::fromEntity);
    }

    /**
     * Get all active suppliers
     */
    public List<SupplierDTO> getAllActiveSuppliers() {
        return supplierRepository.findAllActive().stream()
                .map(SupplierDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search suppliers
     */
    public Page<SupplierDTO> searchSuppliers(String keyword, Pageable pageable) {
        return supplierRepository.searchSuppliers(keyword, pageable)
                .map(SupplierDTO::fromEntity);
    }

    /**
     * Get supplier by ID
     */
    public Optional<SupplierDTO> getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .map(SupplierDTO::fromEntity);
    }

    /**
     * Create new supplier
     */
    @Transactional
    @Auditable(entityType = "Supplier", action = ActivityAction.CREATE, description = "Created supplier: {0}")
    public SupplierDTO createSupplier(SupplierDTO dto) {
        if (supplierRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Tên nhà cung cấp đã tồn tại: " + dto.getName());
        }

        // Auto-generate code if not provided
        String code = dto.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = "SUP-" + System.currentTimeMillis();
        } else if (supplierRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Mã nhà cung cấp đã tồn tại: " + code);
        }

        Supplier supplier = Supplier.builder()
                .name(dto.getName())
                .code(code)
                .contactName(dto.getContactName())
                .contactEmail(dto.getContactEmail())
                .contactPhone(dto.getContactPhone())
                .address(dto.getAddress())
                .city(dto.getCity())
                .country(dto.getCountry())
                .paymentTerms(dto.getPaymentTerms())
                .notes(dto.getNotes())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        supplier = supplierRepository.save(supplier);
        log.info("Created supplier: {}", supplier.getName());
        return SupplierDTO.fromEntity(supplier);
    }

    /**
     * Update supplier
     */
    @Transactional
    @Auditable(entityType = "Supplier", action = ActivityAction.UPDATE, description = "Updated supplier ID: {0}")
    public SupplierDTO updateSupplier(Long id, SupplierDTO dto) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp: " + id));

        if (dto.getName() != null && !supplier.getName().equals(dto.getName())) {
            if (supplierRepository.existsByName(dto.getName())) {
                throw new IllegalArgumentException("Tên nhà cung cấp đã tồn tại: " + dto.getName());
            }
            supplier.setName(dto.getName());
        }

        if (dto.getCode() != null)
            supplier.setCode(dto.getCode());
        if (dto.getContactName() != null)
            supplier.setContactName(dto.getContactName());
        if (dto.getContactEmail() != null)
            supplier.setContactEmail(dto.getContactEmail());
        if (dto.getContactPhone() != null)
            supplier.setContactPhone(dto.getContactPhone());
        if (dto.getAddress() != null)
            supplier.setAddress(dto.getAddress());
        if (dto.getCity() != null)
            supplier.setCity(dto.getCity());
        if (dto.getCountry() != null)
            supplier.setCountry(dto.getCountry());
        if (dto.getPaymentTerms() != null)
            supplier.setPaymentTerms(dto.getPaymentTerms());
        if (dto.getNotes() != null)
            supplier.setNotes(dto.getNotes());
        if (dto.getIsActive() != null)
            supplier.setIsActive(dto.getIsActive());

        supplier = supplierRepository.save(supplier);
        log.info("Updated supplier: {}", supplier.getName());
        return SupplierDTO.fromEntity(supplier);
    }

    /**
     * Delete supplier
     */
    @Transactional
    @Auditable(entityType = "Supplier", action = ActivityAction.DELETE, description = "Deleted supplier ID: {0}")
    public void deleteSupplier(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy nhà cung cấp: " + id);
        }
        supplierRepository.deleteById(id);
        log.info("Deleted supplier: {}", id);
    }

    /**
     * Get suppliers for a product - returns full info including supplier details
     */
    public List<java.util.Map<String, Object>> getSuppliersForProduct(Long productId) {
        return productSupplierRepository.findByProductProductId(productId).stream()
                .map(ps -> {
                    java.util.Map<String, Object> result = new java.util.HashMap<>();
                    result.put("supplierId", ps.getSupplier().getSupplierId());
                    result.put("supplierName", ps.getSupplier().getName());
                    result.put("supplierCode", ps.getSupplier().getCode());
                    result.put("isPrimary", ps.getIsPreferred() != null && ps.getIsPreferred());
                    result.put("costPrice", ps.getCostPrice());
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * Add supplier to product
     */
    @Transactional
    public void addSupplierToProduct(Long productId, Long supplierId, boolean isPreferred) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + productId));

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp: " + supplierId));

        if (productSupplierRepository.existsByProductProductIdAndSupplierSupplierId(productId, supplierId)) {
            throw new IllegalArgumentException("Nhà cung cấp đã được liên kết với sản phẩm này");
        }

        ProductSupplier productSupplier = ProductSupplier.builder()
                .product(product)
                .supplier(supplier)
                .isPreferred(isPreferred)
                .build();

        productSupplierRepository.save(productSupplier);
        log.info("Added supplier {} to product {}", supplierId, productId);
    }

    /**
     * Remove supplier from product
     */
    @Transactional
    public void removeSupplierFromProduct(Long productId, Long supplierId) {
        if (!productSupplierRepository.existsByProductProductIdAndSupplierSupplierId(productId, supplierId)) {
            throw new IllegalArgumentException("Không tìm thấy liên kết nhà cung cấp-sản phẩm");
        }
        productSupplierRepository.deleteByProductProductIdAndSupplierSupplierId(productId, supplierId);
        log.info("Removed supplier {} from product {}", supplierId, productId);
    }
}
