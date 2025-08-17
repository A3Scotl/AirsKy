package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.ReservationStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {
    private Long bookingId;
    private String userEmail;
    private String flightNumber;
    private String travelClass;
    private LocalDateTime bookingDate;
    private BigDecimal totalAmount;
    private ReservationStatus status;
    private List<PassengerSeatResponse> passengers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}