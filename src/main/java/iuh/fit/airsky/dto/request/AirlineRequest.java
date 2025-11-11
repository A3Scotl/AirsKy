package iuh.fit.airsky.dto.request;

import lombok.Data;

@Data
public class AirlineRequest {
    private String airlineCode;
    private String airlineName;
    private String contact;
    private String thumbnail;
    private Boolean active;
}