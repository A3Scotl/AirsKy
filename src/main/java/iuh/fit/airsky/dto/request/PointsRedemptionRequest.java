package iuh.fit.airsky.dto.request;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class PointsRedemptionRequest {
    
    @NotNull(message = "User ID không được để trống")
    private Long userId;
    
    @NotNull(message = "Số điểm không được để trống")
    @Min(value = 100, message = "Số điểm tối thiểu là 100")
    private Integer pointsToRedeem;
    
    @NotNull(message = "Số tiền giảm giá không được để trống")
    @Min(value = 10000, message = "Số tiền giảm giá tối thiểu là 10,000 VND")
    private Long discountAmount; // Amount in VND
}