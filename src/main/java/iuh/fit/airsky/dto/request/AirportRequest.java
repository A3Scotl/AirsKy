package iuh.fit.airsky.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class AirportRequest {
    private String airportCode;
    private String airportName;
    private Long countryId;
    private List<String> cityNames;
    private String thumbnail;
    private Boolean active;
}