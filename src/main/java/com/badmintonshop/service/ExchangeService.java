package com.badmintonshop.service;

import com.badmintonshop.dto.request.ExchangeRequest;
import com.badmintonshop.dto.response.ExchangeResponse;
import com.badmintonshop.entity.Exchange;
import com.badmintonshop.entity.OrderItem;
import com.badmintonshop.entity.ProductVariant;
import com.badmintonshop.entity.enums.ExchangeStatus;
import com.badmintonshop.repository.ExchangeRepository;
import com.badmintonshop.repository.OrderItemRepository;
import com.badmintonshop.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeService {

    private final ExchangeRepository exchangeRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;

    public List<ExchangeResponse> getAllExchanges() {
        return exchangeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ExchangeResponse getExchangeById(Long id) {
        return exchangeRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Exchange not found"));
    }

    public List<ExchangeResponse> getExchangesByUserId(Long userId) {
        return exchangeRepository.findByOrder_User_UserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExchangeResponse createExchange(ExchangeRequest request) {
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new IllegalArgumentException("Order Item not found"));

        if (exchangeRepository.existsByOrderItem_OrderItemId(orderItem.getOrderItemId())) {
            throw new IllegalArgumentException("This item has already been requested for exchange");
        }

        Exchange exchange = new Exchange();
        exchange.setOrder(orderItem.getOrder());
        exchange.setOrderItem(orderItem);
        exchange.setOldVariant(orderItem.getVariant());
        exchange.setExchangeNumber("EX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        exchange.setReason(request.getReason());
        exchange.setDescription(request.getDescription());
        // Only set images if provided, otherwise keep default empty JSON array
        if (request.getImages() != null && !request.getImages().isBlank()) {
            exchange.setImages(request.getImages());
        } else {
            exchange.setImages("[]");
        }
        exchange.setPickupAddress(request.getPickupAddress());
        exchange.setStatus(ExchangeStatus.REQUESTED);

        if (request.getNewVariantId() != null) {
            ProductVariant newVariant = productVariantRepository.findById(request.getNewVariantId())
                    .orElseThrow(() -> new IllegalArgumentException("New Variant not found"));
            exchange.setNewVariant(newVariant);
        }

        Exchange saved = exchangeRepository.save(exchange);

        // Update Order Item status handled dynamically in Controller
        // orderItemRepository.save(orderItem);

        return mapToResponse(saved);
    }

    @Transactional
    public ExchangeResponse updateStatus(Long id, ExchangeStatus status, String notes) {
        Exchange exchange = exchangeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exchange not found"));

        if (status == ExchangeStatus.APPROVED) {
            exchange.approve();
            if (notes != null)
                exchange.setAdminNotes(notes);
        } else if (status == ExchangeStatus.REJECTED) {
            exchange.reject(notes);
        } else if (status == ExchangeStatus.COMPLETED) {
            exchange.complete();
        } else {
            exchange.setStatus(status);
        }

        return mapToResponse(exchangeRepository.save(exchange));
    }

    private ExchangeResponse mapToResponse(Exchange exchange) {
        return ExchangeResponse.builder()
                .exchangeId(exchange.getExchangeId())
                .exchangeNumber(exchange.getExchangeNumber())
                .orderId(exchange.getOrder().getOrderId())
                .orderNumber(exchange.getOrder().getOrderNumber())
                .oldProductName(exchange.getOrderItem().getProduct().getName())
                .oldVariantName(exchange.getOldVariant() != null ? exchange.getOldVariant().getSku() : "N/A")
                .newProductName(
                        exchange.getNewVariant() != null ? exchange.getNewVariant().getProduct().getName() : null)
                .newVariantName(exchange.getNewVariant() != null ? exchange.getNewVariant().getSku() : null)
                .status(exchange.getStatus())
                .reason(exchange.getReason())
                .description(exchange.getDescription())
                .createdAt(exchange.getCreatedAt())
                .pickupScheduledAt(exchange.getPickupScheduledAt())
                .build();
    }
}
