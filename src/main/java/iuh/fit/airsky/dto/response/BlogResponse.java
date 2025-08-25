package iuh.fit.airsky.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogResponse {
    private Long blogId;
    private String title;
    private String content;
    private String slug;
    private String excerpt;
    private String featuredImage;
    private LocalDateTime publishedDate;
    private Boolean isPublished;
    private Long viewCount;
    private Long likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Author info
    private Long authorId;
    private String authorName;
    private String authorEmail;
    
    // Categories
    private Set<CategoryResponse> categories;
    
    // Comments count
    private Long commentCount;
}
