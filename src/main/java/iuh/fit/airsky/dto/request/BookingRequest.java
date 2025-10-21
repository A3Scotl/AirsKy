package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.CheckInType;
import iuh.fit.airsky.enums.PaymentMethod;
import iuh.fit.airsky.enums.BookingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
public class BookingRequest {
    private Long userId;
    // Thay đổi: sử dụng flightSegments thay vì flightId và classId đơn lẻ
    private List<FlightSegmentRequest> flightSegments;
    private LocalDateTime bookingDate;
    private LocalDateTime holdTime;
    private BigDecimal totalAmount;
    private BookingStatus status = BookingStatus.PENDING;
    private List<PassengerSeatRequest> passengers;
    private PaymentMethod paymentMethod;
    private CheckInType checkInType = CheckInType.OFFLINE;
    private String dealCode; // Mã giảm giá (tùy chọn)
    private List<BookingAncillaryServiceRequest> ancillaryServices; // Dịch vụ đi kèm
    private String contactEmail; // Email để nhận thông tin booking
    private String contactName; // Tên người liên hệ

}