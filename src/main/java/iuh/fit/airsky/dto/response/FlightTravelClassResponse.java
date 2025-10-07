package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlightTravelClassResponse {
    private Long id;
    private TravelClassResponse travelClass;
    private BigDecimal price;
    private Integer capacity;
    private Integer bookedSeat;
}
