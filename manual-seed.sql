-- Chạy script này trong MySQL (phpMyAdmin hoặc Workbench) để có dữ liệu Test
-- 1. Tạo Category
INSERT INTO categories (category_id, name, slug, description, type, status, created_at)
VALUES (1, 'Rackets', 'rackets', 'Badminton Rackets', 'PRODUCT', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE name = name;

-- 2. Tạo User (test@user.com)
INSERT INTO users (user_id, email, password_hash, full_name, phone, status, created_at)
VALUES (1, 'test@user.com', 'hash', 'Test User', '0123456789', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE full_name = full_name;

-- 3. Tạo Product (Yonex Astrox 100ZZ)
INSERT INTO products (product_id, category_id, name, slug, sku, full_description, short_description, base_price, product_type, status, created_at)
VALUES (1, 1, 'Yonex Astrox 100ZZ', 'yonex-astrox-100zz', 'AX100ZZ', 'Best racket', 'Short desc', 3500000, 'RACKET', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE name = name;

-- 4. Tạo Variant (4U/G5)
INSERT INTO product_variants (variant_id, product_id, attributes, sku, price_adjustment, status)
VALUES (1, 1, '{"color": "Navy/Orange", "size": "4U/G5"}', 'AX100ZZ-NO-4UG5', 0, 'ACTIVE')
ON DUPLICATE KEY UPDATE sku = sku;
