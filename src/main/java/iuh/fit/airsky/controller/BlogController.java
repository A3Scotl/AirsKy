package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.BlogRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.BlogResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.service.BlogService;
import iuh.fit.airsky.service.BlogLikeService;
import iuh.fit.airsky.service.CloudinaryService;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final BlogLikeService blogLikeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> createBlog(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "excerpt", required = false) String excerpt,
            @RequestParam(value = "featuredImageFile", required = false) MultipartFile featuredImageFile,
            @RequestParam(value = "featuredImage", required = false) String featuredImage,
            @RequestParam(value = "isPublished", defaultValue = "false") Boolean isPublished,
            @RequestParam("categoryIds") String categoryIdsStr) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long authorId = getUserIdFromAuthentication(auth);
            
            // Validate required fields
            if (title == null || title.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Tiêu đề không được để trống", null, "/api/v1/blogs");
            }
            if (content == null || content.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Nội dung không được để trống", null, "/api/v1/blogs");
            }
            if (categoryIdsStr == null || categoryIdsStr.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Phải chọn ít nhất 1 danh mục", null, "/api/v1/blogs");
            }
            
            // Build BlogRequest object
            BlogRequest request = new BlogRequest();
            request.setTitle(title.trim());
            request.setContent(content);
            request.setExcerpt(excerpt);
            request.setIsPublished(isPublished);
            
            // Parse category IDs
            try {
                Set<Long> categoryIds = Arrays.stream(categoryIdsStr.split(","))
                        .map(String::trim)
                        .map(Long::valueOf)
                        .collect(Collectors.toSet());
                request.setCategoryIds(categoryIds);
            } catch (NumberFormatException e) {
                return ApiResponseUtil.buildResponse(false, "Category IDs không hợp lệ", null, "/api/v1/blogs");
            }
            
            // Handle image upload if provided
            if (featuredImageFile != null && !featuredImageFile.isEmpty()) {
                String imageUrl = cloudinaryService.uploadFile(featuredImageFile);
                request.setFeaturedImage(imageUrl);
                log.info("Featured image uploaded successfully: {}", imageUrl);
            } else if (featuredImage != null && !featuredImage.trim().isEmpty()) {
                request.setFeaturedImage(featuredImage);
            }
            
            BlogResponse response = blogService.createBlog(request, authorId);
            return ApiResponseUtil.buildResponse(true, "Tạo bài viết thành công", response, "/api/v1/blogs");
        } catch (Exception e) {
            log.error("Error creating blog: ", e);
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blogs");
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> updateBlog(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "excerpt", required = false) String excerpt,
            @RequestParam(value = "featuredImageFile", required = false) MultipartFile featuredImageFile,
            @RequestParam(value = "featuredImage", required = false) String featuredImage,
            @RequestParam(value = "isPublished", defaultValue = "false") Boolean isPublished,
            @RequestParam("categoryIds") String categoryIdsStr) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long authorId = getUserIdFromAuthentication(auth);
            
            // Validate required fields
            if (title == null || title.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Tiêu đề không được để trống", null, "/api/v1/blogs/" + id);
            }
            if (content == null || content.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Nội dung không được để trống", null, "/api/v1/blogs/" + id);
            }
            if (categoryIdsStr == null || categoryIdsStr.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Phải chọn ít nhất 1 danh mục", null, "/api/v1/blogs/" + id);
            }
            
            // Build BlogRequest object
            BlogRequest request = new BlogRequest();
            request.setTitle(title.trim());
            request.setContent(content);
            request.setExcerpt(excerpt);
            request.setIsPublished(isPublished);
            
            // Parse category IDs
            try {
                Set<Long> categoryIds = Arrays.stream(categoryIdsStr.split(","))
                        .map(String::trim)
                        .map(Long::valueOf)
                        .collect(Collectors.toSet());
                request.setCategoryIds(categoryIds);
            } catch (NumberFormatException e) {
                return ApiResponseUtil.buildResponse(false, "Category IDs không hợp lệ", null, "/api/v1/blogs/" + id);
            }
            
            // Handle image upload if provided
            if (featuredImageFile != null && !featuredImageFile.isEmpty()) {
                String imageUrl = cloudinaryService.uploadFile(featuredImageFile);
                request.setFeaturedImage(imageUrl);
                log.info("Featured image uploaded successfully: {}", imageUrl);
            } else if (featuredImage != null && !featuredImage.trim().isEmpty()) {
                request.setFeaturedImage(featuredImage);
            }
            
            BlogResponse response = blogService.updateBlog(id, request, authorId);
            return ApiResponseUtil.buildResponse(true, "Cập nhật bài viết thành công", response, "/api/v1/blogs/" + id);
        } catch (Exception e) {
            log.error("Error updating blog: ", e);
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blogs/" + id);
        }
    }

    @GetMapping("/liked")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<BlogResponse>>> getLikedBlogs(@PageableDefault Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = getUserIdFromAuthentication(auth);
        PageResponse<BlogResponse> response = blogLikeService.getLikedBlogs(userId, pageable);
        return ApiResponseUtil.buildResponse(true, "Lấy danh sách bài viết đã tim thành công", response, "/api/v1/blogs/liked");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogResponse>> getBlogById(@PathVariable Long id) {
        return blogService.findById(id)
                .map(blog -> {
                    // Increment view count
                    blogService.incrementViewCount(id);
                    return ApiResponseUtil.buildResponse(true, "Lấy thông tin bài viết thành công", blog, "/api/v1/blogs/" + id);
                })
                .orElse(ApiResponseUtil.buildResponse(false, "Không tìm thấy bài viết", null, "/api/v1/blogs/" + id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<BlogResponse>> getBlogBySlug(@PathVariable String slug) {
        return blogService.findBySlugAndPublished(slug)
                .map(blog -> {
                    // Increment view count
                    blogService.incrementViewCount(blog.getBlogId());
                    return ApiResponseUtil.buildResponse(true, "Lấy thông tin bài viết thành công", blog, "/api/v1/blogs/slug/" + slug);
                })
                .orElse(ApiResponseUtil.buildResponse(false, "Không tìm thấy bài viết", null, "/api/v1/blogs/slug/" + slug));
    }

    @GetMapping("/admin/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> getBlogBySlugAdmin(@PathVariable String slug) {
        return blogService.findBySlug(slug)
                .map(blog -> ApiResponseUtil.buildResponse(true, "Lấy thông tin bài viết thành công", blog, "/api/v1/blogs/admin/" + slug))
                .orElse(ApiResponseUtil.buildResponse(false, "Không tìm thấy bài viết", null, "/api/v1/blogs/admin/" + slug));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BlogResponse>>> getAllPublishedBlogs(
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<BlogResponse> response = blogService.findAllPublished(pageable);
        return ApiResponseUtil.buildResponse(true, "Lấy danh sách bài viết thành công", response, "/api/v1/blogs");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<BlogResponse>>> getAllBlogs(
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<BlogResponse> response = blogService.findAll(pageable);
        return ApiResponseUtil.buildResponse(true, "Lấy danh sách bài viết thành công", response, "/api/v1/blogs/admin");
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<ApiResponse<PageResponse<BlogResponse>>> getBlogsByAuthor(
            @PathVariable Long authorId,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<BlogResponse> response = blogService.findByAuthorAndPublished(authorId, pageable);
        return ApiResponseUtil.buildResponse(true, "Lấy danh sách bài viết của tác giả thành công", response, "/api/v1/blogs/author/" + authorId);
    }

    @GetMapping("/category/{categorySlug}")
    public ResponseEntity<ApiResponse<PageResponse<BlogResponse>>> getBlogsByCategory(
            @PathVariable String categorySlug,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<BlogResponse> response = blogService.findByCategoryAndPublished(categorySlug, pageable);
        return ApiResponseUtil.buildResponse(true, "Lấy danh sách bài viết theo thể loại thành công", response, "/api/v1/blogs/category/" + categorySlug);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<BlogResponse>>> searchBlogs(
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<BlogResponse> response = blogService.searchByKeyword(keyword, pageable);
        return ApiResponseUtil.buildResponse(true, "Tìm kiếm bài viết thành công", response, "/api/v1/blogs/search?keyword=" + keyword);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBlog(@PathVariable Long id) {
        try {
            blogService.delete(id);
            return ApiResponseUtil.buildResponse(true, "Xóa bài viết thành công", null, "/api/v1/blogs/" + id);
        } catch (Exception e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blogs/" + id);
        }
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> publishBlog(@PathVariable Long id) {
        try {
            blogService.publish(id);
            return ApiResponseUtil.buildResponse(true, "Đăng bài viết thành công", null, "/api/v1/blogs/" + id + "/publish");
        } catch (Exception e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blogs/" + id + "/publish");
        }
    }

    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unpublishBlog(@PathVariable Long id) {
        try {
            blogService.unpublish(id);
            return ApiResponseUtil.buildResponse(true, "Hủy đăng bài viết thành công", null, "/api/v1/blogs/" + id + "/unpublish");
        } catch (Exception e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blogs/" + id + "/unpublish");
        }
    }

    @GetMapping("/check-slug/{slug}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<Boolean>> checkSlugExists(@PathVariable String slug) {
        boolean exists = blogService.existsBySlug(slug);
        return ApiResponseUtil.buildResponse(true, "Kiểm tra slug thành công", exists, "/api/v1/blogs/check-slug/" + slug);
    }
    
    /**
     * Helper method to extract user ID from JWT authentication
     * Assumes the JWT token contains user ID in subject or custom claim
     */
    private Long getUserIdFromAuthentication(Authentication auth) {
        try {
            return Long.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + auth.getName()));
            return user.getId();
        }
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> uploadImageForEditor(@RequestParam("upload") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "File không được để trống", null, "/api/v1/blogs/upload-image");
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ApiResponseUtil.buildResponse(false, "File phải là hình ảnh", null, "/api/v1/blogs/upload-image");
            }
            
            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ApiResponseUtil.buildResponse(false, "Kích thước file không được vượt quá 10MB", null, "/api/v1/blogs/upload-image");
            }
            
            String imageUrl = cloudinaryService.uploadFile(file);
            log.info("Editor image uploaded successfully: {}", imageUrl);
            
            return ApiResponseUtil.buildResponse(true, "Upload ảnh thành công", imageUrl, "/api/v1/blogs/upload-image");
        } catch (Exception e) {
            log.error("Error uploading editor image: ", e);
            return ApiResponseUtil.buildResponse(false, "Lỗi upload ảnh: " + e.getMessage(), null, "/api/v1/blogs/upload-image");
        }
    }

    @PostMapping("/{id}/save")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> saveBlog(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = getUserIdFromAuthentication(auth);
            blogService.saveBlog(id, userId);
            return ApiResponseUtil.buildResponse(true, "Đã lưu bài viết thành công", null, "/api/v1/blogs/" + id + "/save");
        } catch (Exception e) {
            log.error("Error saving blog: ", e);
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blogs/" + id + "/save");
        }
    }

    @DeleteMapping("/{id}/save")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unsaveBlog(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = getUserIdFromAuthentication(auth);
            blogService.unsaveBlog(id, userId);
            return ApiResponseUtil.buildResponse(true, "Đã bỏ lưu bài viết thành công", null, "/api/v1/blogs/" + id + "/save");
        } catch (Exception e) {
            log.error("Error unsaving blog: ", e);
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blogs/" + id + "/save");
        }
    }

    @GetMapping("/saved")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<BlogResponse>>> getSavedBlogs(@PageableDefault Pageable pageable) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = getUserIdFromAuthentication(auth);
            PageResponse<BlogResponse> response = blogService.getSavedBlogs(userId, pageable);
            return ApiResponseUtil.buildResponse(true, "Lấy lịch sử bài viết đã lưu thành công", response, "/api/v1/blogs/saved");
        } catch (Exception e) {
            log.error("Error getting saved blogs: ", e);
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blogs/saved");
        }
    }
}
