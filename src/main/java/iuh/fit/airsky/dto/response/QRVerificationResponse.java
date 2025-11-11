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
public class QRVerificationResponse {
    private boolean valid;
    private String message;
    private String bookingCode;
    private Long passengerId;
    private String passengerName;
    private String flightNumber;
    private String seatNumber;
    private String route;
    private LocalDateTime departureTime;
    private LocalDateTime checkedAt;
    private String status;
}