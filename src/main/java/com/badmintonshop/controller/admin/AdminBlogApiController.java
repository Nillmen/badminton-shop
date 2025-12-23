package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.blog.BlogPostDTO;
import com.badmintonshop.security.StaffUserDetails;
import com.badmintonshop.service.BlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin API for blog management
 */
@RestController
@RequestMapping("/admin/api/blogs")
@RequiredArgsConstructor
@Slf4j
public class AdminBlogApiController {

    private final BlogService blogService;

    /**
     * Get all blogs with pagination
     * GET /admin/api/blogs
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('blogs.view')")
    public ResponseEntity<Page<BlogPostDTO>> getAllBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<BlogPostDTO> blogs = blogService.getAllBlogsForAdmin(pageable);
        return ResponseEntity.ok(blogs);
    }

    /**
     * Get blog by ID
     * GET /admin/api/blogs/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('blogs.view')")
    public ResponseEntity<BlogPostDTO> getBlogById(@PathVariable Long id) {
        return blogService.getBlogById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new blog post
     * POST /admin/api/blogs
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('blogs.create')")
    public ResponseEntity<?> createBlog(
            @RequestBody BlogPostDTO dto,
            @AuthenticationPrincipal StaffUserDetails currentStaff) {

        try {
            BlogPostDTO created = blogService.createBlog(dto, currentStaff.getStaffId());
            log.info("Created blog: {}", created.getTitle());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update blog post
     * PUT /admin/api/blogs/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('blogs.update')")
    public ResponseEntity<?> updateBlog(
            @PathVariable Long id,
            @RequestBody BlogPostDTO dto) {

        try {
            BlogPostDTO updated = blogService.updateBlog(id, dto);
            log.info("Updated blog: {}", updated.getTitle());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete blog post (soft delete)
     * DELETE /admin/api/blogs/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('blogs.delete')")
    public ResponseEntity<?> deleteBlog(@PathVariable Long id) {
        try {
            blogService.deleteBlog(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa bài viết"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Publish blog post
     * PUT /admin/api/blogs/{id}/publish
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('blogs.update')")
    public ResponseEntity<?> publishBlog(@PathVariable Long id) {
        try {
            BlogPostDTO published = blogService.publishBlog(id);
            return ResponseEntity.ok(published);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
