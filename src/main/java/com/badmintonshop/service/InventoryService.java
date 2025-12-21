package com.badmintonshop.service;

import com.badmintonshop.entity.Inventory;
import com.badmintonshop.entity.OrderItem;
import com.badmintonshop.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * Trừ tồn kho atomic (được gọi khi tạo Order)
     */
    @Transactional
    public void sellStock(Long productId, Long variantId, Integer quantity) {
        // Sử dụng atomicDecrease để đảm bảo tính toàn vẹn dữ liệu
        int updatedRows = inventoryRepository.atomicDecrease(productId, variantId, quantity);

        if (updatedRows == 0) {
            // Nếu không update được row nào => Hết hàng hoặc tồn kho không đủ
            // Lấy thông tin tồn kho hiện tại để báo lỗi chi tiết hơn
            Optional<Inventory> inventoryOpt;
            if (variantId != null) {
                inventoryOpt = inventoryRepository.findByProduct_ProductIdAndVariant_VariantId(productId, variantId);
            } else {
                inventoryOpt = inventoryRepository.findByProduct_ProductIdAndVariantIsNull(productId);
            }

            int currentStock = inventoryOpt.map(Inventory::getQuantityAvailable).orElse(0);
            throw new RuntimeException("Tồn kho không đủ cho sản phẩm ID " + productId +
                    (variantId != null ? " (Variant: " + variantId + ")" : "") +
                    ". Chỉ còn: " + currentStock);
        }

        // Log transaction nếu cần thiết (TODO: Implement InventoryTransaction)
    }

    /**
     * Hoàn lại tồn kho cho từng Item trong danh sách (được gọi từ
     * PaymentService/OrderService khi HỦY Order)
     */
    @Transactional
    public void restoreStock(List<OrderItem> items) {
        System.out.println("--- [INVENTORY] Bắt đầu hoàn lại tồn kho cho đơn hàng ---");
        for (OrderItem item : items) {

            // SỬA LỖI: Lấy ID từ Entity Object
            Long productId = item.getProduct().getProductId();
            Long variantId = item.getVariant() != null ? item.getVariant().getVariantId() : null;

            Integer quantity = item.getQuantity();

            // Gọi hàm restockStock chi tiết đã có
            this.restockStock(productId, variantId, quantity);

            System.out.println("Hoàn lại " + quantity + " sản phẩm ID: " + productId
                    + (variantId != null ? " (Variant ID: " + variantId + ")" : ""));
        }
        System.out.println("--- [INVENTORY] Hoàn tất ---");
    }

    /**
     * Hoàn lại tồn kho chi tiết (được gọi khi HỦY Order)
     */
    @Transactional
    public void restockStock(Long productId, Long variantId, Integer quantity) {
        int updatedRows = inventoryRepository.atomicIncrease(productId, variantId, quantity);

        if (updatedRows == 0) {
            // Trường hợp này hiếm khi xảy ra trừ khi Sản phẩm bị xóa cứng khi đang vận hành
            throw new RuntimeException("Không tìm thấy bản ghi tồn kho để hoàn lại cho sản phẩm ID: " + productId);
        }
    }
}