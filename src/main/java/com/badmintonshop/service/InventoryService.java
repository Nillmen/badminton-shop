package com.badmintonshop.service;

import com.badmintonshop.dto.inventory.InventoryAdjustRequest;
import com.badmintonshop.dto.inventory.InventoryDTO;
import com.badmintonshop.dto.inventory.InventoryTransactionDTO;
import com.badmintonshop.entity.Inventory;
import com.badmintonshop.entity.InventoryTransaction;
import com.badmintonshop.entity.ProductVariant;
import com.badmintonshop.entity.enums.TransactionType;
import com.badmintonshop.entity.enums.VariantStatus;
import com.badmintonshop.repository.InventoryRepository;
import com.badmintonshop.repository.InventoryTransactionRepository;
import com.badmintonshop.repository.ProductRepository;
import com.badmintonshop.repository.ProductVariantRepository;
import com.badmintonshop.security.Auditable;
import com.badmintonshop.entity.enums.ActivityAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Inventory operations with atomic operations and race condition
 * handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InventoryService {

        private final InventoryRepository inventoryRepository;
        private final InventoryTransactionRepository transactionRepository;
        private final ProductRepository productRepository;
        private final ProductVariantRepository productVariantRepository;

        /**
         * Get all inventory with pagination
         */
        public Page<InventoryDTO> getAllInventory(Pageable pageable) {
                return inventoryRepository.findAll(pageable)
                                .map(InventoryDTO::fromEntity);
        }

        /**
         * Search inventory with filters
         */
        public Page<InventoryDTO> searchInventory(Long productId, Boolean lowStock, Pageable pageable) {
                return inventoryRepository.searchInventory(productId, lowStock, pageable)
                                .map(InventoryDTO::fromEntity);
        }

        /**
         * Search inventory with keyword and status filter
         * stockStatus: LOW_STOCK, OUT_OF_STOCK, IN_STOCK
         */
        public Page<InventoryDTO> searchInventoryWithKeyword(String keyword, String stockStatus, Pageable pageable) {
                return inventoryRepository.searchInventoryWithKeyword(keyword, stockStatus, pageable)
                                .map(InventoryDTO::fromEntity);
        }

        /**
         * Get inventory by product ID
         */
        public List<InventoryDTO> getInventoryByProductId(Long productId) {
                return inventoryRepository.findByProductProductId(productId).stream()
                                .map(InventoryDTO::fromEntity)
                                .collect(Collectors.toList());
        }

        /**
         * Get low stock inventory
         */
        public List<InventoryDTO> getLowStockInventory() {
                return inventoryRepository.findLowStock().stream()
                                .map(InventoryDTO::fromEntity)
                                .collect(Collectors.toList());
        }

        /**
         * Get low stock inventory with pagination
         */
        public Page<InventoryDTO> getLowStockInventory(Pageable pageable) {
                return inventoryRepository.findLowStock(pageable)
                                .map(InventoryDTO::fromEntity);
        }

        /**
         * Update stock quantity (set absolute value)
         */
        @Transactional
        @Auditable(entityType = "Inventory", action = ActivityAction.UPDATE, description = "Updated inventory ID: {0}")
        public InventoryDTO updateStock(Long inventoryId, int newQuantity, String reason) {
                Inventory inventory = inventoryRepository.findById(inventoryId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy tồn kho: " + inventoryId));

                int previousQuantity = inventory.getQuantityAvailable();
                int adjustment = newQuantity - previousQuantity;

                inventory.setQuantityAvailable(newQuantity);
                inventory.setUpdatedAt(LocalDateTime.now());
                inventoryRepository.saveAndFlush(inventory);
                // Refresh with variant eagerly loaded for status update
                inventory = inventoryRepository.findByIdWithVariant(inventoryId).get();

                // Record transaction - use IN for increase, OUT for decrease
                createTransaction(inventory, previousQuantity, newQuantity, Math.abs(adjustment),
                                adjustment > 0 ? TransactionType.IN : TransactionType.OUT,
                                reason, "adjustment", null);

                log.info("Updated inventory {}: {} -> {}", inventoryId, previousQuantity, newQuantity);

                // Check and update product and variant status if needed
                checkAndUpdateProductStatus(inventory.getProduct().getProductId());
                checkAndUpdateVariantStatus(inventory);

                return InventoryDTO.fromEntity(inventory);
        }

        /**
         * Adjust stock quantity (atomic operation with race condition handling)
         */
        @Transactional
        @Auditable(entityType = "Inventory", action = ActivityAction.UPDATE, description = "Adjusted inventory ID: {0}")
        public InventoryDTO adjustStock(InventoryAdjustRequest request) {
                Long inventoryId = request.getInventoryId();
                int adjustment = request.getAdjustment();

                if (inventoryId == null) {
                        throw new IllegalArgumentException("ID tồn kho không được để trống");
                }

                Inventory inventory = inventoryRepository.findById(inventoryId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy tồn kho: " + inventoryId));

                int previousQuantity = inventory.getQuantityAvailable();

                // Use atomic update to handle race conditions
                int rowsAffected = inventoryRepository.adjustQuantity(inventoryId, adjustment);

                if (rowsAffected == 0) {
                        throw new IllegalArgumentException("Không thể điều chỉnh tồn kho. Số lượng không đủ.");
                }

                // Refresh entity with variant eagerly loaded for status update
                inventory = inventoryRepository.findByIdWithVariant(inventoryId).get();
                int newQuantity = inventory.getQuantityAvailable();

                // Record transaction - use ADJUSTMENT for manual adjustments
                createTransaction(inventory, previousQuantity, newQuantity, Math.abs(adjustment),
                                TransactionType.ADJUSTMENT,
                                request.getReason(), "adjustment", null);

                log.info("Adjusted inventory {}: {} units", inventoryId, adjustment);

                // Check and update product and variant status if needed
                checkAndUpdateProductStatus(inventory.getProduct().getProductId());
                checkAndUpdateVariantStatus(inventory);

                return InventoryDTO.fromEntity(inventory);
        }

        /**
         * Reserve stock for an order
         */
        @Transactional
        public void reserveStock(Long inventoryId, int quantity) {
                Inventory inventory = inventoryRepository.findById(inventoryId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy tồn kho: " + inventoryId));

                if (inventory.getActualAvailable() < quantity) {
                        throw new IllegalArgumentException("Không đủ hàng trong kho");
                }

                int previousQuantity = inventory.getQuantityAvailable();
                inventory.reserve(quantity);
                inventoryRepository.save(inventory);

                // Use OUT for reservation (stock going out)
                createTransaction(inventory, previousQuantity, inventory.getQuantityAvailable(), quantity,
                                TransactionType.OUT, "Đặt hàng - Reserved", "order", null);
                log.info("Reserved {} units from inventory {}", quantity, inventoryId);
        }

        /**
         * Release reserved stock
         */
        @Transactional
        public void releaseReservation(Long inventoryId, int quantity) {
                Inventory inventory = inventoryRepository.findById(inventoryId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy tồn kho: " + inventoryId));

                int previousQuantity = inventory.getQuantityAvailable();
                inventory.releaseReservation(quantity);
                inventoryRepository.save(inventory);

                // Use RETURN for releasing reservation (stock returning)
                createTransaction(inventory, previousQuantity, inventory.getQuantityAvailable(), quantity,
                                TransactionType.RETURN, "Hủy đặt hàng - Released", "order", null);
                log.info("Released {} units reservation from inventory {}", quantity, inventoryId);
        }

        /**
         * Complete sale (reduce from reserved)
         */
        @Transactional
        public void completeSale(Long inventoryId, int quantity, Long orderId) {
                Inventory inventory = inventoryRepository.findById(inventoryId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy tồn kho: " + inventoryId));

                int previousQuantity = inventory.getQuantityAvailable();
                inventory.sell(quantity);
                inventoryRepository.save(inventory);

                // Use OUT for completed sale
                createTransaction(inventory, previousQuantity, inventory.getQuantityAvailable(), quantity,
                                TransactionType.OUT, "Bán hàng", "order", orderId);
                log.info("Completed sale of {} units from inventory {}", quantity, inventoryId);

                // Check and update product and variant status if needed
                checkAndUpdateProductStatus(inventory.getProduct().getProductId());
                checkAndUpdateVariantStatus(inventory);
        }

        /**
         * Restock inventory
         */
        @Transactional
        @Auditable(entityType = "Inventory", action = ActivityAction.UPDATE, description = "Restocked inventory ID: {0}")
        public InventoryDTO restock(Long inventoryId, int quantity, Long purchaseOrderId) {
                Inventory inventory = inventoryRepository.findById(inventoryId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy tồn kho: " + inventoryId));

                int previousQuantity = inventory.getQuantityAvailable();
                inventory.restock(quantity);
                inventoryRepository.save(inventory);

                // Use IN for restocking
                createTransaction(inventory, previousQuantity, inventory.getQuantityAvailable(), quantity,
                                TransactionType.IN, "Nhập kho", "purchase_order", purchaseOrderId);
                log.info("Restocked {} units to inventory {}", quantity, inventoryId);

                // Check and update product and variant status if needed (might restore from
                // OUT_OF_STOCK)
                checkAndUpdateProductStatus(inventory.getProduct().getProductId());
                checkAndUpdateVariantStatus(inventory);

                return InventoryDTO.fromEntity(inventory);
        }

        /**
         * Get inventory statistics
         */
        public InventoryStats getInventoryStats() {
                return InventoryStats.builder()
                                .totalItems(inventoryRepository.count())
                                .lowStockItems(inventoryRepository.countLowStock())
                                .outOfStockItems(inventoryRepository.countOutOfStock())
                                .build();
        }

        /**
         * Create inventory transaction record
         */
        private void createTransaction(Inventory inventory, int quantityBefore, int quantityAfter,
                        int quantity, TransactionType type, String reason, String referenceType, Long referenceId) {
                InventoryTransaction transaction = InventoryTransaction.builder()
                                .product(inventory.getProduct())
                                .variant(inventory.getVariant())
                                .transactionType(type)
                                .quantity(quantity)
                                .quantityBefore(quantityBefore)
                                .quantityAfter(quantityAfter)
                                .reason(reason)
                                .referenceType(referenceType)
                                .referenceId(referenceId)
                                .createdAt(LocalDateTime.now())
                                .build();
                transactionRepository.save(transaction);
        }

        /**
         * Get transaction history with filters
         */
        public Page<InventoryTransactionDTO> getTransactionHistory(
                        Long productId,
                        TransactionType transactionType,
                        LocalDateTime startDate,
                        LocalDateTime endDate,
                        Pageable pageable) {
                return transactionRepository
                                .searchTransactions(productId, transactionType, startDate, endDate, pageable)
                                .map(InventoryTransactionDTO::fromEntity);
        }

        /**
         * Check and update product status based on inventory levels.
         * If all inventory for a product is out of stock, set product status to
         * OUT_OF_STOCK.
         * If product was OUT_OF_STOCK and now has stock, set it back to ACTIVE.
         */
        @Transactional
        public void checkAndUpdateProductStatus(Long productId) {
                if (productId == null) {
                        return;
                }

                var productOpt = productRepository.findById(productId);
                if (productOpt.isEmpty()) {
                        return;
                }

                var product = productOpt.get();
                boolean hasStock = inventoryRepository.hasAvailableStock(productId);

                if (!hasStock && product.getStatus() == com.badmintonshop.entity.enums.ProductStatus.ACTIVE) {
                        // All inventory is out of stock, update product status
                        product.setStatus(com.badmintonshop.entity.enums.ProductStatus.OUT_OF_STOCK);
                        productRepository.save(product);
                        log.info("Product {} status changed to OUT_OF_STOCK (all variants out of stock)", productId);
                } else if (hasStock
                                && product.getStatus() == com.badmintonshop.entity.enums.ProductStatus.OUT_OF_STOCK) {
                        // Stock is available again, restore product status to ACTIVE
                        product.setStatus(com.badmintonshop.entity.enums.ProductStatus.ACTIVE);
                        productRepository.save(product);
                        log.info("Product {} status restored to ACTIVE (stock available)", productId);
                }
        }

        /**
         * Check and update variant status based on its inventory level.
         * If variant inventory is out of stock, set variant status to INACTIVE.
         * If variant was INACTIVE and now has stock, set it back to ACTIVE.
         */
        @Transactional
        public void checkAndUpdateVariantStatus(Inventory inventory) {
                if (inventory == null) {
                        log.debug("checkAndUpdateVariantStatus: inventory is null");
                        return;
                }

                Long inventoryId = inventory.getInventoryId();
                // Get variant ID directly from DB to avoid lazy loading issues
                Optional<Long> variantIdOpt = inventoryRepository.findVariantIdByInventoryId(inventoryId);

                if (variantIdOpt.isEmpty()) {
                        log.debug("checkAndUpdateVariantStatus: inventory {} has no variant linked in DB", inventoryId);
                        return;
                }

                Long variantId = variantIdOpt.get();
                log.info("checkAndUpdateVariantStatus: Processing variant {} for inventory {}", variantId, inventoryId);

                // Refresh variant from DB to get current status
                var variantOpt = productVariantRepository.findById(variantId);
                if (variantOpt.isEmpty()) {
                        log.warn("checkAndUpdateVariantStatus: Variant {} not found in DB", variantId);
                        return;
                }
                ProductVariant variant = variantOpt.get();

                boolean isOutOfStock = inventory.getQuantityAvailable() <= 0;
                VariantStatus currentStatus = variant.getStatus();

                log.info("checkAndUpdateVariantStatus: variant {} current status={}, quantityAvailable={}, isOutOfStock={}",
                                variantId, currentStatus, inventory.getQuantityAvailable(), isOutOfStock);

                if (isOutOfStock) {
                        try {
                                log.info("Attempting to update variant {} status to INACTIVE. Current stock: {}",
                                                variantId, inventory.getQuantityAvailable());
                                // Updates are now self-flushing via repository annotation
                                productVariantRepository.updateStatus(variantId, VariantStatus.INACTIVE);
                                log.info("Variant {} status updated to INACTIVE", variantId);
                        } catch (Exception e) {
                                log.error("Failed to update variant {} status: {}", variantId, e.getMessage(), e);
                        }
                } else if (!isOutOfStock && currentStatus == VariantStatus.INACTIVE) {
                        try {
                                log.info("Attempting to update variant {} status to ACTIVE. Current stock: {}",
                                                variantId, inventory.getQuantityAvailable());
                                productVariantRepository.updateStatus(variantId, VariantStatus.ACTIVE);
                                log.info("Variant {} status updated to ACTIVE", variantId);
                        } catch (Exception e) {
                                log.error("Failed to update variant {} status: {}", variantId, e.getMessage(), e);
                        }
                } else {
                        log.info("No status change needed for variant {}. Stock: {}, Current Status: {}", variantId,
                                        inventory.getQuantityAvailable(), currentStatus);
                }
        }

        /**
         * Restore stock for an order (when payment fails or order is cancelled)
         * This reverses the stock reduction done during order creation
         */
        @Transactional
        public void restoreStockForOrder(com.badmintonshop.entity.Order order) {
                if (order == null || order.getItems() == null) {
                        log.warn("restoreStockForOrder: Order or items is null");
                        return;
                }

                for (var orderItem : order.getItems()) {
                        try {
                                Long productId = orderItem.getProduct().getProductId();
                                Long variantId = orderItem.getVariant() != null ? orderItem.getVariant().getVariantId()
                                                : null;
                                int quantity = orderItem.getQuantity();

                                // Find inventory for this product/variant
                                Inventory inventory;
                                if (variantId != null) {
                                        inventory = inventoryRepository.findByVariantVariantId(variantId).orElse(null);
                                } else {
                                        inventory = inventoryRepository
                                                        .findByProductProductIdAndVariantIsNull(productId).orElse(null);
                                }

                                if (inventory != null) {
                                        int previousQty = inventory.getQuantityAvailable();
                                        inventory.restock(quantity);
                                        inventoryRepository.save(inventory);

                                        // Record transaction
                                        createTransaction(inventory, previousQty, inventory.getQuantityAvailable(),
                                                        quantity,
                                                        TransactionType.RETURN, "Hoàn kho - Payment failed/cancelled",
                                                        "order", order.getOrderId());

                                        log.info("Restored {} units for product {} variant {} (order {})",
                                                        quantity, productId, variantId, order.getOrderNumber());

                                        // Update status if needed
                                        checkAndUpdateProductStatus(productId);
                                        checkAndUpdateVariantStatus(inventory);
                                } else {
                                        log.warn("Inventory not found for product {} variant {}", productId, variantId);
                                }
                        } catch (Exception e) {
                                log.error("Failed to restore stock for order item: {}", e.getMessage(), e);
                        }
                }

                log.info("Completed stock restoration for order {}", order.getOrderNumber());
        }

        @lombok.Builder
        @lombok.Getter
        public static class InventoryStats {
                private long totalItems;
                private long lowStockItems;
                private long outOfStockItems;
        }
}
