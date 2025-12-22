package com.badmintonshop.service;

import com.badmintonshop.dto.request.StringProductRequest;
import com.badmintonshop.dto.response.StringProductResponse;
import com.badmintonshop.entity.Brand;
import com.badmintonshop.entity.StringProduct;
import com.badmintonshop.exception.ResourceNotFoundException;
import com.badmintonshop.repository.BrandRepository;
import com.badmintonshop.repository.StringProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StringProductService {

    private final StringProductRepository stringProductRepository;
    private final BrandRepository brandRepository;

    public List<StringProductResponse> getAll() {
        return stringProductRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<StringProductResponse> getActiveStrings() {
        return stringProductRepository.findAll().stream()
                .filter(StringProduct::getIsActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public StringProductResponse getById(Long id) {
        StringProduct stringProduct = stringProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("String product not found with id: " + id));
        return mapToResponse(stringProduct);
    }

    @Transactional
    public StringProductResponse create(StringProductRequest request) {
        StringProduct stringProduct = mapToEntity(request);
        StringProduct saved = stringProductRepository.save(stringProduct);
        return mapToResponse(saved);
    }

    @Transactional
    public StringProductResponse update(Long id, StringProductRequest request) {
        StringProduct existing = stringProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("String product not found with id: " + id));

        updateEntity(existing, request);
        StringProduct saved = stringProductRepository.save(existing);
        return mapToResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!stringProductRepository.existsById(id)) {
            throw new ResourceNotFoundException("String product not found with id: " + id);
        }
        stringProductRepository.deleteById(id);
    }

    @Transactional
    public void toggleStatus(Long id) {
        StringProduct existing = stringProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("String product not found with id: " + id));
        existing.setIsActive(!existing.getIsActive());
        stringProductRepository.save(existing);
    }

    private StringProductResponse mapToResponse(StringProduct entity) {
        return StringProductResponse.builder()
                .stringId(entity.getStringId())
                .name(entity.getName())
                .sku(entity.getSku())
                .description(entity.getDescription())
                .brandId(entity.getBrand() != null ? entity.getBrand().getBrandId() : null)
                .brandName(entity.getBrand() != null ? entity.getBrand().getName() : null)
                .stringType(entity.getStringType())
                .gauge(entity.getGauge())
                .material(entity.getMaterial())
                .durabilityRating(entity.getDurabilityRating())
                .repulsionRating(entity.getRepulsionRating())
                .controlRating(entity.getControlRating())
                .hittingSoundRating(entity.getHittingSoundRating())
                .recommendedTensionMin(entity.getRecommendedTensionMin())
                .recommendedTensionMax(entity.getRecommendedTensionMax())
                .tensionRange(entity.getTensionRange())
                .retailPrice(entity.getRetailPrice())
                .lengthPerRoll(entity.getLengthPerRoll())
                .quantityInStock(entity.getQuantityInStock())
                .color(entity.getColor())
                .imageUrl(entity.getImageUrl())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private StringProduct mapToEntity(StringProductRequest request) {
        StringProduct.StringProductBuilder builder = StringProduct.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .stringType(request.getStringType())
                .gauge(request.getGauge())
                .material(request.getMaterial())
                .durabilityRating(request.getDurabilityRating())
                .repulsionRating(request.getRepulsionRating())
                .controlRating(request.getControlRating())
                .hittingSoundRating(request.getHittingSoundRating())
                .recommendedTensionMin(request.getRecommendedTensionMin())
                .recommendedTensionMax(request.getRecommendedTensionMax())
                .retailPrice(request.getRetailPrice())
                .lengthPerRoll(request.getLengthPerRoll())
                .quantityInStock(request.getQuantityInStock())
                .color(request.getColor())
                .imageUrl(request.getImageUrl())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true);

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));
            builder.brand(brand);
        }

        return builder.build();
    }

    private void updateEntity(StringProduct entity, StringProductRequest request) {
        entity.setName(request.getName());
        entity.setSku(request.getSku());
        entity.setDescription(request.getDescription());
        entity.setStringType(request.getStringType());
        entity.setGauge(request.getGauge());
        entity.setMaterial(request.getMaterial());
        entity.setDurabilityRating(request.getDurabilityRating());
        entity.setRepulsionRating(request.getRepulsionRating());
        entity.setControlRating(request.getControlRating());
        entity.setHittingSoundRating(request.getHittingSoundRating());
        entity.setRecommendedTensionMin(request.getRecommendedTensionMin());
        entity.setRecommendedTensionMax(request.getRecommendedTensionMax());
        entity.setRetailPrice(request.getRetailPrice());
        entity.setLengthPerRoll(request.getLengthPerRoll());
        entity.setQuantityInStock(request.getQuantityInStock());
        entity.setColor(request.getColor());
        entity.setImageUrl(request.getImageUrl());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));
            entity.setBrand(brand);
        } else {
            entity.setBrand(null);
        }
    }
}
