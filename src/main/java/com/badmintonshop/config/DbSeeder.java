package com.badmintonshop.config;

import com.badmintonshop.entity.Inventory;
import com.badmintonshop.entity.Product;
import com.badmintonshop.entity.ProductVariant;
import com.badmintonshop.entity.User;
import com.badmintonshop.entity.enums.UserStatus;
import com.badmintonshop.repository.InventoryRepository;
import com.badmintonshop.repository.ProductRepository;
import com.badmintonshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DbSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    // Inject PasswordEncoder if available, or just use raw strings/dummy for now
    // since we bypass login
    // private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedMainUser();
        seedInventory();
    }

    private void seedMainUser() {
        if (!userRepository.existsById(1L)) {
            User user = new User();
            user.setUserId(1L); // Force ID 1 if DB supports or relies on sequence reset (JPA might ignore this
                                // if Identity)
            // Ideally we check by email "admin@example.com"
            if (userRepository.findByEmailAndDeletedAtIsNull("admin@example.com").isEmpty()) {
                user.setEmail("admin@example.com");
                user.setFullName("System Admin");
                user.setPasswordHash("$2a$10$DUMMY"); // Dummy hash
                user.setPhone("0900000000");
                user.setStatus(UserStatus.ACTIVE);
                userRepository.save(user);
                System.out.println(">>> SEEDED USER 1 (admin@example.com)");
            }
        }
    }

    private void seedInventory() {
        System.out.println(">>> CHECKING INVENTORY SEEDING...");
        List<Product> products = productRepository.findAllWithVariants();

        for (Product p : products) {
            // 1. Stock for Base Product (if no variants, or hybrid)
            if (p.getVariants().isEmpty()) {
                ensureStock(p, null);
            } else {
                // 2. Stock for Variants
                for (ProductVariant v : p.getVariants()) {
                    ensureStock(p, v);
                }
            }
        }
    }

    private void ensureStock(Product p, ProductVariant v) {
        // Simple check: Is there ANY (main) inventory record?
        // Note: Real logic might check by warehouse. Here default "main".
        boolean exists;
        if (v == null) {
            exists = inventoryRepository.findByProduct_ProductIdAndVariantIsNull(p.getProductId()).isPresent();
        } else {
            exists = inventoryRepository.findByProduct_ProductIdAndVariant_VariantId(p.getProductId(), v.getVariantId())
                    .isPresent();
        }

        if (!exists) {
            Inventory inv = new Inventory();
            inv.setProduct(p);
            inv.setVariant(v);
            inv.setQuantityAvailable(100);
            inv.setQuantitySold(0);
            inv.setWarehouseLocation("main");
            inventoryRepository.save(inv);
            System.out.println(">>> SEEDED STOCK FOR: " + p.getName() + (v != null ? " - " + v.getVariantName() : ""));
        }
    }
}
