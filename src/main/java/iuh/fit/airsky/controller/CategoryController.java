package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.CategoryRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.CategoryResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.service.CategoryService;
import iuh.fit.airsky.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        try {
            CategoryResponse response = categoryService.createCategory(request);
            return ApiResponseUtil.buildResponse(true, "Tạo thể loại thành công", response, "/api/v1/categories");
        } catch (IllegalArgumentException e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/categories");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        try {
            CategoryResponse response = categoryService.updateCategory(id, request);
            return ApiResponseUtil.buildResponse(true, "Cập nhật thể loại thành công", response, "/api/v1/categories/" + id);
        } catch (IllegalArgumentException e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/categories/" + id);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(category -> ApiResponseUtil.buildResponse(true, "Lấy thông tin thể loại thành công", category, "/api/v1/categories/" + id))
                .orElse(ApiResponseUtil.buildResponse(false, "Không tìm thấy thể loại", null, "/api/v1/categories/" + id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        return categoryService.findBySlug(slug)
                .map(category -> ApiResponseUtil.buildResponse(true, "Lấy thông tin thể loại thành công", category, "/api/v1/categories/slug/" + slug))
                .orElse(ApiResponseUtil.buildResponse(false, "Không tìm thấy thể loại", null, "/api/v1/categories/slug/" + slug));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getAllCategories(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<CategoryResponse> response = categoryService.findAll(pageable);
        return ApiResponseUtil.buildResponse(true, "Lấy danh sách thể loại thành công", response, "/api/v1/categories");
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategoriesList() {
        List<CategoryResponse> response = categoryService.findAll();
        return ApiResponseUtil.buildResponse(true, "Lấy danh sách thể loại thành công", response, "/api/v1/categories/all");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.delete(id);
            return ApiResponseUtil.buildResponse(true, "Xóa thể loại thành công", null, "/api/v1/categories/" + id);
        } catch (IllegalArgumentException e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/categories/" + id);
        }
    }

    @GetMapping("/check-slug/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkSlugExists(@PathVariable String slug) {
        boolean exists = categoryService.existsBySlug(slug);
        return ApiResponseUtil.buildResponse(true, "Kiểm tra slug thành công", exists, "/api/v1/categories/check-slug/" + slug);
    }

    @GetMapping("/check-name/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkNameExists(@PathVariable String name) {
        boolean exists = categoryService.existsByName(name);
        return ApiResponseUtil.buildResponse(true, "Kiểm tra tên thành công", exists, "/api/v1/categories/check-name/" + name);
    }
}
