package iuh.fit.airsky.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long reviewId;
    private Long bookingId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long flightId;
    private String flightCode;
    private Integer rating;
    private String comment;
    private LocalDateTime reviewDate;
    private Boolean isApproved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}