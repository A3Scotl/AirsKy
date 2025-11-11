package iuh.fit.airsky.dto.request;

import java.util.List;

import iuh.fit.airsky.dto.request.GateRequest;
import lombok.Data;

@Data
public class AirportRequest {
    private String airportCode;
    private String airportName;
    private Long countryId;
    private List<String> cityNames;
    private Boolean active;
    private List<GateRequest> gates;
    private String thumbnailUrl; // chỉ nhận url ảnh từ client, không nhận file
}