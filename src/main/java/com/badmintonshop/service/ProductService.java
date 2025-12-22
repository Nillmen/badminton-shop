package com.badmintonshop.service;

import com.badmintonshop.dto.product.ProductListDTO;
import com.badmintonshop.dto.product.ProductRequest;
import com.badmintonshop.dto.product.ProductResponse;
import com.badmintonshop.entity.Brand;
import com.badmintonshop.entity.Category;
import com.badmintonshop.entity.Inventory;
import com.badmintonshop.entity.Product;
import com.badmintonshop.entity.ProductImage;
import com.badmintonshop.entity.enums.ProductStatus;
import com.badmintonshop.entity.enums.ProductType;
import com.badmintonshop.entity.enums.VariantStatus;
import com.badmintonshop.repository.BrandRepository;
import com.badmintonshop.repository.CategoryRepository;
import com.badmintonshop.repository.InventoryRepository;
import com.badmintonshop.repository.ProductImageRepository;
import com.badmintonshop.repository.ProductRepository;
import com.badmintonshop.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Service for Product operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryRepository inventoryRepository;
    private final PromotionPriceService promotionPriceService;
    private final JdbcTemplate jdbcTemplate;
    private final ProductVariantRepository productVariantRepository;

    /**
     * Get all products with pagination (with promotion prices applied)
     */
    public Page<ProductListDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::mapToProductListDTO);
    }

    /**
     * Get product by ID
     */
    public Optional<ProductResponse> getProductById(Long id) {
        return productRepository.findByIdWithImages(id)
                .map(this::mapToProductResponse);
    }

    /**
     * Get product by slug (public-facing, only returns products with variants)
     */
    public Optional<ProductResponse> getProductBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .filter(p -> Boolean.TRUE.equals(p.getHasVariants()))
                .map(this::mapToProductResponse);
    }

    /**
     * Get featured products
     */
    public List<ProductListDTO> getFeaturedProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findFeaturedProducts(pageable)
                .getContent().stream()
                .map(this::mapToProductListDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get new arrivals
     */
    public List<ProductListDTO> getNewArrivals(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findNewArrivals(pageable)
                .getContent().stream()
                .map(this::mapToProductListDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get best sellers
     */
    public List<ProductListDTO> getBestSellers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findBestSellers(pageable)
                .getContent().stream()
                .map(this::mapToProductListDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get products on sale
     */
    public Page<ProductListDTO> getProductsOnSale(Pageable pageable) {
        return productRepository.findProductsOnSale(pageable)
                .map(this::mapToProductListDTO);
    }

    /**
     * Search products by keyword
     */
    public Page<ProductListDTO> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchByKeyword(keyword, pageable)
                .map(this::mapToProductListDTO);
    }

    /**
     * Advanced search with filters (public-facing, only shows products with
     * variants)
     */
    public Page<ProductListDTO> searchProductsAdvanced(
            String keyword,
            Long categoryId,
            Long brandId,
            ProductType productType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductStatus status,
            Boolean isPublished,
            Pageable pageable) {
        return productRepository.searchProducts(
                keyword, categoryId, brandId, productType,
                minPrice, maxPrice, status, pageable)
                .map(this::mapToProductListDTO);
    }

    /**
     * Advanced search with filters for admin (shows ALL products including those
     * without variants)
     */
    public Page<ProductListDTO> searchProductsAdvancedAdmin(
            String keyword,
            Long categoryId,
            Long brandId,
            ProductType productType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductStatus status,
            Boolean isPublished,
            Pageable pageable) {
        return productRepository.searchProductsAdmin(
                keyword, categoryId, brandId, productType,
                minPrice, maxPrice, status, pageable)
                .map(this::mapToProductListDTO);
    }

    /**
     * Get products by category
     */
    public Page<ProductListDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable)
                .map(this::mapToProductListDTO);
    }

    /**
     * Get products by brand
     */
    public Page<ProductListDTO> getProductsByBrand(Long brandId, Pageable pageable) {
        return productRepository.findByBrandId(brandId, pageable)
                .map(this::mapToProductListDTO);
    }

    /**
     * Create new product
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        // Generate slug
        String slug = request.getSlug();
        if (slug == null || slug.isEmpty()) {
            slug = generateSlug(request.getName());
        }

        // Check slug uniqueness
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        // Check SKU uniqueness
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("SKU đã tồn tại: " + request.getSku());
        }

        // Get category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục: " + request.getCategoryId()));

        // Get brand (optional)
        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Không tìm thấy thương hiệu: " + request.getBrandId()));
        }

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .sku(request.getSku())
                .category(category)
                .brand(brand)
                .productType(request.getProductType())
                .shortDescription(request.getShortDescription())
                .fullDescription(request.getFullDescription())
                .basePrice(request.getBasePrice())
                .compareAtPrice(request.getCompareAtPrice())
                .costPrice(request.getCostPrice())
                .weight(request.getWeight())
                .dimensions(request.getDimensions())
                .racketFlexibility(request.getRacketFlexibility())
                .racketBalancePoint(request.getRacketBalancePoint())
                .racketShaftDiameter(request.getRacketShaftDiameter())
                .racketFrameShape(request.getRacketFrameShape())
                .racketRecommendedTension(request.getRacketRecommendedTension())
                .racketMaxTension(request.getRacketMaxTension())
                .racketMaterial(request.getRacketMaterial())
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.DRAFT)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .isNewArrival(request.getIsNewArrival() != null ? request.getIsNewArrival() : false)
                .isBestSeller(request.getIsBestSeller() != null ? request.getIsBestSeller() : false)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .metaKeywords(request.getMetaKeywords())
                .build();

        product = productRepository.save(product);

        // Add images if provided
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                String imageUrl = request.getImageUrls().get(i);
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(imageUrl)
                        .displayOrder(i)
                        .isPrimary(i == 0 || imageUrl.equals(request.getPrimaryImageUrl()))
                        .build();
                productImageRepository.save(image);
            }
        }

        // Create base inventory record for product (with quantity 0)
        // This allows tracking inventory even before variants are added
        Inventory baseInventory = Inventory.builder()
                .product(product)
                .variant(null) // Base product inventory (no variant)
                .quantityAvailable(0)
                .quantityReserved(0)
                .quantitySold(0)
                .lowStockThreshold(10)
                .reorderPoint(20)
                .warehouseLocation("main")
                .updatedAt(LocalDateTime.now())
                .build();
        inventoryRepository.save(baseInventory);

        log.info("Created product: {} ({}) with base inventory record", product.getName(), product.getSku());
        return ProductResponse.fromEntity(productRepository.findByIdWithImages(product.getProductId()).orElse(product));
    }

    /**
     * Update product
     */
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + id));

        // Update basic fields
        if (request.getName() != null)
            product.setName(request.getName());
        if (request.getSlug() != null && !request.getSlug().isEmpty()) {
            if (!product.getSlug().equals(request.getSlug()) && productRepository.existsBySlug(request.getSlug())) {
                throw new IllegalArgumentException("Slug đã tồn tại: " + request.getSlug());
            }
            product.setSlug(request.getSlug());
        }
        if (request.getSku() != null) {
            if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
                throw new IllegalArgumentException("SKU đã tồn tại: " + request.getSku());
            }
            product.setSku(request.getSku());
        }

        // Update category
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Không tìm thấy danh mục: " + request.getCategoryId()));
            product.setCategory(category);
        }

        // Update brand
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Không tìm thấy thương hiệu: " + request.getBrandId()));
            product.setBrand(brand);
        }

        // Update other fields
        if (request.getProductType() != null)
            product.setProductType(request.getProductType());
        if (request.getShortDescription() != null)
            product.setShortDescription(request.getShortDescription());
        if (request.getFullDescription() != null)
            product.setFullDescription(request.getFullDescription());
        if (request.getBasePrice() != null)
            product.setBasePrice(request.getBasePrice());
        if (request.getCompareAtPrice() != null)
            product.setCompareAtPrice(request.getCompareAtPrice());
        if (request.getCostPrice() != null)
            product.setCostPrice(request.getCostPrice());
        if (request.getWeight() != null)
            product.setWeight(request.getWeight());
        if (request.getDimensions() != null)
            product.setDimensions(request.getDimensions());
        if (request.getRacketFlexibility() != null)
            product.setRacketFlexibility(request.getRacketFlexibility());
        if (request.getRacketBalancePoint() != null)
            product.setRacketBalancePoint(request.getRacketBalancePoint());
        if (request.getRacketShaftDiameter() != null)
            product.setRacketShaftDiameter(request.getRacketShaftDiameter());
        if (request.getRacketFrameShape() != null)
            product.setRacketFrameShape(request.getRacketFrameShape());
        if (request.getRacketRecommendedTension() != null)
            product.setRacketRecommendedTension(request.getRacketRecommendedTension());
        if (request.getRacketMaxTension() != null)
            product.setRacketMaxTension(request.getRacketMaxTension());
        if (request.getRacketMaterial() != null)
            product.setRacketMaterial(request.getRacketMaterial());
        if (request.getStatus() != null) {
            ProductStatus newStatus = request.getStatus();
            ProductStatus oldStatus = product.getStatus();
            product.setStatus(newStatus);

            // Cascade status to variants when product is set to INACTIVE or OUT_OF_STOCK
            if (newStatus == ProductStatus.INACTIVE || newStatus == ProductStatus.OUT_OF_STOCK) {
                VariantStatus variantStatus = (newStatus == ProductStatus.INACTIVE)
                        ? VariantStatus.INACTIVE
                        : VariantStatus.INACTIVE; // OUT_OF_STOCK maps to INACTIVE for variants
                productVariantRepository.updateStatusByProductId(id, variantStatus);
                log.info("Cascaded status {} to all variants of product {}", variantStatus, id);
            }
        }
        if (request.getIsFeatured() != null)
            product.setIsFeatured(request.getIsFeatured());
        if (request.getIsNewArrival() != null)
            product.setIsNewArrival(request.getIsNewArrival());
        if (request.getIsBestSeller() != null)
            product.setIsBestSeller(request.getIsBestSeller());
        if (request.getDisplayOrder() != null)
            product.setDisplayOrder(request.getDisplayOrder());
        if (request.getMetaTitle() != null)
            product.setMetaTitle(request.getMetaTitle());
        if (request.getMetaDescription() != null)
            product.setMetaDescription(request.getMetaDescription());
        if (request.getMetaKeywords() != null)
            product.setMetaKeywords(request.getMetaKeywords());

        product = productRepository.save(product);
        log.info("Updated product: {} ({})", product.getName(), product.getSku());
        return ProductResponse.fromEntity(product);
    }

    /**
     * Delete product (soft delete)
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + id));

        product.setDeletedAt(LocalDateTime.now());
        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);
        log.info("Soft deleted product: {} ({})", product.getName(), product.getSku());
    }

    /**
     * Increment view count
     */
    @Transactional
    public void incrementViewCount(Long productId) {
        productRepository.incrementViewCount(productId);
    }

    /**
     * Get low stock products
     */
    public List<ProductListDTO> getLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold).stream()
                .map(this::mapToProductListDTO)
                .collect(Collectors.toList());
    }

    /**
     * Generate slug from name
     */
    private String generateSlug(String name) {
        if (name == null)
            return "";
        return name.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    /**
     * Get deleted products for trash - uses JdbcTemplate to bypass @Where filter
     */
    public Page<ProductListDTO> getDeletedProducts(Pageable pageable) {
        // Count total deleted products
        String countSql = "SELECT COUNT(*) FROM products WHERE deleted_at IS NOT NULL";
        Long totalCount = jdbcTemplate.queryForObject(countSql, Long.class);
        log.info("Deleted products count from database: {}", totalCount);
        if (totalCount == null || totalCount == 0) {
            log.info("No deleted products found in database");
            return Page.empty(pageable);
        }

        // Query deleted products with pagination
        String sql = """
                SELECT p.product_id, p.name, p.slug, p.sku, p.product_type, p.short_description,
                       p.base_price, p.compare_at_price, p.status, p.is_featured, p.is_new_arrival,
                       p.is_best_seller, p.sold_count, p.rating_average, p.rating_count,
                       c.category_id, c.name as category_name,
                       b.brand_id, b.name as brand_name, b.logo_url as brand_logo_url,
                       (SELECT pi.image_url FROM product_images pi WHERE pi.product_id = p.product_id
                        AND (pi.is_primary = 1 OR pi.is_primary = true) LIMIT 1) as primary_image_url
                FROM products p
                LEFT JOIN categories c ON p.category_id = c.category_id
                LEFT JOIN brands b ON p.brand_id = b.brand_id
                WHERE p.deleted_at IS NOT NULL
                ORDER BY p.deleted_at DESC
                LIMIT ? OFFSET ?
                """;

        List<ProductListDTO> products = jdbcTemplate.query(
                sql,
                new Object[] { pageable.getPageSize(), pageable.getOffset() },
                (rs, rowNum) -> mapRowToProductListDTO(rs));

        return new org.springframework.data.domain.PageImpl<>(products, pageable, totalCount);
    }

    /**
     * Map ResultSet row to ProductListDTO
     */
    private ProductListDTO mapRowToProductListDTO(ResultSet rs) throws SQLException {
        return ProductListDTO.builder()
                .productId(rs.getLong("product_id"))
                .name(rs.getString("name"))
                .slug(rs.getString("slug"))
                .sku(rs.getString("sku"))
                .productType(rs.getString("product_type") != null
                        ? com.badmintonshop.entity.enums.ProductType.valueOf(rs.getString("product_type"))
                        : null)
                .shortDescription(rs.getString("short_description"))
                .basePrice(rs.getBigDecimal("base_price"))
                .compareAtPrice(rs.getBigDecimal("compare_at_price"))
                .currentPrice(rs.getBigDecimal("base_price")) // Same as base price for deleted items
                .status(rs.getString("status") != null
                        ? com.badmintonshop.entity.enums.ProductStatus.valueOf(rs.getString("status"))
                        : null)
                .isFeatured(rs.getBoolean("is_featured"))
                .isNewArrival(rs.getBoolean("is_new_arrival"))
                .isBestSeller(rs.getBoolean("is_best_seller"))
                .soldCount(rs.getInt("sold_count"))
                .ratingAverage(rs.getBigDecimal("rating_average"))
                .ratingCount(rs.getInt("rating_count"))
                .categoryId(rs.getObject("category_id") != null ? rs.getLong("category_id") : null)
                .categoryName(rs.getString("category_name"))
                .brandId(rs.getObject("brand_id") != null ? rs.getLong("brand_id") : null)
                .brandName(rs.getString("brand_name"))
                .brandLogoUrl(rs.getString("brand_logo_url"))
                .primaryImageUrl(rs.getString("primary_image_url"))
                .build();
    }

    // ==================== PROMOTION PRICE MAPPING ====================

    /**
     * Map Product entity to ProductListDTO with promotion prices applied
     */
    private ProductListDTO mapToProductListDTO(Product product) {
        ProductListDTO dto = ProductListDTO.fromEntity(product);
        if (dto == null)
            return null;

        applyPromotionToProductListDTO(dto);
        return dto;
    }

    /**
     * Map Product entity to ProductResponse with promotion prices applied
     */
    private ProductResponse mapToProductResponse(Product product) {
        ProductResponse dto = ProductResponse.fromEntity(product);
        if (dto == null)
            return null;

        applyPromotionToProductResponse(dto);
        return dto;
    }

    /**
     * Apply promotion pricing to ProductListDTO
     */
    private void applyPromotionToProductListDTO(ProductListDTO dto) {
        Long productId = dto.getProductId();
        Long categoryId = dto.getCategoryId();
        BigDecimal basePrice = dto.getBasePrice();

        if (promotionPriceService.hasActivePromotion(productId, categoryId)) {
            dto.setHasActivePromotion(true);
            dto.setPromotionPrice(promotionPriceService.calculatePromotionPrice(basePrice, productId, categoryId));
            dto.setPromotionDiscount(
                    promotionPriceService.calculatePromotionDiscount(basePrice, productId, categoryId));

            PromotionPriceService.PromotionInfo info = promotionPriceService.getAppliedPromotionInfo(productId,
                    categoryId);
            if (info != null) {
                dto.setPromotionName(info.discountText());
            }
        } else {
            dto.setHasActivePromotion(false);
        }
    }

    /**
     * Apply promotion pricing to ProductResponse
     */
    private void applyPromotionToProductResponse(ProductResponse dto) {
        Long productId = dto.getProductId();
        Long categoryId = dto.getCategoryId();
        BigDecimal basePrice = dto.getBasePrice();

        boolean hasPromotion = promotionPriceService.hasActivePromotion(productId, categoryId);
        dto.setHasActivePromotion(hasPromotion);

        if (hasPromotion) {
            dto.setPromotionPrice(promotionPriceService.calculatePromotionPrice(basePrice, productId, categoryId));
            dto.setPromotionDiscount(
                    promotionPriceService.calculatePromotionDiscount(basePrice, productId, categoryId));

            PromotionPriceService.PromotionInfo info = promotionPriceService.getAppliedPromotionInfo(productId,
                    categoryId);
            if (info != null) {
                dto.setPromotionName(info.discountText());
            }
        }

        // Load stock quantity and apply promotion to each variant
        if (dto.getVariants() != null) {
            for (var variant : dto.getVariants()) {
                // Load stock quantity from inventory
                Integer stockQuantity = inventoryRepository.findByVariantVariantId(variant.getVariantId())
                        .map(inv -> inv.getActualAvailable())
                        .orElse(0);
                variant.setStockQuantity(stockQuantity);

                // Apply promotion if active
                if (hasPromotion) {
                    BigDecimal variantFinalPrice = variant.getFinalPrice();
                    if (variantFinalPrice != null) {
                        BigDecimal variantPromotionPrice = promotionPriceService.calculatePromotionPrice(
                                variantFinalPrice, productId, categoryId);
                        variant.setPromotionPrice(variantPromotionPrice);
                    }
                }
            }
        }
    }

    /**
     * Restore product from trash
     */
    @Transactional
    public void restoreProduct(Long id) {
        Product product = productRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + id));

        // Check if the product's category is soft-deleted
        // We use native query to bypass @Where filter
        Long categoryId = product.getCategory() != null ? product.getCategory().getCategoryId() : null;
        if (categoryId != null) {
            Optional<Category> categoryOpt = categoryRepository.findByIdIncludingDeleted(categoryId);
            if (categoryOpt.isPresent() && categoryOpt.get().getDeletedAt() != null) {
                throw new IllegalArgumentException(
                        String.format("Không thể khôi phục sản phẩm '%s' vì danh mục '%s' đã bị xóa. " +
                                "Vui lòng khôi phục danh mục trước.",
                                product.getName(), categoryOpt.get().getName()));
            }
        }

        // Check if the product's brand is soft-deleted
        Long brandId = product.getBrand() != null ? product.getBrand().getBrandId() : null;
        if (brandId != null) {
            Optional<Brand> brandOpt = brandRepository.findByIdIncludingDeleted(brandId);
            if (brandOpt.isPresent() && brandOpt.get().getDeletedAt() != null) {
                throw new IllegalArgumentException(
                        String.format("Không thể khôi phục sản phẩm '%s' vì thương hiệu '%s' đã bị xóa. " +
                                "Vui lòng khôi phục thương hiệu trước.",
                                product.getName(), brandOpt.get().getName()));
            }
        }

        product.setDeletedAt(null);
        product.setStatus(ProductStatus.DRAFT);
        productRepository.save(product);
        log.info("Restored product: {} ({})", product.getName(), product.getSku());
    }

    /**
     * Hard delete product (permanently)
     */
    @Transactional
    public void hardDeleteProduct(Long id) {
        Product product = productRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + id));

        // Delete related images first
        productImageRepository.deleteByProductId(product.getProductId());

        // Delete related inventory records
        inventoryRepository.deleteByProductProductId(product.getProductId());

        // Delete the product
        productRepository.delete(product);
        log.info("Hard deleted product: {} ({})", product.getName(), product.getSku());
    }

    public List<ProductListDTO> getAllActiveProductsBasicInfo() {
        return productRepository.findAllActive().stream()
                .map(p -> ProductListDTO.builder()
                        .productId(p.getProductId())
                        .name(p.getName())
                        .build())
                .collect(Collectors.toList());
    }
}
