package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.AncillaryServiceType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AncillaryServiceRequest {
    
    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(max = 100, message = "Tên dịch vụ không được vượt quá 100 ký tự")
    private String serviceName;
    
    @NotNull(message = "Loại dịch vụ không được để trống")
    private AncillaryServiceType serviceType;
    
    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;
    
    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;
    
    private Boolean isActive = true;
    
    private String thumbnail;
    
    @Min(value = 1, message = "Số lượng tối đa phải lớn hơn 0")
    private Integer maxQuantity = 1;
    
    private Boolean isPerPassenger = true;
}