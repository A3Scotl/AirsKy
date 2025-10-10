package iuh.fit.airsky.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CheckinRequest {
    // Booking identification
    private String bookingCode;
    private String passengerFullName;
    private Long passengerId;

    // Seat change information
    private String newSeatNumber;
    private BigDecimal seatChangeCharge;

    // Ancillary services updates
    private List<BookingAncillaryServiceRequest> servicesToAdd;
    private List<Long> serviceIdsToRemove;

    private BigDecimal totalAdditionalCharge; 

    private Long bookingId;
    private String seatNumber;
    private BigDecimal ticketPrice;
}