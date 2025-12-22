package com.badmintonshop.service.stringing;

import com.badmintonshop.dto.StringServiceDTO;
import com.badmintonshop.entity.StringingService;
import com.badmintonshop.entity.enums.StringingServiceType;
import com.badmintonshop.repository.StringingServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service("stringingServiceManagementService")
public class StringingServiceService {

    @Autowired
    private StringingServiceRepository repo;

    public List<StringServiceDTO> getAllActive() {
        return repo.findAllActive()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public StringServiceDTO create(StringServiceDTO dto) {
        StringingService entity = toEntity(dto);
        entity.setIsActive(true);
        return toDTO(repo.save(entity));
    }

    public StringServiceDTO update(Long id, StringServiceDTO dto) {
        StringingService existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        existing.setServiceName(dto.getServiceName());
        existing.setServiceType(dto.getServiceType());
        existing.setDescription(dto.getDescription());
        existing.setBasePrice(dto.getBasePrice());
        existing.setEstimatedTimeMinutes(dto.getEstimatedTimeMinutes());

        return toDTO(repo.save(existing));
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    private StringServiceDTO toDTO(StringingService s) {
        StringServiceDTO dto = new StringServiceDTO();
        dto.setServiceId(s.getServiceId());
        dto.setServiceName(s.getServiceName());
        dto.setServiceType(s.getServiceType());
        dto.setDescription(s.getDescription());
        dto.setBasePrice(s.getBasePrice());
        dto.setEstimatedTimeMinutes(s.getEstimatedTimeMinutes());
        dto.setIsActive(s.getIsActive());
        return dto;
    }

    private StringingService toEntity(StringServiceDTO dto) {
        return StringingService.builder()
                .serviceName(dto.getServiceName())
                .serviceType(dto.getServiceType())
                .description(dto.getDescription())
                .basePrice(dto.getBasePrice())
                .estimatedTimeMinutes(dto.getEstimatedTimeMinutes())
                .build();
    }

    public StringServiceDTO getById(Long id) {
        Object result = repo.findDtoById(id);

        if (result == null) {
            throw new RuntimeException("Stringing service not found with id: " + id);
        }

        Object[] row = (Object[]) result;

        StringServiceDTO dto = new StringServiceDTO();
        dto.setServiceId(((Number) row[0]).longValue());
        dto.setServiceName((String) row[1]);
        dto.setServiceType(
                StringingServiceType.valueOf((String) row[2]));
        dto.setDescription((String) row[3]);
        dto.setBasePrice((BigDecimal) row[4]);
        dto.setEstimatedTimeMinutes((Integer) row[5]);
        dto.setIsActive((Boolean) row[6]); // ✅ FIX Ở ĐÂY

        return dto;
    }

}
