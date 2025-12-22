package com.badmintonshop.service;

import com.badmintonshop.dto.cart.*;
import com.badmintonshop.entity.*;
import com.badmintonshop.exception.ResourceNotFoundException;
import com.badmintonshop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final PromotionPriceService promotionPriceService;
    private final StringingServiceRepository stringingServiceRepository;
    private final StringProductRepository stringProductRepository;
    private final InventoryRepository inventoryRepository;

    private static final int CART_EXPIRY_DAYS = 7;

    /**
     * Get or create cart for logged-in user
     */
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart cart = Cart.builder()
                            .user(user)
                            .expiresAt(LocalDateTime.now().plusDays(CART_EXPIRY_DAYS))
                            .build();
                    return cartRepository.save(cart);
                });
    }

    /**
     * Get or create cart for guest
     */
    public Cart getOrCreateGuestCart(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart cart = Cart.builder()
                            .sessionId(sessionId)
                            .expiresAt(LocalDateTime.now().plusDays(CART_EXPIRY_DAYS))
                            .build();
                    return cartRepository.save(cart);
                });
    }

    /**
     * Get cart response for user
     */
    @Transactional(readOnly = true)
    public CartResponse getCartResponse(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserWithItems(userId);
        if (cartOpt.isEmpty()) {
            return buildEmptyCartResponse();
        }
        return mapToResponse(cartOpt.get());
    }

    /**
     * Get cart response for guest
     */
    @Transactional(readOnly = true)
    public CartResponse getGuestCartResponse(String sessionId) {
        Optional<Cart> cartOpt = cartRepository.findBySessionWithItems(sessionId);
        if (cartOpt.isEmpty()) {
            return buildEmptyCartResponse();
        }
        return mapToResponse(cartOpt.get());
    }

    /**
     * Add item to cart
     */
    public CartResponse addToCart(Long userId, String sessionId, AddToCartRequest request) {
        Cart cart;
        if (userId != null) {
            cart = getOrCreateCart(userId);
        } else if (sessionId != null) {
            cart = getOrCreateGuestCart(sessionId);
        } else {
            throw new IllegalArgumentException("Either userId or sessionId must be provided");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        }

        // Check if item already exists in cart
        Optional<CartItem> existingItem;
        if (variant != null) {
            existingItem = cartItemRepository.findByCartCartIdAndProductProductIdAndVariantVariantId(
                    cart.getCartId(), product.getProductId(), variant.getVariantId());
        } else {
            existingItem = cartItemRepository.findByCartAndProductNoVariant(
                    cart.getCartId(), product.getProductId());
        }

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            // Check inventory before updating
            checkInventoryAvailability(product, variant, newQuantity);

            item.increaseQuantity(request.getQuantity());
            cartItemRepository.save(item);
            log.info("Increased quantity for cart item {}", item.getCartItemId());
        } else {
            // Check inventory before adding new item
            checkInventoryAvailability(product, variant, request.getQuantity());

            // Add new item
            BigDecimal price = variant != null ? variant.getFinalPrice() : product.getCurrentPrice();

            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .priceAtAdd(price)
                    .build();

            cart.addItem(item);
            cartItemRepository.save(item);
            log.info("Added new item to cart: product={}, variant={}",
                    request.getProductId(), request.getVariantId());
        }

        // Update cart expiry
        cart.setExpiresAt(LocalDateTime.now().plusDays(CART_EXPIRY_DAYS));
        cartRepository.save(cart);

        return mapToResponse(cart);
    }

    /**
     * Update cart item quantity
     */
    public CartResponse updateCartItem(Long cartItemId, Long userId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        // Verify ownership
        Cart cart = item.getCart();
        if (cart.getUser() != null && !cart.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("You don't have permission to modify this cart");
        }

        if (quantity <= 0) {
            // Remove item if quantity is 0 or negative
            cart.removeItem(item);
            cartItemRepository.delete(item);
            log.info("Removed cart item {}", cartItemId);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
            log.info("Updated cart item {} quantity to {}", cartItemId, quantity);
        }

        return mapToResponse(cart);
    }

    /**
     * Remove item from cart
     */
    public CartResponse removeFromCart(Long cartItemId, Long userId, String sessionId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        Cart cart = item.getCart();

        // Verify ownership
        if (cart.getUser() != null && userId != null && !cart.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("You don't have permission to modify this cart");
        }
        if (cart.getUser() == null && !cart.getSessionId().equals(sessionId)) {
            throw new IllegalStateException("You don't have permission to modify this cart");
        }

        cart.removeItem(item);
        cartItemRepository.delete(item);
        log.info("Removed cart item {}", cartItemId);

        return mapToResponse(cart);
    }

    /**
     * Clear cart
     */
    public void clearCart(Long userId, String sessionId) {
        Cart cart;
        if (userId != null) {
            cart = cartRepository.findByUserUserId(userId).orElse(null);
        } else {
            cart = cartRepository.findBySessionId(sessionId).orElse(null);
        }

        if (cart != null) {
            cart.clear();
            cartRepository.save(cart);
            log.info("Cleared cart {}", cart.getCartId());
        }
    }

    /**
     * Get cart item count
     */
    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId, String sessionId) {
        Optional<Cart> cartOpt;
        if (userId != null) {
            cartOpt = cartRepository.findByUserUserId(userId);
        } else {
            cartOpt = cartRepository.findBySessionId(sessionId);
        }

        if (cartOpt.isEmpty()) {
            return 0;
        }
        return cartItemRepository.getTotalQuantity(cartOpt.get().getCartId());
    }

    /**
     * Merge guest cart into user cart when user logs in
     */
    public void mergeGuestCart(String sessionId, Long userId) {
        Optional<Cart> guestCartOpt = cartRepository.findBySessionWithItems(sessionId);
        if (guestCartOpt.isEmpty()) {
            return;
        }

        Cart guestCart = guestCartOpt.get();
        Cart userCart = getOrCreateCart(userId);

        for (CartItem guestItem : guestCart.getItems()) {
            // Check if item already exists in user cart
            Optional<CartItem> existingItem;
            if (guestItem.getVariant() != null) {
                existingItem = cartItemRepository.findByCartCartIdAndProductProductIdAndVariantVariantId(
                        userCart.getCartId(),
                        guestItem.getProduct().getProductId(),
                        guestItem.getVariant().getVariantId());
            } else {
                existingItem = cartItemRepository.findByCartAndProductNoVariant(
                        userCart.getCartId(),
                        guestItem.getProduct().getProductId());
            }

            if (existingItem.isPresent()) {
                // Increase quantity
                CartItem item = existingItem.get();
                item.increaseQuantity(guestItem.getQuantity());
                cartItemRepository.save(item);
            } else {
                // Add new item to user cart
                CartItem newItem = CartItem.builder()
                        .cart(userCart)
                        .product(guestItem.getProduct())
                        .variant(guestItem.getVariant())
                        .quantity(guestItem.getQuantity())
                        .priceAtAdd(guestItem.getPriceAtAdd())
                        .stringingService(guestItem.getStringingService())
                        .stringProduct(guestItem.getStringProduct())
                        .tension(guestItem.getTension())
                        .stringingNotes(guestItem.getStringingNotes())
                        .build();
                userCart.addItem(newItem);
                cartItemRepository.save(newItem);
            }
        }

        // Delete guest cart
        cartRepository.delete(guestCart);
        log.info("Merged guest cart {} into user cart {}", guestCart.getCartId(), userCart.getCartId());
    }

    /**
     * Check if requested quantity is available in inventory
     * Throws InsufficientStockException if not enough stock
     */
    private void checkInventoryAvailability(Product product, ProductVariant variant, int requestedQuantity) {
        int availableQuantity = 0;

        if (variant != null) {
            // Check inventory for specific variant
            Optional<Inventory> inventory = inventoryRepository.findByVariantVariantId(variant.getVariantId());
            if (inventory.isPresent()) {
                availableQuantity = inventory.get().getActualAvailable();
            }
        } else {
            // Check total inventory for product (when no variant specified)
            availableQuantity = inventoryRepository.getTotalAvailableQuantity(product.getProductId());
        }

        if (requestedQuantity > availableQuantity) {
            String productName = variant != null
                    ? product.getName() + " - " + variant.getVariantName()
                    : product.getName();
            log.warn("Insufficient stock for {}: requested={}, available={}",
                    productName, requestedQuantity, availableQuantity);
            throw new com.badmintonshop.exception.InsufficientStockException(
                    product.getProductId(), requestedQuantity, availableQuantity);
        }
    }

    // Helper methods
    private CartResponse buildEmptyCartResponse() {
        return CartResponse.builder()
                .items(List.of())
                .totalItems(0)
                .totalQuantity(0)
                .subtotal(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .promotionDiscount(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .isEmpty(true)
                .build();
    }

    private CartResponse mapToResponse(Cart cart) {
        List<CartItemDTO> items = cart.getItems().stream()
                .map(this::mapItemToDTO)
                .collect(Collectors.toList());

        // Calculate subtotal using promotion prices where applicable
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalPromotionDiscount = BigDecimal.ZERO;

        for (CartItemDTO item : items) {
            if (item.getPromotionPrice() != null) {
                // Use promotion price if available
                subtotal = subtotal.add(item.getPromotionPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                if (item.getPromotionDiscount() != null) {
                    totalPromotionDiscount = totalPromotionDiscount.add(
                            item.getPromotionDiscount().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            } else {
                // Use original subtotal
                subtotal = subtotal.add(item.getSubtotal());
            }
        }

        int totalQuantity = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .userId(cart.getUser() != null ? cart.getUser().getUserId() : null)
                .sessionId(cart.getSessionId())
                .items(items)
                .totalItems(items.size())
                .totalQuantity(totalQuantity)
                .subtotal(subtotal)
                .shippingFee(BigDecimal.ZERO) // TODO: Calculate based on shipping rules
                .discount(BigDecimal.ZERO) // Coupon discounts (applied separately at checkout)
                .promotionDiscount(totalPromotionDiscount)
                .total(subtotal) // After promotion, before coupon
                .isEmpty(items.isEmpty())
                .build();
    }

    private CartItemDTO mapItemToDTO(CartItem item) {
        Product product = item.getProduct();
        ProductImage primaryImage = product.getPrimaryImage();
        Long categoryId = product.getCategory() != null ? product.getCategory().getCategoryId() : null;

        // Calculate promotion price
        BigDecimal unitPrice = item.getPriceAtAdd();
        BigDecimal promotionPrice = null;
        BigDecimal promotionDiscount = null;
        String promotionName = null;

        if (promotionPriceService.hasActivePromotion(product.getProductId(), categoryId)) {
            promotionPrice = promotionPriceService.calculatePromotionPrice(unitPrice, product.getProductId(),
                    categoryId);
            promotionDiscount = promotionPriceService.calculatePromotionDiscount(unitPrice, product.getProductId(),
                    categoryId);
            PromotionPriceService.PromotionInfo info = promotionPriceService
                    .getAppliedPromotionInfo(product.getProductId(), categoryId);
            if (info != null) {
                promotionName = info.discountText();
            }
        }

        return CartItemDTO.builder()
                .cartItemId(item.getCartItemId())
                .productId(product.getProductId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .productImage(primaryImage != null ? primaryImage.getImageUrl() : null)
                .variantId(item.getVariant() != null ? item.getVariant().getVariantId() : null)
                .variantName(item.getVariant() != null ? item.getVariant().getVariantName() : null)
                .quantity(item.getQuantity())
                .unitPrice(unitPrice)
                .subtotal(item.getSubtotal())
                .promotionPrice(promotionPrice)
                .promotionDiscount(promotionDiscount)
                .promotionName(promotionName)
                .stringingServiceId(
                        item.getStringingService() != null ? item.getStringingService().getServiceId() : null)
                .stringingServiceName(
                        item.getStringingService() != null ? item.getStringingService().getServiceName() : null)
                .stringingPrice(item.getStringingService() != null ? item.getStringingService().getBasePrice() : null)
                .stringProductId(item.getStringProduct() != null ? item.getStringProduct().getStringId() : null)
                .stringProductName(item.getStringProduct() != null ? item.getStringProduct().getName() : null)
                .stringPrice(item.getStringProduct() != null ? item.getStringProduct().getRetailPrice() : null)
                .tension(item.getTension())
                .stringingNotes(item.getStringingNotes())
                .inStock(product.isAvailable())
                .build();
    }

    // ===== Methods for page-based CartController and OrderService =====

    /**
     * Get cart for User or session (for page-based controller)
     * Uses fetch queries to eagerly load items and avoid
     * LazyInitializationException
     */
    public Cart getCart(User user, String sessionId) {
        if (user != null) {
            // Use fetch query to load items eagerly
            return cartRepository.findByUserWithItems(user.getUserId())
                    .orElseGet(() -> {
                        Cart cart = Cart.builder()
                                .user(user)
                                .expiresAt(LocalDateTime.now().plusDays(CART_EXPIRY_DAYS))
                                .build();
                        return cartRepository.save(cart);
                    });
        } else {
            // Use fetch query to load items eagerly
            return cartRepository.findBySessionWithItems(sessionId)
                    .orElseGet(() -> {
                        Cart cart = Cart.builder()
                                .sessionId(sessionId)
                                .expiresAt(LocalDateTime.now().plusDays(CART_EXPIRY_DAYS))
                                .build();
                        return cartRepository.save(cart);
                    });
        }
    }

    /**
     * Add item to cart (for page-based controller)
     */
    public Cart addToCart(User user, String sessionId, Long productId, Long variantId, int quantity) {
        Cart cart = getCart(user, sessionId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductVariant variant = null;
        if (variantId != null) {
            variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        }

        // Check if item exists
        final Long finalVariantId = variantId;
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(productId) &&
                        (finalVariantId == null ||
                                (item.getVariant() != null && item.getVariant().getVariantId().equals(finalVariantId))))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;

            // Check inventory before updating
            checkInventoryAvailability(product, variant, newQuantity);

            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            // Check inventory before adding new item
            checkInventoryAvailability(product, variant, quantity);

            BigDecimal price = (variant != null) ? variant.getFinalPrice() : product.getCurrentPrice();
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .quantity(quantity)
                    .priceAtAdd(price)
                    .build();
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        cart.setExpiresAt(LocalDateTime.now().plusDays(CART_EXPIRY_DAYS));
        return cartRepository.save(cart);
    }

    /**
     * Update stringing options for a cart item
     */
    public void updateStringingOption(Long cartItemId, Long serviceId, Long stringId, BigDecimal tension,
            String notes) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (serviceId != null) {
            StringingService service = stringingServiceRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stringing service not found"));
            item.setStringingService(service);
        } else {
            item.setStringingService(null);
        }

        if (stringId != null) {
            StringProduct stringProduct = stringProductRepository.findById(stringId)
                    .orElseThrow(() -> new ResourceNotFoundException("String product not found"));
            item.setStringProduct(stringProduct);
        } else {
            item.setStringProduct(null);
        }

        item.setTension(tension);
        item.setStringingNotes(notes);

        cartItemRepository.save(item);
        log.info("Updated stringing options for cart item {}", cartItemId);
    }

    /**
     * Remove a cart item by ID
     */
    public void removeItem(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
        log.info("Removed cart item {}", cartItemId);
    }

    /**
     * Clear cart for User or session (for page-based controller)
     */
    public void clearCart(User user, String sessionId) {
        Cart cart = getCart(user, sessionId);
        cart.clear();
        cartRepository.save(cart);
        log.info("Cleared cart {}", cart.getCartId());
    }
}
