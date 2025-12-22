-- Seed data for testing admin login
-- Password: admin123 (BCrypt hash with strength 12)

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

-- Additional staff for testing
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

-- Notes:
-- Password for both accounts: admin123
-- To generate new BCrypt hash, use online tool or run:
-- new BCryptPasswordEncoder(12).encode("your_password")
