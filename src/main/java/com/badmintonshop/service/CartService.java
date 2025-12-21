package com.badmintonshop.service;

import com.badmintonshop.dto.CartItemRequest;
import com.badmintonshop.dto.CartItemResponse;
import com.badmintonshop.dto.CartResponse;
import com.badmintonshop.entity.*;
import com.badmintonshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.badmintonshop.dto.StringingRequest;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    // Inject các Repository cần thiết cho việc thêm item
    private final StringingServiceRepository stringingServiceRepository;
    private final StringProductRepository stringProductRepository;
    private final UserRepository userRepository;

    public CartResponse getCartResponse(Long userId, String sessionId) {
        Cart cart = getCartEntity(userId, sessionId);
        return mapToCartResponse(cart);
    }

    public CartResponse addToCart(Long userId, String sessionId, CartItemRequest request) {
        Cart cart = getCartEntity(userId, sessionId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

        // FIX: Dùng tên đầy đủ để tránh xung đột với
        // com.badmintonshop.service.StringingService
        com.badmintonshop.entity.StringingService stringingServiceEntity = null;
        if (request.getStringingServiceId() != null) {
            stringingServiceEntity = stringingServiceRepository.findById(request.getStringingServiceId())
                    .orElseThrow(() -> new RuntimeException("Dịch vụ đan không tồn tại!"));
        }

        StringProduct stringProductEntity = null;
        if (request.getStringId() != null) {
            stringProductEntity = stringProductRepository.findById(request.getStringId())
                    .orElseThrow(() -> new RuntimeException("Loại cước không tồn tại!"));
        }

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(request.getProductId()) &&
                        isStringingMatching(item, request))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .priceAtAdd(product.getBasePrice())
                    // Set Entity vào CartItem
                    .stringingService(stringingServiceEntity)
                    .stringProduct(stringProductEntity)
                    .tension(request.getTension())
                    .stringingNotes(request.getStringingNotes())
                    .build();
            cart.addItem(newItem);
        }

        cartRepository.save(cart);
        return mapToCartResponse(cart);
    }

    public CartResponse updateQuantity(Long userId, String sessionId, Long cartItemId, int newQuantity) {
        Cart cart = getCartEntity(userId, sessionId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Không có quyền sửa");
        }

        if (newQuantity <= 0)
            cart.removeItem(item);
        else
            item.setQuantity(newQuantity);

        cartRepository.save(cart);
        return mapToCartResponse(cart);
    }

    public CartResponse removeItem(Long userId, String sessionId, Long cartItemId) {
        Cart cart = getCartEntity(userId, sessionId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (item.getCart().getCartId().equals(cart.getCartId())) {
            cart.removeItem(item);
            cartRepository.save(cart);
        }
        return mapToCartResponse(cart);
    }

    public void clearCart(Long userId, String sessionId) {
        Cart cart = getCartEntity(userId, sessionId);
        cart.clear();
        cartRepository.save(cart);
    }

    // --- Private Methods ---

    // Trong CartService.java

    // Trong CartService.java

    public Cart getCartEntity(Long userId, String sessionId) {

        User user = null;
        if (userId != null) {
            // FIX 1: Tính toán User Entity VÀ KHAI BÁO CÁC BIẾN CẦN THIẾT LÀ final
            user = userRepository.findById(userId).orElse(null);
        }

        final Long finalUserId = userId;
        final String finalSessionId = sessionId;
        final User finalUser = user;

        Optional<Cart> userCartOpt = Optional.empty();
        Optional<Cart> sessionCartOpt = Optional.empty();

        if (finalUserId != null) {
            userCartOpt = cartRepository.findByUser_UserId(finalUserId);
        }

        if (finalSessionId != null) {
            List<Cart> sessionCarts = cartRepository.findBySessionId(finalSessionId);
            if (!sessionCarts.isEmpty()) {
                sessionCartOpt = Optional.of(sessionCarts.get(0));
                // Self-healing: Delete duplicates
                if (sessionCarts.size() > 1) {
                    for (int i = 1; i < sessionCarts.size(); i++) {
                        cartRepository.delete(sessionCarts.get(i));
                    }
                }
            }
        }

        // Trường hợp 1: User đang đăng nhập (có userId)
        if (finalUserId != null) {
            if (userCartOpt.isPresent()) {
                // Merge Cart
                if (sessionCartOpt.isPresent()
                        && !sessionCartOpt.get().getCartId().equals(userCartOpt.get().getCartId())) {
                    Cart userCart = userCartOpt.get();
                    Cart sessionCart = sessionCartOpt.get();

                    mergeCarts(userCart, sessionCart);

                    cartRepository.delete(sessionCart);
                    return userCart;
                }
                return userCartOpt.get();
            } else if (sessionCartOpt.isPresent()) {
                // Gán Session Cart đó cho User mới đăng nhập
                Cart cart = sessionCartOpt.get();
                cart.setUser(finalUser);
                cart.setSessionId(null);
                cartRepository.save(cart);
                return cart;
            }
        }

        // Trường hợp 2: Guest hoặc tạo mới

        // FIX 2: Tách logic tạo Cart mới (createCart) thành Supplier riêng biệt
        java.util.function.Supplier<Cart> cartSupplier = () -> {
            return createCart(finalUserId, finalSessionId, finalUser);
        };

        // Nếu không có User Cart, trả về Session Cart hoặc tạo Cart mới
        if (userCartOpt.isPresent()) {
            return userCartOpt.get();
        } else {
            return sessionCartOpt.orElseGet(cartSupplier);
        }
    }

    // Hàm createCart cần sửa lại để khớp với cách gọi
    private Cart createCart(Long userId, String sessionId, User user) {

        return cartRepository.save(Cart.builder()
                .user(user)
                .sessionId(sessionId)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());
    }

    // Logic Merge Cart (CartItem vs CartItem)
    private void mergeCarts(Cart userCart, Cart sessionCart) {
        for (CartItem sessionItem : sessionCart.getItems()) {
            Optional<CartItem> existingItemOpt = userCart.getItems().stream()
                    .filter(userItem -> userItem.getProduct().getProductId()
                            .equals(sessionItem.getProduct().getProductId()) &&
                            isStringingMatching(userItem, sessionItem))
                    .findFirst();

            if (existingItemOpt.isPresent()) {
                CartItem userItem = existingItemOpt.get();
                userItem.setQuantity(userItem.getQuantity() + sessionItem.getQuantity());
            } else {
                sessionItem.setCart(userCart);
                userCart.addItem(sessionItem);
                cartItemRepository.save(sessionItem);
            }
        }
    }

    // Helper so sánh cho Merge Cart (CartItem vs CartItem)
    private boolean isStringingMatching(CartItem item1, CartItem item2) {
        boolean tensionMatch = (item1.getTension() == null && item2.getTension() == null) ||
                (item1.getTension() != null && item2.getTension() != null &&
                        item1.getTension().compareTo(item2.getTension()) == 0);

        boolean notesMatch = (item1.getStringingNotes() == null && item2.getStringingNotes() == null) ||
                (item1.getStringingNotes() != null && item2.getStringingNotes() != null
                        && item1.getStringingNotes().equals(item2.getStringingNotes()));

        Long serviceId1 = item1.getStringingService() != null ? item1.getStringingService().getServiceId() : null;
        Long serviceId2 = item2.getStringingService() != null ? item2.getStringingService().getServiceId() : null;
        boolean serviceMatch = (serviceId1 == null && serviceId2 == null)
                || (serviceId1 != null && serviceId1.equals(serviceId2));

        Long stringId1 = item1.getStringProduct() != null ? item1.getStringProduct().getStringId() : null;
        Long stringId2 = item2.getStringProduct() != null ? item2.getStringProduct().getStringId() : null;
        boolean stringMatch = (stringId1 == null && stringId2 == null)
                || (stringId1 != null && stringId1.equals(stringId2));

        return tensionMatch && notesMatch && serviceMatch && stringMatch;
    }

    // Helper so sánh cho addToCart (CartItem vs CartItemRequest)
    private boolean isStringingMatching(CartItem item, CartItemRequest request) {
        boolean tensionMatch = (item.getTension() == null && request.getTension() == null) ||
                (item.getTension() != null && request.getTension() != null &&
                        item.getTension().compareTo(request.getTension()) == 0);

        boolean notesMatch = (item.getStringingNotes() == null && request.getStringingNotes() == null) ||
                (item.getStringingNotes() != null && request.getStringingNotes() != null
                        && item.getStringingNotes().equals(request.getStringingNotes()));

        Long itemServiceId = item.getStringingService() != null ? item.getStringingService().getServiceId() : null;
        boolean serviceIdMatch = (itemServiceId == null && request.getStringingServiceId() == null) ||
                (itemServiceId != null && itemServiceId.equals(request.getStringingServiceId()));

        Long itemStringId = item.getStringProduct() != null ? item.getStringProduct().getStringId() : null;
        boolean stringIdMatch = (itemStringId == null && request.getStringId() == null) ||
                (itemStringId != null && itemStringId.equals(request.getStringId()));

        return tensionMatch && notesMatch && serviceIdMatch && stringIdMatch;
    }

    private CartResponse mapToCartResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setCartId(cart.getCartId());
        response.setTotalItems(cart.getTotalItems());

        List<CartItemResponse> itemResponses = cart.getItems().stream().map(item -> {
            CartItemResponse itemResp = new CartItemResponse();
            itemResp.setCartItemId(item.getCartItemId());
            itemResp.setProductId(item.getProduct().getProductId());
            itemResp.setProductName(item.getProduct().getName());
            itemResp.setPrice(item.getPriceAtAdd());
            itemResp.setQuantity(item.getQuantity());

            itemResp.setTension(item.getTension());

            // Map tên dịch vụ
            if (item.getStringingService() != null) {
                // FIX: Đảm bảo kiểm tra item.getStringProduct() trước khi gọi .getName()
                String stringName = item.getStringProduct() != null ? item.getStringProduct().getName() : "N/A";
                itemResp.setStringingServiceName(item.getStringingService().getServiceName()
                        + " (" + stringName + " - " + item.getTension() + " lbs)");
            } else {
                itemResp.setStringingServiceName(null);
            }

            BigDecimal subtotal = item.getSubtotal();
            itemResp.setSubtotal(subtotal);
            return itemResp;
        }).collect(Collectors.toList());

        response.setItems(itemResponses);
        BigDecimal grandTotal = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalPrice(grandTotal);
        return response;
    }

    // 1. Logic thêm/cập nhật thông tin đan vợt cho 1 item trong giỏ
    public CartResponse updateStringingInfo(Long userId, String sessionId, Long cartItemId, StringingRequest request) {
        Cart cart = getCartEntity(userId, sessionId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ!"));

        // Kiểm tra quyền sở hữu giỏ hàng
        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa sản phẩm này!");
        }

        // Lấy Entity dịch vụ đan
        if (request.getStringingServiceId() != null) {
            com.badmintonshop.entity.StringingService service = stringingServiceRepository
                    .findById(request.getStringingServiceId())
                    .orElseThrow(() -> new RuntimeException("Dịch vụ đan không tồn tại!"));
            item.setStringingService(service);
        }

        // Lấy Entity loại cước
        if (request.getStringId() != null) {
            StringProduct string = stringProductRepository.findById(request.getStringId())
                    .orElseThrow(() -> new RuntimeException("Loại cước không tồn tại!"));
            item.setStringProduct(string);
        }

        item.setTension(request.getTension());
        item.setStringingNotes(request.getStringingNotes());

        cartItemRepository.save(item);
        return mapToCartResponse(cart);
    }

    // 2. Logic lấy tổng số lượng item để hiện badge icon giỏ hàng
    public Integer getCartCount(Long userId, String sessionId) {
        Cart cart = getCartEntity(userId, sessionId);
        if (cart.getItems() == null)
            return 0;
        return cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}