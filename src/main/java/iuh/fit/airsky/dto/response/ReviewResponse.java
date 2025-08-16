package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Long reviewId;
    private Long userId;
    private Long flightId;
    private Integer rating;
    private String comments;
    private LocalDateTime reviewDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}