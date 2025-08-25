package iuh.fit.airsky.service;

public interface BlogLikeService {
    void likeBlog(Long blogId, Long userId);
    void unlikeBlog(Long blogId, Long userId);
    boolean isLikedByUser(Long blogId, Long userId);
    long countLikes(Long blogId);
}
