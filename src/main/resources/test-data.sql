-- Insert Dummy Category
INSERT INTO categories (category_id, name, slug, description, category_type, status, created_at)
VALUES (1, 'Rackets', 'rackets', 'Badminton Rackets', 'RACKET', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE name = name;

-- Insert Dummy User (ID 1)
INSERT INTO users (user_id, email, password_hash, full_name, phone, status, created_at)
VALUES (1, 'test@user.com', 'hash', 'Test User', '0123456789', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE full_name = full_name;

-- Insert Dummy Product
INSERT INTO products (product_id, category_id, name, slug, sku, full_description, short_description, base_price, product_type, status, created_at)
VALUES (1, 1, 'Yonex Astrox 100ZZ', 'yonex-astrox-100zz', 'AX100ZZ', 'Best racket', 'Short desc', 3500000, 'RACKET', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE name = name;

-- Insert Dummy Product Variant
INSERT INTO product_variants (variant_id, product_id, attributes, sku, price_adjustment, status)
VALUES (1, 1, '{"color": "Navy/Orange", "size": "4U/G5"}', 'AX100ZZ-NO-4UG5', 0, 'ACTIVE')
ON DUPLICATE KEY UPDATE sku = sku;

-- Inventory Seeding for Dummy Product
INSERT INTO inventory (product_id, variant_id, quantity_available, updated_at)
SELECT 1, 1, 100, NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM inventory WHERE product_id = 1 AND variant_id = 1);

-- Insert Admin Staff (Password: admin123)
INSERT INTO staff (email, password_hash, full_name, phone, role, status, created_at, updated_at)
VALUES (
    'admin@shop.vn',
    '$2a$12$NPAuKW3OFeGs8V12tpgtSuaywdUtULb44aZf5k1kDXcNzWc1KtZD6',
    'Administrator',
    '0909000000',
    'SUPER_ADMIN',
    'ACTIVE',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), role = VALUES(role);

-- Insert Staff Member (Password: admin123)
INSERT INTO staff (email, password_hash, full_name, phone, role, status, created_at, updated_at)
VALUES (
    'staff@shop.vn',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X3a2c0h3K8e.1pJHu',
    'Staff Member',
    '0909111111',
    'SALE_STAFF',
    'ACTIVE',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE email = email;

-- ==============================================================================
-- AUTO-FIX: seed inventory for manual products (missing inventory)
-- This ensures 'Product out of stock' error does not occur for products without inventory
-- ==============================================================================

-- 1. Seed inventory for products without variants (or fallback)
INSERT INTO inventory (product_id, variant_id, quantity_available, quantity_reserved, quantity_sold, updated_at)
SELECT p.product_id, NULL, 50, 0, 0, NOW()
FROM products p
LEFT JOIN inventory i ON p.product_id = i.product_id AND i.variant_id IS NULL
WHERE i.inventory_id IS NULL;

-- 2. Seed inventory for all variants
INSERT INTO inventory (product_id, variant_id, quantity_available, quantity_reserved, quantity_sold, updated_at)
SELECT pv.product_id, pv.variant_id, 50, 0, 0, NOW()
FROM product_variants pv
LEFT JOIN inventory i ON pv.variant_id = i.variant_id
WHERE i.inventory_id IS NULL;

-- 3. FORCE UPDATE: Ensure everything has at least 50 stock (fixes existing 0-stock records)
UPDATE inventory SET quantity_available = 50 WHERE quantity_available < 10;
