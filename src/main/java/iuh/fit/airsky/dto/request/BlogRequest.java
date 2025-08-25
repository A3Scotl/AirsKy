package iuh.fit.airsky.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class BlogRequest {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    private String title;
    
    @NotBlank(message = "Nội dung không được để trống")
    private String content; // HTML content from CKEditor
    
    // Slug will be auto-generated from title, so no validation needed
    private String slug; // Optional, will be generated if not provided
    
    @Size(max = 1000, message = "Tóm tắt không được vượt quá 1000 ký tự")
    private String excerpt;
    
    // File upload for featured image
    private MultipartFile featuredImageFile;
    
    // Optional: URL for featured image (if not uploading file)
    @Size(max = 300, message = "Đường dẫn ảnh đại diện không được vượt quá 300 ký tự")
    private String featuredImage;
    
    private LocalDateTime publishedDate;
    
    private Boolean isPublished = false;
    
    @NotEmpty(message = "Phải chọn ít nhất 1 danh mục")
    private Set<Long> categoryIds;
}
