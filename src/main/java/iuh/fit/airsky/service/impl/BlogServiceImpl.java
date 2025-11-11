package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.BlogRequest;
import iuh.fit.airsky.dto.response.BlogResponse;
import iuh.fit.airsky.dto.response.CategoryResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.BlogMapper;
import iuh.fit.airsky.mapper.CategoryMapper;
import iuh.fit.airsky.model.Blog;
import iuh.fit.airsky.model.Category;
import iuh.fit.airsky.model.SavedBlog;
import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.BlogRepository;
import iuh.fit.airsky.repository.CategoryRepository;
import iuh.fit.airsky.repository.SavedBlogRepository;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.BlogService;
import iuh.fit.airsky.service.NotificationService;
import iuh.fit.airsky.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final BlogMapper blogMapper;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SavedBlogRepository savedBlogRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public BlogResponse createBlog(BlogRequest request, Long authorId) {
        log.info("Creating new blog with title: {}", request.getTitle());
        
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + authorId));
        
        Set<Category> categories = request.getCategoryIds().stream()
                .map(categoryId -> categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại với ID: " + categoryId)))
                .collect(Collectors.toSet());
        
        Blog blog = blogMapper.toEntity(request);
        
        // Auto-generate unique slug from title
        String slug = SlugUtils.generateUniqueSlug(request.getTitle(), blogRepository::existsBySlug);
        blog.setSlug(slug);
        
        blog.setAuthor(author);
        blog.setCategories(categories);
        blog.setViewCount(0L);
        blog.setLikeCount(0L);
        
        if (request.getIsPublished() && request.getPublishedDate() == null) {
            blog.setPublishedDate(LocalDateTime.now());
        }
        
        Blog savedBlog = blogRepository.save(blog);
        
        log.info("Blog created successfully with ID: {}", savedBlog.getBlogId());
        return mapToBlogResponse(savedBlog);
    }

    @Override
    @Transactional
    public BlogResponse updateBlog(Long id, BlogRequest request, Long authorId) {
        log.info("Updating blog with ID: {}", id);
        
        Blog existingBlog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog không tồn tại với ID: " + id));
        
        // Check if user is the author
        if (!existingBlog.getAuthor().getId().equals(authorId)) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa blog này");
        }
        
        Set<Category> categories = request.getCategoryIds().stream()
                .map(categoryId -> categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại với ID: " + categoryId)))
                .collect(Collectors.toSet());
        
        existingBlog.setTitle(request.getTitle());
        existingBlog.setContent(request.getContent());
        existingBlog.setExcerpt(request.getExcerpt());
        existingBlog.setFeaturedImage(request.getFeaturedImage());
        existingBlog.setCategories(categories);
        
        // Auto-generate new slug if title changed
        String newSlug = SlugUtils.generateSlug(request.getTitle());
        if (!existingBlog.getSlug().equals(newSlug)) {
            String uniqueSlug = SlugUtils.generateUniqueSlug(request.getTitle(), 
                slug -> !slug.equals(existingBlog.getSlug()) && blogRepository.existsBySlug(slug));
            existingBlog.setSlug(uniqueSlug);
        }
        
        // Handle publish status
        if (request.getIsPublished() && !existingBlog.getIsPublished()) {
            existingBlog.setIsPublished(true);
            existingBlog.setPublishedDate(LocalDateTime.now());
        } else if (!request.getIsPublished() && existingBlog.getIsPublished()) {
            existingBlog.setIsPublished(false);
            existingBlog.setPublishedDate(null);
        }
        
        Blog updatedBlog = blogRepository.save(existingBlog);
        
        log.info("Blog updated successfully with ID: {}", updatedBlog.getBlogId());
        return mapToBlogResponse(updatedBlog);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BlogResponse> findById(Long id) {
        log.debug("Finding blog by ID: {}", id);
        return blogRepository.findById(id)
                .map(this::mapToBlogResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BlogResponse> findBySlug(String slug) {
        log.debug("Finding blog by slug: {}", slug);
        return blogRepository.findBySlug(slug)
                .map(this::mapToBlogResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BlogResponse> findBySlugAndPublished(String slug) {
        log.debug("Finding published blog by slug: {}", slug);
        return blogRepository.findBySlugAndIsPublishedTrue(slug)
                .map(this::mapToBlogResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> findAll(Pageable pageable) {
        log.debug("Finding all blogs with pagination");
        Page<Blog> blogPage = blogRepository.findAll(pageable);
        return createPageResponse(blogPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> findAllPublished(Pageable pageable) {
        log.debug("Finding all published blogs with pagination");
        Page<Blog> blogPage = blogRepository.findByIsPublishedTrue(pageable);
        return createPageResponse(blogPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> findByAuthor(Long authorId, Pageable pageable) {
        log.debug("Finding blogs by author ID: {}", authorId);
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + authorId));
        
        Page<Blog> blogPage = blogRepository.findByAuthor(author, pageable);
        return createPageResponse(blogPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> findByAuthorAndPublished(Long authorId, Pageable pageable) {
        log.debug("Finding published blogs by author ID: {}", authorId);
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + authorId));
        
        Page<Blog> blogPage = blogRepository.findByAuthorAndIsPublishedTrue(author, pageable);
        return createPageResponse(blogPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> findByCategoryAndPublished(String categorySlug, Pageable pageable) {
        log.debug("Finding published blogs by category slug: {}", categorySlug);
        Page<Blog> blogPage = blogRepository.findByCategorySlugAndIsPublishedTrue(categorySlug, pageable);
        return createPageResponse(blogPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> searchByKeyword(String keyword, Pageable pageable) {
        log.debug("Searching published blogs by keyword: {}", keyword);
        Page<Blog> blogPage = blogRepository.findByKeywordAndIsPublishedTrue(keyword, pageable);
        return createPageResponse(blogPage);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting blog with ID: {}", id);
        
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog không tồn tại với ID: " + id));
        
        blogRepository.delete(blog);
        log.info("Blog deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        log.info("Publishing blog with ID: {}", id);
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog không tồn tại với ID: " + id));
        log.info("Current isPublished: {}, publishedDate: {}", blog.getIsPublished(), blog.getPublishedDate());
        blog.setIsPublished(true);
        blog.setPublishedDate(LocalDateTime.now());
        Blog savedBlog = blogRepository.save(blog);
        log.info("Blog published successfully with ID: {}", id);
        // Gửi thông báo real-time cho tất cả users khi blog được publish
        try {
            sendBlogPublishedNotification(savedBlog);
        } catch (Exception e) {
            log.error("[SAFE] Error while sending blog published notification for blog {}: {}", id, e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void unpublish(Long id) {
        log.info("Unpublishing blog with ID: {}", id);
        
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog không tồn tại với ID: " + id));
        
        blog.setIsPublished(false);
        blog.setPublishedDate(null);
        
        blogRepository.save(blog);
        log.info("Blog unpublished successfully with ID: {}", id);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        log.debug("Incrementing view count for blog ID: {}", id);
        
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog không tồn tại với ID: " + id));
        
        blog.setViewCount(blog.getViewCount() + 1);
        blogRepository.save(blog);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return blogRepository.existsBySlug(slug);
    }

    @Override
    @Transactional
    public void saveBlog(Long blogId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog không tồn tại với ID: " + blogId));
        if (savedBlogRepository.existsByUserAndBlog(user, blog)) {
            throw new IllegalArgumentException("Blog đã được lưu trước đó");
        }
        SavedBlog savedBlog = SavedBlog.builder()
                .user(user)
                .blog(blog)
                .build();
        savedBlogRepository.save(savedBlog);
    }

    @Override
    @Transactional
    public void unsaveBlog(Long blogId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog không tồn tại với ID: " + blogId));
        if (!savedBlogRepository.existsByUserAndBlog(user, blog)) {
            throw new IllegalArgumentException("Blog chưa được lưu");
        }
        savedBlogRepository.deleteByUserAndBlog(user, blog);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> getSavedBlogs(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));
        var savedBlogPage = savedBlogRepository.findByUser(user, pageable);
        var blogResponses = savedBlogPage.getContent().stream()
                .map(SavedBlog::getBlog)
                .map(this::mapToBlogResponse)
                .toList();
        return new PageResponse<>(
                blogResponses,
                savedBlogPage.getNumber(),
                savedBlogPage.getSize(),
                savedBlogPage.getTotalElements(),
                savedBlogPage.getTotalPages(),
                savedBlogPage.isLast()
        );
    }

    private BlogResponse mapToBlogResponse(Blog blog) {
    log.debug("Mapping blog {} with {} categories", blog.getBlogId(), 
             blog.getCategories() != null ? blog.getCategories().size() : 0);
    
    // Thêm log chi tiết categories
    if (blog.getCategories() != null && !blog.getCategories().isEmpty()) {
        String categoryIds = blog.getCategories().stream()
                .map(Category::getCategoryId)
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        log.debug("Categories for blog {}: {}", blog.getBlogId(), categoryIds);
    }
    
    return BlogResponse.builder()
            .blogId(blog.getBlogId())
            .title(blog.getTitle())
            .content(blog.getContent())
            .slug(blog.getSlug())
            .excerpt(blog.getExcerpt())
            .featuredImage(blog.getFeaturedImage())
            .publishedDate(blog.getPublishedDate())
            .isPublished(blog.getIsPublished())
            .viewCount(blog.getViewCount())
            .likeCount(blog.getLikeCount())
            .createdAt(blog.getCreatedAt())
            .updatedAt(blog.getUpdatedAt())
            .authorId(blog.getAuthor().getId())
            .authorName(blog.getAuthor().getFirstName() + " " + blog.getAuthor().getLastName())
            .authorEmail(blog.getAuthor().getEmail())
            .categories(blog.getCategories() != null && !blog.getCategories().isEmpty() ? 
                       blog.getCategories().stream()
                               .map(categoryMapper::toResponseDTO)
                               .collect(Collectors.toSet()) : 
                       Set.of())
            .build();
}

    private PageResponse<BlogResponse> createPageResponse(Page<Blog> blogPage) {
        return new PageResponse<>(
                blogPage.getContent().stream()
                        .map(this::mapToBlogResponse)
                        .collect(Collectors.toList()),
                blogPage.getNumber(),
                blogPage.getSize(),
                blogPage.getTotalElements(),
                blogPage.getTotalPages(),
                blogPage.isLast()
        );
    }

    /**
     * Gửi thông báo real-time khi blog được publish
     * Sử dụng @Async để không block main transaction
     */
    @Async
    public void sendBlogPublishedNotification(Blog blog) {
        try {
            log.info("Sending blog published notification for blog: {}", blog.getBlogId());
            List<User> allUsers = userRepository.findAll();
            if (allUsers.isEmpty()) {
                log.info("No users found to send blog notification");
                return;
            }
            String message = String.format("Bài viết mới: %s",
                blog.getTitle().length() > 50 ? blog.getTitle().substring(0, 47) + "..." : blog.getTitle());
            String title = "Bài viết mới từ " + blog.getAuthor().getFirstName() + " " + blog.getAuthor().getLastName();
            int notificationCount = 0;
            for (User user : allUsers) {
                // Chỉ gửi cho user có id hợp lệ và không phải tác giả
                if (user.getId() != null && !user.getId().equals(blog.getAuthor().getId())) {
                    try {
                        notificationService.createAndSendNotification(
                            user.getId(),
                            "NEW_PUBLIC_BLOG",
                            message,
                            blog.getBlogId(),
                            title
                        );
                        notificationCount++;
                        if (notificationCount % 10 == 0) {
                            Thread.sleep(100); // 100ms delay mỗi 10 notifications
                        }
                    } catch (Exception e) {
                        log.error("Failed to send blog notification to user {}: {}", user.getId(), e.getMessage());
                    }
                }
            }
            log.info("Sent blog published notifications to {} users for blog {}", notificationCount, blog.getBlogId());
        } catch (Exception e) {
            log.error("Error sending blog published notification for blog {}: {}", blog.getBlogId(), e.getMessage(), e);
        }
    }
}
