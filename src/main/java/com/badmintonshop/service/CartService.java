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
        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user " + userId));
        initializeCart(cart);
        return cart;
    }

    @Transactional
    public Cart getOrCreateCart(Long userId, String sessionId) {
        Cart cart;
        boolean isNew = false;

        if (userId != null) {
            // Logged in user - Check for merge
            Optional<Cart> userCartOpt = cartRepository.findByUser_UserId(userId);
            Optional<Cart> sessionCartOpt = sessionId != null ? cartRepository.findBySessionId(sessionId)
                    : Optional.empty();

            if (userCartOpt.isPresent()) {
                cart = userCartOpt.get();
                // Merge if session cart exists and is different
                if (sessionCartOpt.isPresent() && !sessionCartOpt.get().getCartId().equals(cart.getCartId())) {
                    mergeCarts(cart, sessionCartOpt.get());
                }
            } else {
                // No user cart.
                if (sessionCartOpt.isPresent()) {
                    // Assign session cart to user
                    cart = sessionCartOpt.get();
                    cart.setUser(userRepository.findById(userId).orElseThrow());
                    cart.setSessionId(null); // Clear session ID or keep it? Better clear to avoid conflict.
                    cart = cartRepository.save(cart);
                } else {
                    // Create new user cart
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    cart = Cart.builder().user(user).build();
                    cart = cartRepository.save(cart);
                }
            }
        } else if (sessionId != null) {
            // Guest user
            cart = cartRepository.findBySessionId(sessionId)
                    .orElseGet(() -> {
                        Cart newCart = Cart.builder().sessionId(sessionId).build();
                        return cartRepository.save(newCart);
                    });
        } else {
            throw new RuntimeException("Either userId or sessionId must be provided");
        }

        initializeCart(cart);
        return cart;
    }

    private void mergeCarts(Cart targetCart, Cart sourceCart) {
        for (CartItem sourceItem : sourceCart.getItems()) {
            // Check if exists in target
            boolean exists = false;
            for (CartItem targetItem : targetCart.getItems()) {
                if (targetItem.getProduct().getProductId().equals(sourceItem.getProduct().getProductId()) &&
                        ((targetItem.getVariant() == null && sourceItem.getVariant() == null) ||
                                (targetItem.getVariant() != null && sourceItem.getVariant() != null &&
                                        targetItem.getVariant().getVariantId()
                                                .equals(sourceItem.getVariant().getVariantId())))) {

                    targetItem.setQuantity(targetItem.getQuantity() + sourceItem.getQuantity());
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                // Move item
                CartItem newItem = CartItem.builder()
                        .cart(targetCart)
                        .product(sourceItem.getProduct())
                        .variant(sourceItem.getVariant())
                        .quantity(sourceItem.getQuantity())
                        .priceAtAdd(sourceItem.getPriceAtAdd())
                        .build();
                targetCart.getItems().add(newItem);
            }
        }
        cartRepository.save(targetCart);
        cartRepository.delete(sourceCart); // Delete old session cart
    }

    private void initializeCart(Cart cart) {
        if (cart.getItems() != null) {
            cart.getItems().size();
            for (CartItem item : cart.getItems()) {
                if (item.getProduct() != null) {
                    item.getProduct().getName(); // Init proxy
                    if (item.getProduct().getImages() != null) {
                        item.getProduct().getImages().size(); // Init images
                    }
                }
                if (item.getVariant() != null) {
                    item.getVariant().getVariantName(); // Init proxy
                }
            }
        }
    }

    // Overload for backward compatibility if needed, or update callers
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return getOrCreateCart(userId, null);
    }

    @Transactional
    public Cart addToCart(Long userId, String sessionId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId, sessionId);

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
    public void removeFromCart(Long userId, String sessionId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId, sessionId);

        // Find the item in the cart's collection to ensure the right association
        CartItem itemToRemove = cart.getItems().stream()
                .filter(item -> item.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in your cart"));

        cart.getItems().remove(itemToRemove);
        cartItemRepository.delete(itemToRemove);
        cartRepository.save(cart);
    }

    @Transactional
    public Cart updateItemQuantity(Long userId, String sessionId, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCart(userId, sessionId);
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
    public void clearCart(Long userId, String sessionId) {
        Cart cart = getOrCreateCart(userId, sessionId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId, String sessionId) {
        if (userId != null) {
            return cartRepository.findByUser_UserId(userId)
                    .map(cart -> cart.getItems().stream().mapToInt(CartItem::getQuantity).sum())
                    .orElse(0);
        }
        return cartRepository.findBySessionId(sessionId)
                .map(cart -> cart.getItems().stream().mapToInt(CartItem::getQuantity).sum())
                .orElse(0);
    }

    @Transactional
    public void addStringingService(Long userId, String sessionId, Long cartItemId, String stringingInfo) {
        Cart cart = getOrCreateCart(userId, sessionId);
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
