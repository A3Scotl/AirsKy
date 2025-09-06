package iuh.fit.airsky.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AirlineResponse {
    private Long airlineId;
    private String airlineCode;
    private String airlineName;
    private String contact;
    private String thumbnail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;
    private LocalDateTime deletedAt;
    private boolean deleted;
}

