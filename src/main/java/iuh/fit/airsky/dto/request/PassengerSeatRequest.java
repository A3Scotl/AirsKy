package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.PassengerType;
import iuh.fit.airsky.enums.SeatTypes;
import iuh.fit.airsky.enums.BaggagePackage;
import iuh.fit.airsky.enums.Gender;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PassengerSeatRequest {
    private Long bookingId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String passportNumber;
    private PassengerType type;

    // Thay đổi: Thay seatId duy nhất bằng List<SeatAssignmentRequest>
    private List<SeatAssignmentRequest> seatAssignments;

    private SeatTypes seatType; // Loại ghế được chọn (STANDARD, EXTRA_LEGROOM, etc.)

    private BaggagePackage baggagePackage;

    // Thêm các trường mới (không bắt buộc)
    private String email;
    private String phone;
    private Gender gender;

    @Data
    public static class SeatAssignmentRequest {
        private Integer segmentOrder; // Thứ tự segment (1, 2, 3...)
        private Long seatId; // ID của ghế cho segment này
        private SeatTypes seatType; // Loại ghế cho segment này (có thể null nếu dùng seatType của passenger)
    }
}
