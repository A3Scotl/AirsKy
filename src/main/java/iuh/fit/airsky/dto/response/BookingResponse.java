package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.BookingStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {
    private Long bookingId;
    private String bookingCode;
    private String userEmail; // Email của user (authenticated) hoặc passenger đầu tiên (guest)
    private String flightNumber;
    private String travelClass;
    private LocalDateTime bookingDate;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private List<PassengerSeatResponse> passengers;
    private PaymentResponse payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BaggageResponse> baggage;
    private List<FlightSegmentResponse> flightSegments;

    // Deal information
    private String appliedDealCode;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    
    // Ancillary services
    private List<BookingAncillaryServiceResponse> ancillaryServices;
    private BigDecimal ancillaryServicesAmount;

    // Seat type pricing details
    private BigDecimal seatTypeAmount;
    private List<SeatTypePricingDetail> seatTypeDetails;
}