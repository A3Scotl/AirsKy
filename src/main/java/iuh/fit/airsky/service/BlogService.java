package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.BlogRequest;
import iuh.fit.airsky.dto.response.BlogResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BlogService {
    BlogResponse createBlog(BlogRequest request, Long authorId);
    BlogResponse updateBlog(Long id, BlogRequest request, Long authorId);
    Optional<BlogResponse> findById(Long id);
    Optional<BlogResponse> findBySlug(String slug);
    Optional<BlogResponse> findBySlugAndPublished(String slug);
    PageResponse<BlogResponse> findAll(Pageable pageable);
    PageResponse<BlogResponse> findAllPublished(Pageable pageable);
    PageResponse<BlogResponse> findByAuthor(Long authorId, Pageable pageable);
    PageResponse<BlogResponse> findByAuthorAndPublished(Long authorId, Pageable pageable);
    PageResponse<BlogResponse> findByCategoryAndPublished(String categorySlug, Pageable pageable);
    PageResponse<BlogResponse> searchByKeyword(String keyword, Pageable pageable);
    void delete(Long id);
    void publish(Long id);
    void unpublish(Long id);
    void incrementViewCount(Long id);
    boolean existsBySlug(String slug);
    void saveBlog(Long blogId, Long userId);
    void unsaveBlog(Long blogId, Long userId);
    PageResponse<BlogResponse> getSavedBlogs(Long userId, Pageable pageable);
}
