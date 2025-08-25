package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.model.User;
import iuh.fit.airsky.service.BlogLikeService;
import iuh.fit.airsky.service.UserService;
import iuh.fit.airsky.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/blog-likes")
@RequiredArgsConstructor
public class BlogLikeController {

    private final BlogLikeService blogLikeService;
    private final UserService userService;

    @PostMapping("/blog/{blogId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> likeBlog(@PathVariable Long blogId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName(); // Lấy email từ JWT
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            blogLikeService.likeBlog(blogId, user.getId());
            return ApiResponseUtil.buildResponse(true, "Thích bài viết thành công", null, "/api/v1/blog-likes/blog/" + blogId);
        } catch (Exception e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blog-likes/blog/" + blogId);
        }
    }

    @DeleteMapping("/blog/{blogId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> unlikeBlog(@PathVariable Long blogId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName(); // Lấy email từ JWT
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            blogLikeService.unlikeBlog(blogId, user.getId());
            return ApiResponseUtil.buildResponse(true, "Bỏ thích bài viết thành công", null, "/api/v1/blog-likes/blog/" + blogId);
        } catch (Exception e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blog-likes/blog/" + blogId);
        }
    }

    @GetMapping("/blog/{blogId}/check")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<Boolean>> checkIfLiked(@PathVariable Long blogId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName(); // Lấy email từ JWT
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            boolean isLiked = blogLikeService.isLikedByUser(blogId, user.getId());
            return ApiResponseUtil.buildResponse(true, "Kiểm tra trạng thái thích thành công", isLiked, "/api/v1/blog-likes/blog/" + blogId + "/check");
        } catch (Exception e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blog-likes/blog/" + blogId + "/check");
        }
    }

    @GetMapping("/blog/{blogId}/count")
    public ResponseEntity<ApiResponse<Long>> getLikeCount(@PathVariable Long blogId) {
        try {
            long count = blogLikeService.countLikes(blogId);
            return ApiResponseUtil.buildResponse(true, "Lấy số lượt thích thành công", count, "/api/v1/blog-likes/blog/" + blogId + "/count");
        } catch (Exception e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/blog-likes/blog/" + blogId + "/count");
        }
    }
}
