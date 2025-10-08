package iuh.fit.airsky.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerCheckInResponse {
    private Long checkInId;
    private String bookingCode;
    private Long bookingId;
    private Long passengerId;
    private String passengerName;
    private String seatNumber;
    private String flightNumber;
    private String departureAirport;
    private String arrivalAirport;
    private LocalDateTime departureTime;
    private LocalDateTime checkedAt;
    private String boardingPassUrl;
}
