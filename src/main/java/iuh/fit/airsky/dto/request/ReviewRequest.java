package iuh.fit.airsky.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewRequest {

    @NotNull(message = "Booking ID không được để trống")
    private Long bookingId;

    @NotNull(message = "User ID không được để trống")
    private Long userId;

    @NotNull(message = "Flight ID không được để trống")
    private Long flightId;

    // Rating và comment có thể null khi tạo review request tự động
    @Min(value = 1, message = "Rating phải từ 1 đến 5")
    @Max(value = 5, message = "Rating phải từ 1 đến 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment không được vượt quá 1000 ký tự")
    private String comment;

    private LocalDateTime reviewDate;

    private Boolean isApproved = true;

    // Thêm các trường cho review request
    private LocalDateTime eligibleAt;
}