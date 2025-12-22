package com.badmintonshop.service.stringing;

import com.badmintonshop.dto.StringDTO;
import com.badmintonshop.entity.StringProduct;
import com.badmintonshop.entity.enums.StringType;
import com.badmintonshop.repository.StringProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service("stringProductManagementService")
public class StringProductService {

    @Autowired
    private StringProductRepository repo;

    /* ================= READ ================= */

    public List<StringDTO> getAllActive() {
        return repo.findAllActiveWithBrand()
                .stream()
                .map(this::mapRowToDTO)
                .collect(Collectors.toList());
    }

    public StringDTO getById(Long id) {
        Object row = repo.findByIdWithBrand(id);
        if (row == null) {
            throw new RuntimeException("String not found with id: " + id);
        }
        return mapRowToDTO((Object[]) row);
    }

    public List<StringDTO> getByBrand(Long brandId) {
        return repo.findByBrandWithBrand(brandId)
                .stream()
                .map(this::mapRowToDTO)
                .collect(Collectors.toList());
    }

    /* ================= WRITE ================= */

    public StringDTO create(StringDTO dto) {
        StringProduct entity = toEntity(dto);
        entity.setIsActive(true);
        return toDTO(repo.save(entity));
    }

    public StringDTO update(Long id, StringDTO dto) {
        StringProduct existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("String not found"));

        existing.setName(dto.getName());
        existing.setSku(dto.getSku());
        existing.setDescription(dto.getDescription());
        existing.setStringType(dto.getStringType());
        existing.setGauge(dto.getGauge());
        existing.setMaterial(dto.getMaterial());
        existing.setDurabilityRating(dto.getDurabilityRating());
        existing.setRepulsionRating(dto.getRepulsionRating());
        existing.setControlRating(dto.getControlRating());
        existing.setHittingSoundRating(dto.getHittingSoundRating());
        existing.setRecommendedTensionMin(dto.getRecommendedTensionMin());
        existing.setRecommendedTensionMax(dto.getRecommendedTensionMax());
        existing.setRetailPrice(dto.getRetailPrice());
        existing.setLengthPerRoll(dto.getLengthPerRoll());
        existing.setQuantityInStock(dto.getQuantityInStock());
        existing.setColor(dto.getColor());
        existing.setImageUrl(dto.getImageUrl());

        return toDTO(repo.save(existing));
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    /* ================= MAPPER ================= */

    private StringDTO toDTO(StringProduct sp) {
        StringDTO dto = new StringDTO();
        dto.setStringId(sp.getStringId());
        dto.setName(sp.getName());
        dto.setSku(sp.getSku());
        dto.setDescription(sp.getDescription());
        dto.setStringType(sp.getStringType());
        dto.setGauge(sp.getGauge());
        dto.setMaterial(sp.getMaterial());
        dto.setDurabilityRating(sp.getDurabilityRating());
        dto.setRepulsionRating(sp.getRepulsionRating());
        dto.setControlRating(sp.getControlRating());
        dto.setHittingSoundRating(sp.getHittingSoundRating());
        dto.setRecommendedTensionMin(sp.getRecommendedTensionMin());
        dto.setRecommendedTensionMax(sp.getRecommendedTensionMax());
        dto.setRetailPrice(sp.getRetailPrice());
        dto.setLengthPerRoll(sp.getLengthPerRoll());
        dto.setQuantityInStock(sp.getQuantityInStock());
        dto.setColor(sp.getColor());
        dto.setImageUrl(sp.getImageUrl());
        dto.setIsActive(sp.getIsActive());

        // Brand chỉ đọc – KHÔNG query
        if (sp.getBrand() != null) {
            dto.setBrandId(sp.getBrand().getBrandId());
            dto.setBrandName(sp.getBrand().getName());
        }

        return dto;
    }

    private StringProduct toEntity(StringDTO dto) {
        return StringProduct.builder()
                .name(dto.getName())
                .sku(dto.getSku())
                .description(dto.getDescription())
                .stringType(dto.getStringType())
                .gauge(dto.getGauge())
                .material(dto.getMaterial())
                .durabilityRating(dto.getDurabilityRating())
                .repulsionRating(dto.getRepulsionRating())
                .controlRating(dto.getControlRating())
                .hittingSoundRating(dto.getHittingSoundRating())
                .recommendedTensionMin(dto.getRecommendedTensionMin())
                .recommendedTensionMax(dto.getRecommendedTensionMax())
                .retailPrice(dto.getRetailPrice())
                .lengthPerRoll(dto.getLengthPerRoll())
                .quantityInStock(dto.getQuantityInStock())
                .color(dto.getColor())
                .imageUrl(dto.getImageUrl())
                .build();
    }

    private StringDTO mapRowToDTO(Object[] r) {
        StringDTO dto = new StringDTO();

        dto.setStringId(((Number) r[0]).longValue());
        dto.setName((String) r[1]);
        dto.setSku((String) r[2]);
        dto.setDescription((String) r[3]);
        dto.setStringType(StringType.valueOf((String) r[4]));
        dto.setGauge((BigDecimal) r[5]);
        dto.setMaterial((String) r[6]);
        dto.setDurabilityRating((Integer) r[7]);
        dto.setRepulsionRating((Integer) r[8]);
        dto.setControlRating((Integer) r[9]);
        dto.setHittingSoundRating((Integer) r[10]);
        dto.setRecommendedTensionMin((BigDecimal) r[11]);
        dto.setRecommendedTensionMax((BigDecimal) r[12]);
        dto.setRetailPrice((BigDecimal) r[13]);
        dto.setLengthPerRoll((BigDecimal) r[14]);
        dto.setQuantityInStock((Integer) r[15]);
        dto.setColor((String) r[16]);
        dto.setImageUrl((String) r[17]);
        dto.setIsActive((Boolean) r[18]);

        dto.setBrandId(((Number) r[19]).longValue());
        dto.setBrandName((String) r[20]);

        return dto;
    }

}
