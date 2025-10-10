package iuh.fit.airsky.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingAncillaryServiceRequest {
    
    @NotNull(message = "ID dịch vụ không được để trống")
    private Long serviceId;

    private Long passengerId; // passengerId từ passengerSeats (có thể là bất kỳ giá trị nào), null nếu áp dụng cho toàn booking

    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity = 1;

    private String notes;
}