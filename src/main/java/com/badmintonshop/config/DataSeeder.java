package com.badmintonshop.config;

import com.badmintonshop.entity.Brand;
import com.badmintonshop.entity.Staff;
import com.badmintonshop.entity.StringProduct;
import com.badmintonshop.entity.StringingService;
import com.badmintonshop.entity.enums.StaffRole;
import com.badmintonshop.entity.enums.StaffStatus;
import com.badmintonshop.entity.enums.StringType;
import com.badmintonshop.entity.enums.StringingServiceType;
import com.badmintonshop.repository.BrandRepository;
import com.badmintonshop.repository.StaffRepository;
import com.badmintonshop.repository.StringProductRepository;
import com.badmintonshop.repository.StringingServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Data seeder to create default super admin on application startup
 * Credentials are read from application-secrets.properties
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringingServiceRepository stringingServiceRepository;
    private final StringProductRepository stringProductRepository;
    private final BrandRepository brandRepository;

    // Super admin credentials from properties
    @Value("${app.admin.email}")
    private String superAdminEmail;

    @Value("${app.admin.password}")
    private String superAdminPassword;

    @Value("${app.admin.name}")
    private String superAdminName;

    @Value("${app.admin.phone}")
    private String superAdminPhone;

    @Override
    public void run(String... args) {
        createSuperAdminIfNotExists();
        createStringingServicesIfNotExists();
        createStringProductsIfNotExists();
    }

    /**
     * Create super admin if not exists, or reset password if exists
     */
    private void createSuperAdminIfNotExists() {
        // Check if this specific email already exists
        var existingStaff = staffRepository.findByEmail(superAdminEmail);

        if (existingStaff.isEmpty()) {
            log.info("Super admin email not found. Creating default super admin...");

            Staff superAdmin = Staff.builder()
                    .email(superAdminEmail)
                    .passwordHash(passwordEncoder.encode(superAdminPassword))
                    .fullName(superAdminName)
                    .phone(superAdminPhone)
                    .role(StaffRole.SUPER_ADMIN)
                    .status(StaffStatus.ACTIVE)
                    .build();

            staffRepository.save(superAdmin);

            log.info("==============================================");
            log.info("DEFAULT SUPER ADMIN CREATED SUCCESSFULLY!");
            log.info("Email: {}", superAdminEmail);
            log.info("Password: [HIDDEN]");
            log.info("Please change the password after first login!");
            log.info("==============================================");
        } else {
            // Reset password and ensure account is active
            Staff staff = existingStaff.get();
            staff.setPasswordHash(passwordEncoder.encode(superAdminPassword));
            staff.setStatus(StaffStatus.ACTIVE);
            staff.setRole(StaffRole.SUPER_ADMIN);
            staffRepository.save(staff);

            log.info("==============================================");
            log.info("SUPER ADMIN PASSWORD RESET!");
            log.info("Email: {}", superAdminEmail);
            log.info("Password: [HIDDEN]");
            log.info("==============================================");
        }
    }

    /**
     * Create stringing services if not exists
     */
    private void createStringingServicesIfNotExists() {
        if (stringingServiceRepository.count() > 0) {
            log.info("Stringing services already exist, skipping seed.");
            return;
        }

        log.info("Creating stringing services...");

        StringingService standard = StringingService.builder()
                .serviceName("Đan vợt thường")
                .serviceType(StringingServiceType.STANDARD)
                .description("Dịch vụ đan vợt tiêu chuẩn với 2 nút")
                .basePrice(new BigDecimal("50000"))
                .estimatedTimeMinutes(30)
                .isActive(true)
                .build();
        stringingServiceRepository.save(standard);

        StringingService fourKnot = StringingService.builder()
                .serviceName("Đan vợt 4 nút")
                .serviceType(StringingServiceType.FOUR_KNOT)
                .description("Dịch vụ đan vợt cao cấp 4 nút, giữ lực căng tốt hơn")
                .basePrice(new BigDecimal("80000"))
                .estimatedTimeMinutes(45)
                .isActive(true)
                .build();
        stringingServiceRepository.save(fourKnot);

        StringingService twoKnot = StringingService.builder()
                .serviceName("Đan vợt 2 nút")
                .serviceType(StringingServiceType.TWO_KNOT)
                .description("Dịch vụ đan vợt 2 nút cơ bản")
                .basePrice(new BigDecimal("60000"))
                .estimatedTimeMinutes(35)
                .isActive(true)
                .build();
        stringingServiceRepository.save(twoKnot);

        log.info("Created 3 stringing services successfully!");
    }

    /**
     * Create string products if not exists
     */
    private void createStringProductsIfNotExists() {
        if (stringProductRepository.count() > 0) {
            log.info("String products already exist, skipping seed.");
            return;
        }

        // Get or create a brand for strings
        Brand yonex = brandRepository.findByName("Yonex")
                .orElseGet(() -> {
                    Brand newBrand = Brand.builder()
                            .name("Yonex")
                            .slug("yonex")
                            .description("Thương hiệu cầu lông số 1 thế giới")
                            .isActive(true)
                            .build();
                    return brandRepository.save(newBrand);
                });

        log.info("Creating string products...");

        StringProduct bg65 = StringProduct.builder()
                .brand(yonex)
                .name("Yonex BG65")
                .sku("STR-BG65-001")
                .description("Cước căng bền, độ nảy trung bình, phù hợp người chơi ở mọi cấp độ")
                .stringType(StringType.SYNTHETIC)
                .gauge(new BigDecimal("0.70"))
                .material("High-Polymer Nylon")
                .durabilityRating(9)
                .repulsionRating(6)
                .controlRating(7)
                .hittingSoundRating(6)
                .recommendedTensionMin(new BigDecimal("20"))
                .recommendedTensionMax(new BigDecimal("28"))
                .retailPrice(new BigDecimal("150000"))
                .quantityInStock(100)
                .color("Trắng")
                .isActive(true)
                .build();
        stringProductRepository.save(bg65);

        StringProduct bg80 = StringProduct.builder()
                .brand(yonex)
                .name("Yonex BG80")
                .sku("STR-BG80-001")
                .description("Cước cao cấp, độ nảy cao, thích hợp cho lối đánh công")
                .stringType(StringType.SYNTHETIC)
                .gauge(new BigDecimal("0.68"))
                .material("VECTRAN")
                .durabilityRating(7)
                .repulsionRating(9)
                .controlRating(7)
                .hittingSoundRating(8)
                .recommendedTensionMin(new BigDecimal("22"))
                .recommendedTensionMax(new BigDecimal("30"))
                .retailPrice(new BigDecimal("200000"))
                .quantityInStock(50)
                .color("Vàng")
                .isActive(true)
                .build();
        stringProductRepository.save(bg80);

        StringProduct nanogy99 = StringProduct.builder()
                .brand(yonex)
                .name("Yonex Nanogy 99")
                .sku("STR-NBG99-001")
                .description("Cước năng động, cân bằng giữa lực và kiểm soát")
                .stringType(StringType.NATURAL)
                .gauge(new BigDecimal("0.69"))
                .material("Multi Nylon + Carbon Nanotube")
                .durabilityRating(8)
                .repulsionRating(8)
                .controlRating(8)
                .hittingSoundRating(7)
                .recommendedTensionMin(new BigDecimal("21"))
                .recommendedTensionMax(new BigDecimal("29"))
                .retailPrice(new BigDecimal("180000"))
                .quantityInStock(75)
                .color("Trắng")
                .isActive(true)
                .build();
        stringProductRepository.save(nanogy99);

        log.info("Created 3 string products successfully!");
    }
}

