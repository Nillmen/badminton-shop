package com.badmintonshop.controller.stringing;

import com.badmintonshop.dto.request.StringProductRequest;
import com.badmintonshop.dto.response.ApiResponse;
import com.badmintonshop.dto.response.StringProductResponse;
import com.badmintonshop.service.StringProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/strings")
@RequiredArgsConstructor
public class AdminStringProductController {

    private final StringProductService stringProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StringProductResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(stringProductService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StringProductResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(stringProductService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StringProductResponse>> create(@Valid @RequestBody StringProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stringProductService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StringProductResponse>> update(@PathVariable Long id,
            @Valid @RequestBody StringProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stringProductService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        stringProductService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted successfully", null));
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id) {
        stringProductService.toggleStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", null));
    }
}
