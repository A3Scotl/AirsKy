package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AvailableSeatsResponse {
    private Long segmentId;
    private Integer segmentOrder;
    private String flightNumber;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String departureAirport;
    private String arrivalAirport;
    private List<String> availableSeats;
    private boolean canSelectSeat; // Based on check-in status
    private String message; // Explanation if seat selection is disabled

    public AvailableSeatsResponse(Long segmentId, Integer segmentOrder, String flightNumber,
                                 LocalDateTime departureTime, LocalDateTime arrivalTime,
                                 String departureAirport, String arrivalAirport,
                                 List<String> availableSeats, boolean canSelectSeat, String message) {
        this.segmentId = segmentId;
        this.segmentOrder = segmentOrder;
        this.flightNumber = flightNumber;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.availableSeats = availableSeats;
        this.canSelectSeat = canSelectSeat;
        this.message = message;
    }
}