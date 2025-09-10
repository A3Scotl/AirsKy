package iuh.fit.airsky.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AirportResponse {
    private Long airportId;
    private String airportCode;
    private String airportName;
    private List<String> cityNames;
    private String thumbnail;
    private String country;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;
    private LocalDateTime deletedAt;
    private boolean deleted;
    private List<GateResponse> gates;
}

