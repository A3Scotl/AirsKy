package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.response.BlogResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface BlogLikeService {
    void likeBlog(Long blogId, Long userId);
    void unlikeBlog(Long blogId, Long userId);
    boolean isLikedByUser(Long blogId, Long userId);
    long countLikes(Long blogId);
    PageResponse<BlogResponse> getLikedBlogs(Long userId, Pageable pageable);
}
