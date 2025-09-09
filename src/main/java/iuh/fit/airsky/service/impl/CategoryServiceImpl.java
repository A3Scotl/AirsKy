package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.CategoryRequest;
import iuh.fit.airsky.dto.response.CategoryResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.CategoryMapper;
import iuh.fit.airsky.model.Category;
import iuh.fit.airsky.repository.CategoryRepository;
import iuh.fit.airsky.service.CategoryService;
import iuh.fit.airsky.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating new category with name: {}", request.getName());
        
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Category với tên này đã tồn tại: " + request.getName());
        }
        
        // Tự động tạo slug từ name
        String slug = SlugUtils.generateUniqueSlug(request.getName(), 
            (candidateSlug) -> categoryRepository.existsBySlug(candidateSlug));
        
        Category category = categoryMapper.toEntity(request);
        category.setName(request.getName());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setActive(request.getActive() != null ? request.getActive() : true);
        
        Category savedCategory = categoryRepository.save(category);
        
        log.info("Category created successfully with ID: {}", savedCategory.getCategoryId());
        return categoryMapper.toResponseDTO(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category with ID: {}", id);
        
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại với ID: " + id));
        
        // Check if name exists for other categories
        if (!existingCategory.getName().equals(request.getName()) && 
            categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Category với tên này đã tồn tại: " + request.getName());
        }
        
        // Nếu name thay đổi, tự động tạo slug mới
        String newSlug = existingCategory.getSlug();
        if (!existingCategory.getName().equals(request.getName())) {
            newSlug = SlugUtils.generateUniqueSlug(request.getName(), 
                (candidateSlug) -> categoryRepository.existsBySlug(candidateSlug));
        }
        
        existingCategory.setName(request.getName());
        existingCategory.setSlug(newSlug);
        existingCategory.setDescription(request.getDescription());

        if (request.getActive() != null) {
            existingCategory.setActive(request.getActive());
        }
        
        Category updatedCategory = categoryRepository.save(existingCategory);
        
        log.info("Category updated successfully with ID: {}", updatedCategory.getCategoryId());
        return categoryMapper.toResponseDTO(updatedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryResponse> findById(Long id) {
        log.debug("Finding category by ID: {}", id);
        return categoryRepository.findById(id)
                .map(categoryMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryResponse> findBySlug(String slug) {
        log.debug("Finding category by slug: {}", slug);
        return categoryRepository.findBySlug(slug)
                .map(categoryMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> findAll(Pageable pageable) {
        log.debug("Finding all categories with pagination");
        Page<Category> categoryPage = categoryRepository.findAll(pageable);
        
        List<CategoryResponse> content = categoryPage.getContent().stream()
                .map(categoryMapper::toResponseDTO)
                .collect(Collectors.toList());
        
        return new PageResponse<>(
                content,
                categoryPage.getNumber(),
                categoryPage.getSize(),
                categoryPage.getTotalElements(),
                categoryPage.getTotalPages(),
                categoryPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        log.debug("Finding all categories");
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting category with ID: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại với ID: " + id));
        
        // Check if category has any blogs
        if (category.getBlogs() != null && !category.getBlogs().isEmpty()) {
            throw new IllegalArgumentException("Không thể xóa category vì còn có blog liên kết");
        }
        
        categoryRepository.delete(category);
        log.info("Category deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return categoryRepository.existsBySlug(slug);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }
}
