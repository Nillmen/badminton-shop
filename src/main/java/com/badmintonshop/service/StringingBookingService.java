package com.badmintonshop.service;

import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.OrderItem;
import com.badmintonshop.entity.Staff;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.StringingStatus;
import com.badmintonshop.exception.ResourceNotFoundException;
import com.badmintonshop.repository.OrderItemRepository;
import com.badmintonshop.repository.OrderRepository;
import com.badmintonshop.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StringingBookingService {

    private final OrderItemRepository orderItemRepository;
    private final StaffRepository staffRepository;
    private final OrderRepository orderRepository;

    public void assignStaff(Long orderItemId, Long staffId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order Item not found"));

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        if (!canAssignRole(staff)) {
            throw new IllegalArgumentException("Staff is not eligible for stringing");
        }

        if (isOverloaded(staff)) {
            throw new IllegalStateException("Staff is overloaded");
        }

        item.assignStringingStaff(staff);

        // Update Order status to STRINGING if not already
        Order order = item.getOrder();
        if (order.getStatus() != OrderStatus.STRINGING) {
            order.setStatus(OrderStatus.STRINGING);
            orderRepository.save(order);
        }

        orderItemRepository.save(item);
    }

    public void startStringing(Long orderItemId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order Item not found"));

        item.startStringing();
        orderItemRepository.save(item);
    }

    public void completeStringing(Long orderItemId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order Item not found"));

        item.completeStringing();
        orderItemRepository.save(item);
    }

    public void approveQuality(Long orderItemId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order Item not found"));

        item.setStringingStatus(StringingStatus.QUALITY_CHECKED);
        orderItemRepository.save(item);

        // Initializing items to prevent LazyInitializationException
        org.hibernate.Hibernate.initialize(item.getOrder().getItems());
        checkOrderReady(item.getOrder());
    }

    private void checkOrderReady(Order order) {
        boolean allStringingDone = order.getItems().stream()
                .filter(OrderItem::getHasStringingService)
                .allMatch(i -> i.getStringingStatus() == StringingStatus.QUALITY_CHECKED);

        if (allStringingDone) {
            order.setStatus(OrderStatus.READY_TO_SHIP);
            orderRepository.save(order);
        }
    }

    private boolean canAssignRole(Staff staff) {
        return staff.isStringingStaff();
    }

    public boolean isOverloaded(Staff staff) {
        if (staff.getDailyStringingCapacity() == null || staff.getDailyStringingCapacity() == 0) {
            return true;
        }
        long activeCount = getCurrentLoad(staff.getStaffId());
        return activeCount >= staff.getDailyStringingCapacity();
    }

    public long getCurrentLoad(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        return orderItemRepository.countByAssignedStaffAndStringingStatusIn(
                staff,
                List.of(StringingStatus.ASSIGNED, StringingStatus.IN_PROGRESS));
    }

    public java.util.List<com.badmintonshop.dto.staff.StaffWorkloadDTO> getStaffWorkload() {
        // Only get staff who can do stringing
        List<Staff> stringingStaff = staffRepository.findAll().stream()
                .filter(this::canAssignRole)
                .toList();

        return stringingStaff.stream().map(staff -> {
            long currentLoad = countByAssignedStaffAndStringingStatusIn(staff,
                    List.of(StringingStatus.ASSIGNED, StringingStatus.IN_PROGRESS));

            // For now, completed today count is mocked or requires complex query.
            // We'll leave it as 0 or implement a time-based query later if requested.
            int completedToday = 0;

            return com.badmintonshop.dto.staff.StaffWorkloadDTO.builder()
                    .staffId(staff.getStaffId())
                    .staffName(staff.getFullName())
                    .email(staff.getEmail())
                    .currentAssignedCount((int) currentLoad)
                    .completedTodayCount(completedToday)
                    .isOnline(true) // Mock online status
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }

    private long countByAssignedStaffAndStringingStatusIn(Staff staff, java.util.Collection<StringingStatus> statuses) {
        return orderItemRepository.countByAssignedStaffAndStringingStatusIn(staff, statuses);
    }
}
