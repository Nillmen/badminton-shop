package com.badmintonshop.service;

import com.badmintonshop.entity.*;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.StringingStatus;
import com.badmintonshop.repository.OrderItemRepository;
import com.badmintonshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;

    public Order createOrderFromCart(User user, String sessionId, String address, String note) {
        Cart cart = cartService.getCart(user, sessionId);
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setStatus(OrderStatus.PENDING); // Assuming payment handled separately or this is COD
        if (user != null) {
            order.setShippingRecipientName(user.getFullName() != null ? user.getFullName() : "Guest");
            order.setShippingPhone(user.getPhone() != null ? user.getPhone() : "0000000000");
        } else {
            order.setShippingRecipientName("Guest");
            order.setShippingPhone("0000000000");
        }
        order.setShippingAddress(address != null ? address : "No Address");
        order.setShippingCity("Ho Chi Minh"); // Default
        order.setShippingDistrict("District 1"); // Default
        order.setPaymentMethod(com.badmintonshop.entity.enums.PaymentMethod.COD);

        order.setCustomerNotes(note);
        order.setSubtotal(BigDecimal.ZERO); // Will be calculated as sum of item subtotals
        order.setTotalAmount(BigDecimal.ZERO); // Will calculate

        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setVariant(cartItem.getVariant());
            orderItem.setProductName(cartItem.getProduct().getName());
            orderItem.setProductSku(cartItem.getProduct().getSku()); // Simplified
            if (cartItem.getVariant() != null) {
                orderItem.setVariantName(cartItem.getVariant().getVariantName());
            }
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getPriceAtAdd());
            orderItem.setSubtotal(cartItem.getSubtotal());

            // Stringing Info transfer
            if (cartItem.hasStringingService()) {
                orderItem.setHasStringingService(true);
                orderItem.setStringingService(cartItem.getStringingService());
                orderItem.setStringingServiceName(cartItem.getStringingService().getServiceName());
                orderItem.setStringingServicePrice(cartItem.getStringingService().getBasePrice());

                if (cartItem.getStringProduct() != null) {
                    orderItem.setStringProduct(cartItem.getStringProduct());
                    orderItem.setStringName(cartItem.getStringProduct().getName());
                    orderItem.setStringPrice(cartItem.getStringProduct().getRetailPrice());
                }

                orderItem.setTension(cartItem.getTension());
                orderItem.setStringingNotes(cartItem.getStringingNotes());
                orderItem.setStringingStatus(StringingStatus.PENDING); // Waiting for admin
            }

            total = total.add(orderItem.getTotalPrice());
            orderItemRepository.save(orderItem);
        }

        order.setSubtotal(total);
        order.setTotalAmount(total);
        orderRepository.save(order);

        // Clear cart
        cartService.clearCart(user, sessionId);

        return order;
    }

    public org.springframework.data.domain.Page<Order> getUserOrders(User user, int page, int size) {
        return orderRepository.findByUser(user, org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending()));
    }

    public Order getOrderForUser(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to view this order");
        }
        return order;
    }
}
