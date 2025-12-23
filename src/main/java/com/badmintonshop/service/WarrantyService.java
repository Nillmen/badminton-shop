package com.badmintonshop.service;

import com.badmintonshop.dto.request.WarrantyRequest;
import com.badmintonshop.dto.response.WarrantyResponse;
import com.badmintonshop.entity.OrderItem;
import com.badmintonshop.entity.Warranty;
import com.badmintonshop.entity.enums.WarrantyStatus;
import com.badmintonshop.repository.OrderItemRepository;
import com.badmintonshop.repository.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarrantyService {

    private final WarrantyRepository warrantyRepository;
    private final OrderItemRepository orderItemRepository;

    public List<WarrantyResponse> getAllWarranties() {
        return warrantyRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public WarrantyResponse getWarrantyById(Long id) {
        return warrantyRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Warranty not found"));
    }

    public List<WarrantyResponse> getWarrantiesByUserId(Long userId) {
        return warrantyRepository.findByOrder_User_UserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WarrantyResponse createWarranty(WarrantyRequest request) {
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new IllegalArgumentException("Order Item not found"));

        Warranty warranty = new Warranty();
        warranty.setOrder(orderItem.getOrder());
        warranty.setOrderItem(orderItem);
        warranty.setProduct(orderItem.getProduct());
        warranty.setWarrantyNumber("WAR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        warranty.setIssueType(request.getIssueType());
        warranty.setDescription(request.getDescription());
        // Only set images if provided, otherwise set default empty JSON array
        if (request.getImages() != null && !request.getImages().isBlank()) {
            warranty.setImages(request.getImages());
        } else {
            warranty.setImages("[]");
        }
        warranty.setStatus(WarrantyStatus.REQUESTED);

        // Set warranty expiry logic (simplified, e.g., 3 months from order)
        warranty.setPurchaseDate(orderItem.getOrder().getCreatedAt().toLocalDate());
        warranty.setWarrantyExpiryDate(warranty.getPurchaseDate().plusMonths(3));
        warranty.checkWarrantyValidity();

        // No need to update OrderItem status as it is now @Transient and calculated
        // dynamically

        return mapToResponse(warrantyRepository.save(warranty));
    }

    @Transactional
    public WarrantyResponse updateStatus(Long id, WarrantyStatus status, String notes) {
        Warranty warranty = warrantyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warranty not found"));

        if (status == WarrantyStatus.APPROVED) {
            warranty.approve();
        } else if (status == WarrantyStatus.REJECTED) {
            warranty.reject(notes);
        } else if (status == WarrantyStatus.SENT_TO_MANUFACTURER) {
            warranty.sendToManufacturer(notes); // using notes as ref number for simplicity
        } else {
            warranty.setStatus(status);
        }

        return mapToResponse(warrantyRepository.save(warranty));
    }

    private WarrantyResponse mapToResponse(Warranty warranty) {
        return WarrantyResponse.builder()
                .warrantyId(warranty.getWarrantyId())
                .warrantyNumber(warranty.getWarrantyNumber())
                .orderId(warranty.getOrder().getOrderId())
                .orderNumber(warranty.getOrder().getOrderNumber())
                .productName(warranty.getProduct().getName())
                // .variantName(warranty.getOrderItem().getProductVariant().getSku()) // Needs
                // checks
                .status(warranty.getStatus())
                .issueType(warranty.getIssueType())
                .description(warranty.getDescription())
                .purchaseDate(warranty.getPurchaseDate())
                .warrantyExpiryDate(warranty.getWarrantyExpiryDate())
                .createdAt(warranty.getCreatedAt())
                .build();
    }
}
