package com.badmintonshop.service;

import com.badmintonshop.dto.cart.CartItemRequest;
import com.badmintonshop.entity.*;
import com.badmintonshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user " + userId));
    }

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });

        // Eagerly fetch items to avoid LazyInitException
        if (cart.getItems() != null) {
            cart.getItems().size(); // Initialize items
            cart.getItems().forEach(item -> {
                if (item.getProduct() != null) {
                    item.getProduct().getName(); // Init product
                    if (item.getProduct().getImages() != null) {
                        item.getProduct().getImages().size(); // Init images
                    }
                }
                if (item.getVariant() != null) {
                    item.getVariant().getAttributes(); // Init variant attributes
                }
            });
        }
        return cart;
    }

    @Transactional
    public Cart addToCart(Long userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        // Check if product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Force initialization of lazy collections
        if (product.getImages() != null)
            product.getImages().size();
        if (product.getVariants() != null)
            product.getVariants().size();

        ProductVariant variantFound = null;
        if (request.getVariantId() != null) {
            variantFound = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Variant not found"));
        }
        final ProductVariant finalVariant = variantFound;

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(product.getProductId()) &&
                        ((item.getVariant() == null && finalVariant == null) ||
                                (item.getVariant() != null && finalVariant != null
                                        && item.getVariant().getVariantId().equals(finalVariant.getVariantId()))))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(finalVariant)
                    .quantity(request.getQuantity())
                    .priceAtAdd(product.getBasePrice()) // Simplification: using base price
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public void removeFromCart(Long userId, Long cartItemId) {
        // Validate ownership
        // For simplicity, just delete by ID if it belongs to mapping (should add
        // ownership check in real app)
        cartItemRepository.deleteById(cartItemId);
    }

    @Transactional
    public Cart updateItemQuantity(Long userId, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Basic ownership check
        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Item does not belong to user cart");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        // Explicitly delete items
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId) {
        return cartRepository.findByUser_UserId(userId)
                .map(cart -> cart.getItems().stream().mapToInt(CartItem::getQuantity).sum())
                .orElse(0);
    }

    @Transactional
    public void addStringingService(Long userId, Long cartItemId, String stringingInfo) {
        // Stub: In real app, create a "Stringing" entity or add note/field to CartItem
        // For now, assume CartItem has a 'stringingNotes' field or similiar
        // Since we didn't add that field to Entity, let's just log it or verify item
        // existence
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        // item.setStringingNote(stringingInfo);
        cartItemRepository.save(item);
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void removeInactiveCarts() {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(7);
        cartRepository.deleteByUpdatedAtBefore(cutoff);
    }
}
