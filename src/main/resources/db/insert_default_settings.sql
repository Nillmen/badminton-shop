-- =====================================================
-- Insert Default System Settings
-- Run this SQL to add required settings for the application
-- =====================================================

-- Store information settings
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'site_name', 'Badminton Shop', 'STRING', 'Tên website', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'site_name');

INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'site_description', 'Cửa hàng chuyên cung cấp vợt, giày, cước và phụ kiện cầu lông chính hãng. Dịch vụ đan vợt chuyên nghiệp.', 'STRING', 'Mô tả website', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'site_description');

INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'site_logo', '/images/logo.png', 'STRING', 'URL logo website', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'site_logo');

-- Shipping settings
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'free_shipping_threshold', '500000', 'NUMBER', 'Đơn hàng từ giá trị này trở lên được miễn phí vận chuyển (VND)', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'free_shipping_threshold');

INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'default_shipping_fee', '30000', 'NUMBER', 'Phí vận chuyển mặc định (VND)', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'default_shipping_fee');

-- Contact settings
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'contact_phone', '0909123456', 'STRING', 'Số điện thoại liên hệ', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'contact_phone');

INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'contact_email', 'contact@badmintonshop.vn', 'STRING', 'Email liên hệ', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'contact_email');

INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'contact_address', '123 Đường ABC, Quận 1, TP.HCM', 'STRING', 'Địa chỉ cửa hàng', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'contact_address');

INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'business_hours', '8:00 - 21:00 (T2 - CN)', 'STRING', 'Giờ làm việc', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'business_hours');

-- Social media links
INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'facebook_url', 'https://facebook.com/badmintonshop', 'STRING', 'Link Facebook', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'facebook_url');

INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'instagram_url', 'https://instagram.com/badmintonshop', 'STRING', 'Link Instagram', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'instagram_url');

INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'youtube_url', '#', 'STRING', 'Link Youtube', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'youtube_url');

INSERT INTO system_settings (setting_key, setting_value, setting_type, description, is_public, updated_at)
SELECT 'tiktok_url', '#', 'STRING', 'Link TikTok', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'tiktok_url');

-- Delete old unused settings (VAT, stringing time, max cart items)
DELETE FROM system_settings WHERE setting_key IN ('tax_rate', 'vat_rate', 'default_stringing_time', 'max_cart_items');

-- Confirm all settings
SELECT setting_key, setting_value, description FROM system_settings ORDER BY setting_key;
