package iuh.fit.airsky.dto.request;

import lombok.Data;

@Data
public class GateRequest {
    private String gateName;
    private String terminal;
    private Long airportId;
}
