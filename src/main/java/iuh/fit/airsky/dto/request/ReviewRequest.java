package iuh.fit.airsky.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewRequest {
    private Long userId;
    private Long flightId;
    private Integer rating;
    private String comments;
    private LocalDateTime reviewDate;
}