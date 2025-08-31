package iuh.fit.airsky.dto.request;

import lombok.Data;

@Data
public class AirportRequest {
    private String airportCode;
    private String airportName;
    private Long countryId;
    private String cityName;
    private String thumbnail;
    private Boolean active;
}