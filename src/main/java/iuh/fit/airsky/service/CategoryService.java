package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.CategoryRequest;
import iuh.fit.airsky.dto.response.CategoryResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    Optional<CategoryResponse> findById(Long id);
    Optional<CategoryResponse> findBySlug(String slug);
    PageResponse<CategoryResponse> findAll(Pageable pageable);
    List<CategoryResponse> findAll();
    void delete(Long id);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);
}
