package com.badmintonshop.controller.stringing;

import com.badmintonshop.dto.StringServiceDTO;
import com.badmintonshop.entity.StringingService;
import com.badmintonshop.service.stringing.StringingServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/stringing-services")
public class AdminStringingServiceController {

    @Autowired
    private StringingServiceService service;

    @GetMapping
    public java.util.List<StringServiceDTO> getAll() {
        return service.getAll();
    }

    @PostMapping
    public StringServiceDTO create(@RequestBody StringServiceDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public StringServiceDTO update(
            @PathVariable Long id,
            @RequestBody StringServiceDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
