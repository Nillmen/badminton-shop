package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.review.ReviewResponse;
import com.badmintonshop.entity.enums.ReviewStatus;
import com.badmintonshop.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin Controller for Review Moderation
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SALE_STAFF')")
public class AdminReviewController {

    private final ReviewService reviewService;

    /**
     * Reviews management page
     * GET /admin/reviews
     */
    @GetMapping("/reviews")
    public String reviewsPage(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponse> reviews;
        
        try {
            ReviewStatus reviewStatus = ReviewStatus.valueOf(status.toUpperCase());
            reviews = reviewService.getReviewsByStatus(reviewStatus, pageable);
        } catch (IllegalArgumentException e) {
            reviews = reviewService.getPendingReviews(pageable);
        }
        
        model.addAttribute("reviews", reviews);
        model.addAttribute("currentStatus", status);
        model.addAttribute("statuses", ReviewStatus.values());
        
        return "admin/reviews";
    }

    /**
     * Approve a review
     * POST /admin/api/reviews/{reviewId}/approve
     */
    @PostMapping("/api/reviews/{reviewId}/approve")
    @ResponseBody
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('content.edit')")
    public ResponseEntity<Map<String, Object>> approveReview(@PathVariable Long reviewId) {
        try {
            reviewService.approveReview(reviewId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã duyệt đánh giá thành công"
            ));
        } catch (Exception e) {
            log.error("Error approving review {}", reviewId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Reject a review
     * POST /admin/api/reviews/{reviewId}/reject
     */
    @PostMapping("/api/reviews/{reviewId}/reject")
    @ResponseBody
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('content.edit')")
    public ResponseEntity<Map<String, Object>> rejectReview(
            @PathVariable Long reviewId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            reviewService.rejectReview(reviewId, reason);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã từ chối đánh giá"
            ));
        } catch (Exception e) {
            log.error("Error rejecting review {}", reviewId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Delete a review (admin)
     * DELETE /admin/api/reviews/{reviewId}
     */
    @DeleteMapping("/api/reviews/{reviewId}")
    @ResponseBody
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('content.delete')")
    public ResponseEntity<Map<String, Object>> deleteReview(@PathVariable Long reviewId) {
        try {
            reviewService.adminDeleteReview(reviewId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xóa đánh giá"
            ));
        } catch (Exception e) {
            log.error("Error deleting review {}", reviewId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get review statistics
     * GET /admin/api/reviews/stats
     */
    @GetMapping("/api/reviews/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReviewStats() {
        return ResponseEntity.ok(reviewService.getReviewStats());
    }
}
