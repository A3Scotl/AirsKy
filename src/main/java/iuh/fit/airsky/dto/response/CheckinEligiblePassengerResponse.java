package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.CheckinStatus;
import iuh.fit.airsky.enums.PassengerType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CheckinEligiblePassengerResponse {
    private Long passengerId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String passportNumber;
    private PassengerType type;
    private String seatNumber;
    private BigDecimal ticketPrice;
    private boolean isCheckedIn;
    private CheckinStatus checkinStatus;
    private String boardingpassurl;
    
    // Flight segment information for roundtrip support
    private String flightNumber;
    private Long segmentId;
    private Integer segmentOrder;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String departureAirport;
    private String arrivalAirport;
}