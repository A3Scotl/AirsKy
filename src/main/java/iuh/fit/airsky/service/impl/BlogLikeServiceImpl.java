package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.response.BlogResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.model.Blog;
import iuh.fit.airsky.model.BlogLike;
import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.BlogLikeRepository;
import iuh.fit.airsky.repository.BlogRepository;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.BlogLikeService;
import iuh.fit.airsky.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BlogLikeServiceImpl implements BlogLikeService {

    private final BlogLikeRepository blogLikeRepository;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public void likeBlog(Long blogId, Long userId) {
        log.info("User {} liking blog {}", userId, blogId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog không tồn tại với ID: " + blogId));

        // Check if user already liked this blog
        if (blogLikeRepository.existsByBlogAndUser(blog, user)) {
            log.warn("User {} already liked blog {}", userId, blogId);
            return;
        }

        BlogLike blogLike = BlogLike.builder()
                .blog(blog)
                .user(user)
                .build();

        blogLikeRepository.save(blogLike);

        // Update like count in blog
        blog.setLikeCount(blog.getLikeCount() + 1);
        blogRepository.save(blog);

        log.info("User {} successfully liked blog {}", userId, blogId);
    }

    @Override
    @Transactional
    public void unlikeBlog(Long blogId, Long userId) {
        log.info("User {} unliking blog {}", userId, blogId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog không tồn tại với ID: " + blogId));

        // Check if user has liked this blog
        if (!blogLikeRepository.existsByBlogAndUser(blog, user)) {
            log.warn("User {} has not liked blog {}", userId, blogId);
            return;
        }

        blogLikeRepository.deleteByBlogAndUser(blog, user);

        // Update like count in blog
        if (blog.getLikeCount() > 0) {
            blog.setLikeCount(blog.getLikeCount() - 1);
            blogRepository.save(blog);
        }

        log.info("User {} successfully unliked blog {}", userId, blogId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long blogId, Long userId) {
        log.debug("Checking if user {} liked blog {}", userId, blogId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog không tồn tại với ID: " + blogId));

        return blogLikeRepository.existsByBlogAndUser(blog, user);
    }

    @Override
    @Transactional(readOnly = true)
    public long countLikes(Long blogId) {
        log.debug("Counting likes for blog {}", blogId);

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog không tồn tại với ID: " + blogId));

        return blogLikeRepository.countByBlog(blog);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> getLikedBlogs(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));
        Page<BlogLike> likedPage = blogLikeRepository.findByUser(user, pageable);
        var blogResponses = likedPage.getContent().stream()
                .map(BlogLike::getBlog)
                .map(this::mapToBlogResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(
                blogResponses,
                likedPage.getNumber(),
                likedPage.getSize(),
                likedPage.getTotalElements(),
                likedPage.getTotalPages(),
                likedPage.isLast()
        );
    }

    private BlogResponse mapToBlogResponse(Blog blog) {
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
                .categories(blog.getCategories() != null ? blog.getCategories().stream().map(categoryMapper::toResponseDTO).collect(Collectors.toSet()) : Set.of())
                .build();
    }
}
