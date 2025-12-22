package com.badmintonshop.service;

import com.badmintonshop.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public boolean decreaseStock(Long productId, Long variantId, int quantity) {
        int updated = inventoryRepository.atomicDecrease(productId, variantId, quantity);
        if (updated == 0) {
            Integer available = inventoryRepository.getAvailable(productId, variantId);
            throw new RuntimeException("Insufficient stock for Product " + productId +
                    (variantId != null ? " Variant " + variantId : "") +
                    ". Requested: " + quantity + ", Available: " + (available != null ? available : 0));
        }
        // Log transaction logic can be added here (Member 2 scope)
        return true;
    }

    public boolean checkStock(Long productId, Long variantId, int quantity) {
        Integer available = inventoryRepository.getAvailable(productId, variantId);
        return available != null && available >= quantity;
    }

    @Transactional
    public void increaseStock(Long productId, Long variantId, int quantity) {
        inventoryRepository.atomicIncrease(productId, variantId, quantity);
    }
}
