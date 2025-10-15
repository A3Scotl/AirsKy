package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.PaymentMethod;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CheckinRequest {
    // Booking identification - Ưu tiên dùng passengerId thay vì tên để tránh trùng
    private String bookingCode;
    private String passengerFullName; // Optional - chỉ dùng khi không có passengerId
    private Long passengerId;         // Required - chính xác hơn tên

    // Seat change information - Hỗ trợ cả seatId và seatNumber
    private String newSeatNumber;     // Backward compatibility
    private Long newSeatId;          // Preferred - chính xác hơn
    private BigDecimal seatChangeCharge;

    // Ancillary services updates - Only add new services
    private List<BookingAncillaryServiceRequest> servicesToAdd;

    private BigDecimal totalAdditionalCharge;

    // Payment method for additional charges (seat changes, services)
    private PaymentMethod paymentMethod;

    // Legacy fields - giữ để tương thích
    private Long bookingId;
    private String seatNumber;
    private BigDecimal ticketPrice;
}