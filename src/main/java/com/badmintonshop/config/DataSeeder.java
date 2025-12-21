package com.badmintonshop.config;

import com.badmintonshop.entity.*;
import com.badmintonshop.entity.enums.ProductStatus;
import com.badmintonshop.entity.enums.ProductType;
import com.badmintonshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedUsers();
        seedProducts();
    }

    private void seedUsers() {
        String email = "test@user.com";
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);

        if (user == null) {
            user = User.builder()
                    .email(email)
                    .passwordHash(new BCryptPasswordEncoder(12).encode("password"))
                    .fullName("Test User")
                    .phone("0123456789")
                    .isEmailVerified(true)
                    .status(com.badmintonshop.entity.enums.UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
            System.out.println(">>> User seeded: " + email);
        } else {
            // Ensure existing user is verified
            boolean changed = false;
            if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
                user.setIsEmailVerified(true);
                changed = true;
            }
            if (user.getStatus() != com.badmintonshop.entity.enums.UserStatus.ACTIVE) {
                user.setStatus(com.badmintonshop.entity.enums.UserStatus.ACTIVE);
                changed = true;
            }

            if (changed) {
                userRepository.save(user);
                System.out.println(">>> User updated to verified/active: " + email);
            }
        }
    }

    private void seedProducts() {
        if (productRepository.count() == 0) {
            // Category
            Category category = Category.builder()
                    .name("Rackets")
                    .slug("rackets")
                    .description("Badminton Rackets")
                    .build();
            categoryRepository.save(category);

            // Product
            Product product = Product.builder()
                    .category(category)
                    .name("Yonex Astrox 100ZZ")
                    .slug("yonex-astrox-100zz")
                    .sku("AX100ZZ")
                    .shortDescription("Power racket")
                    .fullDescription("The best racket for smashes")
                    .basePrice(new BigDecimal("3500000"))
                    .productType(ProductType.RACKET)
                    .status(ProductStatus.ACTIVE)
                    .hasVariants(true)
                    .build();
            product = productRepository.save(product);

            // Variant
            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .sku("AX100ZZ-NO-4UG5")
                    // .attributes("{\"color\": \"Navy/Orange\", \"size\": \"4U/G5\"}") // Should be
                    // valid JSON
                    .variantName("Navy/Orange - 4U/G5")
                    .priceAdjustment(BigDecimal.ZERO)
                    .build();
            productVariantRepository.save(variant);

            System.out.println(">>> Product seeded");
        }
    }
}
