package iuh.fit.airsky.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FlightSegmentRequest {
    private Integer segmentOrder; // Thứ tự của segment (1, 2, 3...)
    private Long flightId;
    private Long classId;
    private AirportInfo departure;
    private AirportInfo arrival;
    private BigDecimal price;
    private String aircraft;
    private String duration;
    private List<PassengerSeatAssignment> passengerSeats;

    @Data
    public static class AirportInfo {
        private String code;
        private String city;
        private LocalDateTime time;
        private String airport;
        private String terminal;
        private String gate;
    }

    @Data
    public static class PassengerSeatAssignment {
        private Long passengerId;
        private Long seatId;
    }
}