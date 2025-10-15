package iuh.fit.airsky.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class SeatChangeCalculationRequest {
    private String bookingCode;
    private Long passengerId;
    private Long newSeatId;
    private String newSeatNumber; // Fallback if newSeatId not provided
    private List<BookingAncillaryServiceRequest> servicesToAdd; // Ancillary services to add
}
