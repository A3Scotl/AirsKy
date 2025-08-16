package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AirportResponse {
    private Long airportId;
    private String airportCode;
    private String airportName;
    private String city;
    private String country;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;
    private LocalDateTime deletedAt;
    private boolean deleted;
}