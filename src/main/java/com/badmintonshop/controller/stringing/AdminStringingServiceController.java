package com.badmintonshop.controller.stringing;

import com.badmintonshop.dto.request.StringingServiceRequest;
import com.badmintonshop.dto.response.ApiResponse;
import com.badmintonshop.dto.response.StringingServiceResponse;
import com.badmintonshop.service.StringingServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/stringing-services")
@RequiredArgsConstructor
public class AdminStringingServiceController {

    private final StringingServiceService stringingServiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StringingServiceResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(stringingServiceService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StringingServiceResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(stringingServiceService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StringingServiceResponse>> create(
            @Valid @RequestBody StringingServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stringingServiceService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StringingServiceResponse>> update(@PathVariable Long id,
            @Valid @RequestBody StringingServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stringingServiceService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        stringingServiceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted successfully", null));
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id) {
        stringingServiceService.toggleStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", null));
    }
}
