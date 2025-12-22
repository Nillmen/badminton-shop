package com.badmintonshop.controller.staff;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Redirect staff stringing page to admin stringing page.
 * Staff sẽ sử dụng chung trang /admin/stringing với phân quyền hiển thị.
 */
@Controller
@RequestMapping("/staff/stringing")
public class StaffStringingController {

    @GetMapping
    public String redirectToAdminStringing() {
        return "redirect:/admin/stringing";
    }

    @GetMapping("/**")
    public String redirectAll() {
        return "redirect:/admin/stringing";
    }
}
