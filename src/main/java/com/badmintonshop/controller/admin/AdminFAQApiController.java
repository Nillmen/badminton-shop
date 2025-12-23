package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.faq.FAQDTO;
import com.badmintonshop.service.FAQService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin API for FAQ management
 */
@RestController
@RequestMapping("/admin/api/faq")
@RequiredArgsConstructor
@Slf4j
public class AdminFAQApiController {

    private final FAQService faqService;

    /**
     * Get all FAQs with pagination
     * GET /admin/api/faq
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('faqs.view')")
    public ResponseEntity<Page<FAQDTO>> getAllFAQs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<FAQDTO> faqs = faqService.getAllFAQsForAdmin(pageable);
        return ResponseEntity.ok(faqs);
    }

    /**
     * Get FAQ by ID
     * GET /admin/api/faq/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('faqs.view')")
    public ResponseEntity<FAQDTO> getFAQById(@PathVariable Long id) {
        return faqService.getFAQById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new FAQ
     * POST /admin/api/faq
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('faqs.create')")
    public ResponseEntity<?> createFAQ(@RequestBody FAQDTO dto) {
        try {
            FAQDTO created = faqService.createFAQ(dto);
            log.info("Created FAQ: {}", created.getQuestion());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update FAQ
     * PUT /admin/api/faq/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('faqs.update')")
    public ResponseEntity<?> updateFAQ(
            @PathVariable Long id,
            @RequestBody FAQDTO dto) {

        try {
            FAQDTO updated = faqService.updateFAQ(id, dto);
            log.info("Updated FAQ: {}", updated.getQuestion());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete FAQ (soft delete)
     * DELETE /admin/api/faq/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('faqs.delete')")
    public ResponseEntity<?> deleteFAQ(@PathVariable Long id) {
        try {
            faqService.deleteFAQ(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa FAQ"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Toggle FAQ active status
     * PUT /admin/api/faq/{id}/toggle-active
     */
    @PutMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('faqs.update')")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        try {
            FAQDTO faq = faqService.toggleActive(id);
            return ResponseEntity.ok(faq);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
