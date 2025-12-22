package com.badmintonshop.service;

import com.badmintonshop.dto.request.StringingServiceRequest;
import com.badmintonshop.dto.response.StringingServiceResponse;
import com.badmintonshop.entity.StringingService;
import com.badmintonshop.exception.ResourceNotFoundException;
import com.badmintonshop.repository.StringingServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StringingServiceService {

    private final StringingServiceRepository stringingServiceRepository;

    public List<StringingServiceResponse> getAll() {
        return stringingServiceRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<StringingServiceResponse> getActiveServices() {
        return stringingServiceRepository.findAll().stream()
                .filter(StringingService::getIsActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public StringingServiceResponse getById(Long id) {
        StringingService service = stringingServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stringing service not found with id: " + id));
        return mapToResponse(service);
    }

    @Transactional
    public StringingServiceResponse create(StringingServiceRequest request) {
        StringingService service = mapToEntity(request);
        StringingService saved = stringingServiceRepository.save(service);
        return mapToResponse(saved);
    }

    @Transactional
    public StringingServiceResponse update(Long id, StringingServiceRequest request) {
        StringingService existing = stringingServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stringing service not found with id: " + id));

        updateEntity(existing, request);
        StringingService saved = stringingServiceRepository.save(existing);
        return mapToResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!stringingServiceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Stringing service not found with id: " + id);
        }
        stringingServiceRepository.deleteById(id);
    }

    @Transactional
    public void toggleStatus(Long id) {
        StringingService existing = stringingServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stringing service not found with id: " + id));
        existing.setIsActive(!existing.getIsActive());
        stringingServiceRepository.save(existing);
    }

    private StringingServiceResponse mapToResponse(StringingService entity) {
        return StringingServiceResponse.builder()
                .serviceId(entity.getServiceId())
                .serviceName(entity.getServiceName())
                .serviceType(entity.getServiceType())
                .description(entity.getDescription())
                .basePrice(entity.getBasePrice())
                .estimatedTimeMinutes(entity.getEstimatedTimeMinutes())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private StringingService mapToEntity(StringingServiceRequest request) {
        return StringingService.builder()
                .serviceName(request.getServiceName())
                .serviceType(request.getServiceType())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .estimatedTimeMinutes(request.getEstimatedTimeMinutes())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
    }

    private void updateEntity(StringingService entity, StringingServiceRequest request) {
        entity.setServiceName(request.getServiceName());
        entity.setServiceType(request.getServiceType());
        entity.setDescription(request.getDescription());
        entity.setBasePrice(request.getBasePrice());
        entity.setEstimatedTimeMinutes(request.getEstimatedTimeMinutes());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }
}
