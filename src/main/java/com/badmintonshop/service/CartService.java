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
    private final SystemSettingService systemSettingService;

    private static final int CART_EXPIRY_DAYS = 7;
    
    // Default shipping fee configuration (fallback if settings not found)
    private static final String DEFAULT_FREE_SHIPPING_THRESHOLD = "500000"; // 500k VND
    private static final String DEFAULT_SHIPPING_FEE_VALUE = "30000"; // 30k VND
    
    /**
     * Get free shipping threshold from system settings
     */
    private BigDecimal getFreeShippingThreshold() {
        String value = systemSettingService.getValue("free_shipping_threshold", DEFAULT_FREE_SHIPPING_THRESHOLD);
        return new BigDecimal(value);
    }
    
    /**
     * Get default shipping fee from system settings
     */
    private BigDecimal getDefaultShippingFee() {
        String value = systemSettingService.getValue("default_shipping_fee", DEFAULT_SHIPPING_FEE_VALUE);
        return new BigDecimal(value);
    }

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
        log.info("getCartResponse called for userId: {}", userId);
        Optional<Cart> cartOpt = cartRepository.findByUserWithItems(userId);
        if (cartOpt.isEmpty()) {
            log.info("No cart found for userId: {}", userId);
            return buildEmptyCartResponse();
        }
        Cart cart = cartOpt.get();
        log.info("Found cart {} for userId: {}, items count: {}", 
                cart.getCartId(), userId, cart.getItems().size());
        return mapToResponse(cart);
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
        log.info("addToCart called - userId: {}, sessionId: {}", userId, sessionId);
        Cart cart;
        if (userId != null) {
            cart = getOrCreateCart(userId);
            log.info("Got/Created cart {} for userId: {}", cart.getCartId(), userId);
        } else if (sessionId != null) {
            cart = getOrCreateGuestCart(sessionId);
            log.info("Got/Created cart {} for sessionId: {}", cart.getCartId(), sessionId);
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

        BigDecimal price = variant != null ? variant.getFinalPrice() : product.getCurrentPrice();
        
        // Check inventory
        checkInventoryAvailability(product, variant, request.getQuantity());

        // CASE 1: Racket with stringing service -> Split into individual items (quantity=1 each)
        if (product.isRacket() && request.getStringingServiceId() != null) {
            StringingService stringingService = stringingServiceRepository.findById(request.getStringingServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stringing service not found"));
            
            StringProduct stringProduct = null;
            if (request.getStringProductId() != null) {
                stringProduct = stringProductRepository.findById(request.getStringProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("String product not found"));
            }
            
            // Create separate cart item for each racket
            for (int i = 0; i < request.getQuantity(); i++) {
                CartItem item = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .variant(variant)
                        .quantity(1)  // Always 1 for rackets with stringing
                        .priceAtAdd(price)
                        .stringingService(stringingService)
                        .stringProduct(stringProduct)
                        .tension(request.getTension())
                        .stringingNotes(request.getStringingNotes())
                        .build();
                cart.addItem(item);
                cartItemRepository.save(item);
            }
            log.info("Added {} rackets with stringing to cart: product={}", request.getQuantity(), request.getProductId());
        }
        // CASE 2: Regular product or racket without stringing -> Merge quantity
        else {
            Optional<CartItem> existingItem;
            if (variant != null) {
                existingItem = cartItemRepository.findByCartCartIdAndProductProductIdAndVariantVariantId(
                        cart.getCartId(), product.getProductId(), variant.getVariantId());
            } else {
                existingItem = cartItemRepository.findByCartAndProductNoVariant(
                        cart.getCartId(), product.getProductId());
            }

            if (existingItem.isPresent()) {
                // Only merge if item doesn't have stringing
                CartItem item = existingItem.get();
                if (!item.hasStringingService()) {
                    item.increaseQuantity(request.getQuantity());
                    cartItemRepository.save(item);
                    log.info("Increased quantity for cart item {}", item.getCartItemId());
                } else {
                    // Item has stringing, add as new item
                    CartItem newItem = CartItem.builder()
                            .cart(cart)
                            .product(product)
                            .variant(variant)
                            .quantity(request.getQuantity())
                            .priceAtAdd(price)
                            .build();
                    cart.addItem(newItem);
                    cartItemRepository.save(newItem);
                    log.info("Added new item (existing has stringing): product={}", request.getProductId());
                }
            } else {
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
        }

        // Update cart expiry
        cart.setExpiresAt(LocalDateTime.now().plusDays(CART_EXPIRY_DAYS));
        cartRepository.save(cart);

        return mapToResponse(cart);
    }


    /**
     * Update cart item quantity - supports both logged-in users and guests
     */
    public CartResponse updateCartItem(Long cartItemId, Long userId, String sessionId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        // Verify ownership
        Cart cart = item.getCart();
        if (cart.getUser() != null && userId != null && !cart.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("You don't have permission to modify this cart");
        }
        if (cart.getUser() == null && sessionId != null && !cart.getSessionId().equals(sessionId)) {
            throw new IllegalStateException("You don't have permission to modify this cart");
        }

        if (quantity <= 0) {
            // Remove item if quantity is 0 or negative
            cart.removeItem(item);
            cartItemRepository.delete(item);
            log.info("Removed cart item {}", cartItemId);
        } else {
            // Check stock availability before updating quantity
            int availableStock = 0;
            String productName = "";
            
            if (item.getVariant() != null) {
                availableStock = item.getVariant().getStockQuantity();
                productName = item.getVariant().getProduct().getName() + 
                    (item.getVariant().getVariantName() != null ? " - " + item.getVariant().getVariantName() : "");
            } else if (item.getProduct() != null) {
                // For products without variants, check total stock from all variants or product stock
                ProductVariant defaultVariant = variantRepository.findByProductProductIdOrderByVariantIdAsc(item.getProduct().getProductId())
                        .stream().findFirst().orElse(null);
                if (defaultVariant != null) {
                    availableStock = defaultVariant.getStockQuantity();
                }
                productName = item.getProduct().getName();
            }
            
            if (quantity > availableStock) {
                throw new IllegalStateException(
                    String.format("Số lượng yêu cầu (%d) vượt quá tồn kho hiện có (%d) cho sản phẩm: %s", 
                        quantity, availableStock, productName));
            }
            
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
            BigDecimal itemTotal;
            
            if (item.getPromotionPrice() != null) {
                // Use promotion price for product
                itemTotal = item.getPromotionPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                if (item.getPromotionDiscount() != null) {
                    totalPromotionDiscount = totalPromotionDiscount.add(
                            item.getPromotionDiscount().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            } else {
                // Use original unit price
                itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            }
            
            // Add stringing service price if applicable
            if (item.getStringingPrice() != null) {
                itemTotal = itemTotal.add(item.getStringingPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            // Add string product price if applicable
            if (item.getStringPrice() != null) {
                itemTotal = itemTotal.add(item.getStringPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            
            subtotal = subtotal.add(itemTotal);
        }

        int totalQuantity = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        // Get coupon discount from cart
        BigDecimal couponDiscount = cart.getCouponDiscount() != null ? cart.getCouponDiscount() : BigDecimal.ZERO;

        // Calculate shipping fee based on subtotal
        BigDecimal shippingFee = calculateShippingFee(subtotal);
        BigDecimal total = subtotal.add(shippingFee).subtract(couponDiscount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .userId(cart.getUser() != null ? cart.getUser().getUserId() : null)
                .sessionId(cart.getSessionId())
                .items(items)
                .totalItems(items.size())
                .totalQuantity(totalQuantity)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discount(couponDiscount) // Coupon discount from cart
                .promotionDiscount(totalPromotionDiscount)
                .couponCode(cart.getCouponCode())
                .freeShippingThreshold(getFreeShippingThreshold())
                .total(total) // After promotion, coupon and shipping
                .isEmpty(items.isEmpty())
                .build();
    }

    /**
     * Calculate shipping fee based on subtotal
     * Free shipping for orders >= threshold, otherwise default shipping fee
     */
    public BigDecimal calculateShippingFee(BigDecimal subtotal) {
        BigDecimal threshold = getFreeShippingThreshold();
        if (subtotal.compareTo(threshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return getDefaultShippingFee();
    }

    /**
     * Get free shipping threshold for display (public method)
     */
    public BigDecimal getPublicFreeShippingThreshold() {
        return getFreeShippingThreshold();
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
                .categorySlug(product.getCategory() != null ? product.getCategory().getSlug() : null)
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
     * Also clears applied coupon
     */
    public void clearCart(User user, String sessionId) {
        Cart cart = getCart(user, sessionId);
        cart.clear();
        cart.setCouponCode(null);
        cart.setCouponDiscount(null);
        cartRepository.save(cart);
        log.info("Cleared cart {} (including coupon)", cart.getCartId());
    }

    /**
     * Apply coupon to cart
     */
    @Transactional
    public CartResponse applyCouponToCart(Long userId, String sessionId, String couponCode, BigDecimal discountAmount) {
        Cart cart;
        if (userId != null) {
            cart = cartRepository.findByUserWithItems(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));
        } else if (sessionId != null) {
            cart = cartRepository.findBySessionWithItems(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));
        } else {
            throw new IllegalArgumentException("UserId hoặc sessionId phải được cung cấp");
        }

        cart.setCouponCode(couponCode);
        cart.setCouponDiscount(discountAmount);
        cartRepository.save(cart);

        log.info("Applied coupon {} to cart {}, discount: {}", couponCode, cart.getCartId(), discountAmount);
        return mapToResponse(cart);
    }

    /**
     * Remove coupon from cart
     */
    @Transactional
    public CartResponse removeCouponFromCart(Long userId, String sessionId) {
        Cart cart;
        if (userId != null) {
            cart = cartRepository.findByUserWithItems(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));
        } else if (sessionId != null) {
            cart = cartRepository.findBySessionWithItems(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));
        } else {
            throw new IllegalArgumentException("UserId hoặc sessionId phải được cung cấp");
        }

        cart.setCouponCode(null);
        cart.setCouponDiscount(null);
        cartRepository.save(cart);

        log.info("Removed coupon from cart {}", cart.getCartId());
        return mapToResponse(cart);
    }
}
