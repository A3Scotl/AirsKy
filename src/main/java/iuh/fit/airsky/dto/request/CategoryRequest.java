package iuh.fit.airsky.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {
    
    @NotBlank(message = "Tên thể loại không được để trống")
    @Size(max = 100, message = "Tên thể loại không được vượt quá 100 ký tự")
    private String name;
    
    // Slug sẽ được tự động tạo từ name, không cần user nhập
    @Size(max = 100, message = "Slug không được vượt quá 100 ký tự")
    private String slug;
    
    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    private Boolean active;
}
