package com.badmintonshop.dto;

import com.badmintonshop.dto.cart.CartItemResponse;
import com.badmintonshop.dto.cart.CartResponse;
import com.badmintonshop.entity.Cart;
import com.badmintonshop.entity.CartItem;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class DTOMapper {

    public CartResponse toCartResponse(Cart cart) {
        return CartResponse.builder()
                .cartId(cart.getCartId())
                .items(cart.getItems().stream().map(this::toCartItemResponse).collect(Collectors.toList()))
                .build();
    }

    public CartItemResponse toCartItemResponse(CartItem item) {
        String variantName = item.getVariant() != null ? item.getVariant().getVariantName() : null;

        return CartItemResponse.builder()
                .cartItemId(item.getCartItemId())
                .productId(item.getProduct().getProductId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .priceAtAdd(item.getPriceAtAdd())
                .subtotal(item.getSubtotal())
                .variantName(variantName)
                .productImage(
                        item.getProduct().getPrimaryImage() != null ? item.getProduct().getPrimaryImage().getImageUrl()
                                : null)
                .build();
    }

    public com.badmintonshop.dto.order.OrderResponse toOrderResponse(com.badmintonshop.entity.Order order) {
        return toOrderResponse(order, null);
    }

    public com.badmintonshop.dto.order.OrderResponse toOrderResponse(com.badmintonshop.entity.Order order,
            String paymentUrl) {
        return com.badmintonshop.dto.order.OrderResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUser() != null ? order.getUser().getUserId() : null)
                .customerName(order.getUser() != null ? order.getUser().getFullName()
                        : "Guest (" + order.getShippingRecipientName() + ")")
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .shippingRecipientName(order.getShippingRecipientName())
                .shippingPhone(order.getShippingPhone())
                .shippingAddress(order.getFullShippingAddress())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream().map(this::toOrderItemResponse).collect(Collectors.toList()))
                .paymentUrl(paymentUrl)
                .build();
    }

    public com.badmintonshop.dto.order.OrderItemResponse toOrderItemResponse(com.badmintonshop.entity.OrderItem item) {
        String variantInfo = item.getVariant() != null ? item.getVariant().getVariantName() : "Standard";

        return com.badmintonshop.dto.order.OrderItemResponse.builder()
                .productName(item.getProduct().getName())
                .variantInfo(variantInfo)
                .quantity(item.getQuantity())
                .price(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}
