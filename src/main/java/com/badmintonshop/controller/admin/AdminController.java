package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.product.BrandDTO;
import com.badmintonshop.dto.product.CategoryDTO;
import com.badmintonshop.dto.product.CategoryTreeDTO;
import com.badmintonshop.dto.product.ProductListDTO;
import com.badmintonshop.entity.enums.BrandStatus;
import com.badmintonshop.entity.enums.ProductStatus;
import com.badmintonshop.entity.enums.ProductType;
import com.badmintonshop.service.BrandService;
import com.badmintonshop.service.CategoryService;
import com.badmintonshop.service.InventoryService;
import com.badmintonshop.service.ProductService;
import com.badmintonshop.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for page routing
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final ProductService productService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final InventoryService inventoryService;
    private final SupplierService supplierService;

    /**
     * Admin login page
     */
    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    /**
     * Admin dashboard
     */
    @GetMapping({ "/", "/dashboard" })
    public String dashboard(Model model) {
        log.info("Loading admin dashboard");

        // TODO: Add real stats from services
        model.addAttribute("totalOrders", 0);
        model.addAttribute("totalRevenue", 0);
        model.addAttribute("totalCustomers", 0);
        model.addAttribute("pendingStringing", 0);

        return "admin/dashboard";
    }

    /**
     * Access denied page
     */
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }

    /**
     * Products management page
     */
    @GetMapping("/products")
    public String productsPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        log.info("Loading admin products page");

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProductListDTO> products = productService.searchProductsAdvanced(
                keyword, categoryId, brandId, null, null, null, status, null, pageable);

        List<CategoryTreeDTO> categories = categoryService.getCategoryTree();
        List<BrandDTO> brands = brandService.getAllActiveBrands();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("brands", brands);

        return "admin/products";
    }

    /**
     * Brands management page
     */
    @GetMapping("/brands")
    public String brandsPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BrandStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        log.info("Loading admin brands page");

        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());
        Page<BrandDTO> brands = brandService.searchBrands(keyword, status, pageable);

        model.addAttribute("brands", brands);

        return "admin/brands";
    }

    /**
     * Categories management page
     */
    @GetMapping("/categories")
    public String categoriesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        log.info("Loading admin categories page");

        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());
        Page<CategoryDTO> categories = categoryService.getAllCategories(pageable);
        List<CategoryDTO> allCategories = categoryService.getAllActiveCategories();

        // Create parent name map for display
        Map<Long, String> parentNames = new HashMap<>();
        for (CategoryDTO cat : allCategories) {
            parentNames.put(cat.getCategoryId(), cat.getName());
        }

        model.addAttribute("categories", categories);
        model.addAttribute("allCategories", allCategories);
        model.addAttribute("parentNames", parentNames);

        return "admin/categories";
    }

    /**
     * System Settings page
     */
    @GetMapping("/settings")
    public String settingsPage() {
        log.info("Loading admin settings page");
        return "admin/settings";
    }

    /**
     * Email Templates page
     */
    @GetMapping("/email-templates")
    public String emailTemplatesPage() {
        log.info("Loading admin email templates page");
        return "admin/email-templates";
    }

    /**
     * Activity Logs page
     */
    @GetMapping("/activity-logs")
    public String activityLogsPage() {
        log.info("Loading admin activity logs page");
        return "admin/activity-logs";
    }

    /**
     * Inventory management page
     */
    @GetMapping("/inventory")
    public String inventoryPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        log.info("Loading admin inventory page - keyword: {}, stockStatus: {}", keyword, stockStatus);

        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        var inventoryPage = inventoryService.searchInventoryWithKeyword(keyword, stockStatus, pageable);
        var stats = inventoryService.getInventoryStats();

        model.addAttribute("inventory", inventoryPage);
        model.addAttribute("stats", stats);

        return "admin/inventory";
    }

    /**
     * Suppliers management page
     */
    @GetMapping("/suppliers")
    public String suppliersPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        log.info("Loading admin suppliers page");

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        var suppliersPage = supplierService.searchSuppliers(keyword, pageable);

        model.addAttribute("suppliers", suppliersPage);

        return "admin/suppliers";
    }

    /**
     * Staff Management page
     */
    @GetMapping("/staff")
    public String staffPage() {
        log.info("Loading admin staff management page");
        return "admin/staff";
    }

    /**
     * Users (Customers) Management page
     * Accessible via /admin/users or /admin/customers
     */
    @GetMapping({ "/users", "/customers" })
    public String usersPage() {
        log.info("Loading admin users management page");
        return "admin/users";
    }

    /**
     * Product detail/edit page
     */
    @GetMapping("/products/{id}")
    public String productDetailPage(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        log.info("Loading admin product detail page for id: {}", id);
        model.addAttribute("productId", id);
        model.addAttribute("categories", categoryService.getCategoryTree());
        model.addAttribute("brands", brandService.getAllActiveBrands());
        return "admin/product-detail";
    }

    /**
     * Product create page (new product)
     */
    @GetMapping("/products/new")
    public String productNewPage(Model model) {
        log.info("Loading admin new product page");
        model.addAttribute("productId", null);
        model.addAttribute("categories", categoryService.getCategoryTree());
        model.addAttribute("brands", brandService.getAllActiveBrands());
        return "admin/product-detail";
    }

    /**
     * Trash / Recycle Bin page
     */
    @GetMapping("/trash")
    public String trashPage() {
        log.info("Loading admin trash page");
        return "admin/trash";
    }
}
