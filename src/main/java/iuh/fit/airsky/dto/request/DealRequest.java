package iuh.fit.airsky.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DealRequest {
    
    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(max = 20, message = "Mã giảm giá không được vượt quá 20 ký tự")
    private String dealCode;
    
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    private String title;
    
    @NotNull(message = "Phần trăm giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Phần trăm giảm giá phải lớn hơn 0")
    @DecimalMax(value = "100.00", message = "Phần trăm giảm giá không được vượt quá 100%")
    private BigDecimal discountPercentage;
    
    @NotNull(message = "Giá trị đơn hàng tối thiểu không được để trống")
    @DecimalMin(value = "0.00", message = "Giá trị đơn hàng tối thiểu phải lớn hơn hoặc bằng 0")
    private BigDecimal minimumOrderAmount;
    
    @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
    private LocalDateTime validFrom;
    
    @NotNull(message = "Ngày kết thúc hiệu lực không được để trống")
    private LocalDateTime validTo;
    
    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;
    
    private String thumbnail;
    
    @DecimalMin(value = "0.00", message = "Giảm giá tối đa phải lớn hơn hoặc bằng 0")
    private BigDecimal maxDiscountAmount;
    
    private Long departureAirportId;
    
    private Long arrivalAirportId;
    
    @Min(value = 1, message = "Tổng số lượng sử dụng phải lớn hơn 0")
    private Integer totalUsageLimit;
    
    // Alternative field name for API convenience
    @Min(value = 1, message = "Số lượng sử dụng phải lớn hơn 0")
    private Integer usageLimit;
    
    @Min(value = 1, message = "Số lần sử dụng mỗi người phải lớn hơn 0")
    private Integer usagePerUser = 1;
    
    private Boolean isActive = true;
}
