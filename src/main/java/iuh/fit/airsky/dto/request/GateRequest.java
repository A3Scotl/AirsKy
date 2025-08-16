package iuh.fit.airsky.dto.request;

import lombok.Data;

@Data
public class GateRequest {
    private Long airportId;
    private String gateName;
}