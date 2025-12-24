package com.badmintonshop.service;

import com.badmintonshop.entity.*;
import com.badmintonshop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service để generate kiến thức động cho chatbot từ database
 * Kiến thức sẽ được refresh mỗi khi có request (với caching)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGeneratorService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final StringProductRepository stringProductRepository;
    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;

    // Cache
    private String cachedKnowledge;
    private LocalDateTime lastUpdated;
    private static final int CACHE_MINUTES = 5;

    /**
     * Lấy knowledge, tự động refresh nếu cache hết hạn
     */
    public String getKnowledge() {
        if (shouldRefresh()) {
            refreshKnowledge();
        }
        return cachedKnowledge;
    }

    /**
     * Force refresh knowledge
     */
    public void refreshKnowledge() {
        log.info("Refreshing chatbot knowledge from database...");
        long startTime = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("# Thông tin cửa hàng Badminton Shop\n\n");
        sb.append("**Cập nhật lúc:** ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")))
                .append("\n\n");

        // Thông tin cơ bản
        sb.append(getBasicInfo());

        // Sản phẩm nổi bật
        sb.append(getProductsSummary());

        // Thương hiệu
        sb.append(getBrandsSummary());

        // Danh mục
        sb.append(getCategoriesSummary());

        // Cước vợt
        sb.append(getStringsSummary());

        // Khuyến mãi
        sb.append(getPromotionsSummary());

        // Mã giảm giá
        sb.append(getCouponsSummary());

        // Chính sách
        sb.append(getPolicies());

        cachedKnowledge = sb.toString();
        lastUpdated = LocalDateTime.now();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Chatbot knowledge refreshed in {}ms", duration);
    }

    private boolean shouldRefresh() {
        return cachedKnowledge == null ||
                lastUpdated == null ||
                lastUpdated.plusMinutes(CACHE_MINUTES).isBefore(LocalDateTime.now());
    }

    private String getBasicInfo() {
        return """
                ## Thông tin liên hệ
                - Hotline: 1900 1234
                - Email: support@badmintonshop.com
                - Thời gian làm việc: 8:00 - 22:00 hàng ngày

                """;
    }

    private String getProductsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Sản phẩm nổi bật\n\n");

        try {
            // Top 10 sản phẩm active
            List<Product> products = productRepository.findAll(PageRequest.of(0, 10)).getContent();

            if (products.isEmpty()) {
                sb.append("Đang cập nhật sản phẩm...\n\n");
            } else {
                for (Product p : products) {
                    sb.append("- **").append(p.getName()).append("**");
                    if (p.getBrand() != null) {
                        sb.append(" (").append(p.getBrand().getName()).append(")");
                    }
                    if (p.getBasePrice() != null) {
                        sb.append(" - Giá từ ").append(formatPrice(p.getBasePrice().longValue())).append("đ");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            log.error("Error getting products summary: {}", e.getMessage());
            sb.append("Đang cập nhật sản phẩm...\n\n");
        }

        return sb.toString();
    }

    private String getBrandsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Thương hiệu có sẵn\n\n");

        try {
            List<Brand> brands = brandRepository.findAllActive();
            if (!brands.isEmpty()) {
                String brandNames = brands.stream()
                        .map(Brand::getName)
                        .collect(Collectors.joining(", "));
                sb.append(brandNames).append("\n\n");
            } else {
                sb.append("Đang cập nhật thương hiệu...\n\n");
            }
        } catch (Exception e) {
            log.error("Error getting brands summary: {}", e.getMessage());
        }

        return sb.toString();
    }

    private String getCategoriesSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Danh mục sản phẩm\n\n");

        try {
            List<Category> categories = categoryRepository.findAllActive();
            if (!categories.isEmpty()) {
                for (Category c : categories) {
                    sb.append("- ").append(c.getName()).append("\n");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            log.error("Error getting categories summary: {}", e.getMessage());
        }

        return sb.toString();
    }

    private String getStringsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Dịch vụ đan vợt & Cước vợt\n\n");

        try {
            List<StringProduct> strings = stringProductRepository.findAllActive();
            if (!strings.isEmpty()) {
                sb.append("Các loại cước có sẵn:\n");
                for (StringProduct s : strings) {
                    sb.append("- **").append(s.getName()).append("**");
                    if (s.getBrand() != null) {
                        sb.append(" (").append(s.getBrand().getName()).append(")");
                    }
                    if (s.getRetailPrice() != null) {
                        sb.append(" - ").append(formatPrice(s.getRetailPrice().longValue())).append("đ");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            } else {
                sb.append("Dịch vụ đan vợt chuyên nghiệp, giá từ 50.000đ\n\n");
            }
        } catch (Exception e) {
            log.error("Error getting strings summary: {}", e.getMessage());
            sb.append("Dịch vụ đan vợt chuyên nghiệp, giá từ 50.000đ\n\n");
        }

        return sb.toString();
    }

    private String getPromotionsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Khuyến mãi đang diễn ra\n\n");

        try {
            List<Promotion> promotions = promotionRepository.findByIsActiveTrue();
            LocalDateTime now = LocalDateTime.now();

            List<Promotion> activePromotions = promotions.stream()
                    .filter(p -> p.getStartsAt().isBefore(now) && p.getEndsAt().isAfter(now))
                    .toList();

            if (!activePromotions.isEmpty()) {
                for (Promotion p : activePromotions) {
                    sb.append("- **").append(p.getName()).append("**");
                    if (p.getDiscountValue() != null && p.getDiscountValue().doubleValue() > 0) {
                        if (p.getDiscountType() != null && p.getDiscountType().name().contains("PERCENT")) {
                            sb.append(" - Giảm ").append(p.getDiscountValue().intValue()).append("%");
                        } else {
                            sb.append(" - Giảm ").append(formatPrice(p.getDiscountValue().longValue())).append("đ");
                        }
                    }
                    sb.append(" (đến ").append(p.getEndsAt().format(DateTimeFormatter.ofPattern("dd/MM"))).append(")");
                    sb.append("\n");
                }
                sb.append("\n");
            } else {
                sb.append("Liên hệ hotline để biết khuyến mãi mới nhất.\n\n");
            }
        } catch (Exception e) {
            log.error("Error getting promotions summary: {}", e.getMessage());
            sb.append("Liên hệ hotline để biết khuyến mãi mới nhất.\n\n");
        }

        return sb.toString();
    }

    private String getCouponsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Mã giảm giá\n\n");

        try {
            List<Coupon> coupons = couponRepository.findByIsActiveTrue();
            LocalDateTime now = LocalDateTime.now();

            List<Coupon> validCoupons = coupons.stream()
                    .filter(c -> c.getStartsAt().isBefore(now) && c.getExpiresAt().isAfter(now))
                    .filter(c -> c.getUsageLimit() == null || c.getTimesUsed() < c.getUsageLimit())
                    .limit(3)
                    .toList();

            if (!validCoupons.isEmpty()) {
                for (Coupon c : validCoupons) {
                    sb.append("- Mã **").append(c.getCode()).append("**");
                    if (c.getType() != null && c.getValue() != null && c.getValue().doubleValue() > 0) {
                        if (c.getType().name().contains("PERCENT")) {
                            sb.append(" - Giảm ").append(c.getValue().intValue()).append("%");
                        } else {
                            sb.append(" - Giảm ").append(formatPrice(c.getValue().longValue())).append("đ");
                        }
                    }
                    if (c.getMinOrderValue() != null && c.getMinOrderValue().doubleValue() > 0) {
                        sb.append(" (đơn từ ").append(formatPrice(c.getMinOrderValue().longValue())).append("đ)");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            } else {
                sb.append("Đăng ký thành viên để nhận mã giảm giá độc quyền!\n\n");
            }
        } catch (Exception e) {
            log.error("Error getting coupons summary: {}", e.getMessage());
            sb.append("Đăng ký thành viên để nhận mã giảm giá độc quyền!\n\n");
        }

        return sb.toString();
    }

    private String getPolicies() {
        return """
                ## Chính sách

                ### Giao hàng
                - Nội thành TP.HCM/Hà Nội: 1-2 ngày
                - Tỉnh khác: 2-5 ngày
                - Miễn phí ship đơn từ 500.000đ

                ### Đổi trả
                - Đổi trả miễn phí trong 7 ngày nếu lỗi nhà sản xuất
                - Sản phẩm còn nguyên tem, chưa qua sử dụng

                ### Bảo hành
                - Vợt cầu lông: 6-12 tháng
                - Giày: 3 tháng
                - Túi vợt: 6 tháng
                """;
    }

    private String formatPrice(long price) {
        return String.format("%,d", price).replace(",", ".");
    }

    /**
     * Lấy thời gian cập nhật cuối
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
}
