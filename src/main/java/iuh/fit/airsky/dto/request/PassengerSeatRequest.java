package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.PassengerType;
import iuh.fit.airsky.enums.SeatTypes;
import iuh.fit.airsky.enums.BaggagePackage;
import iuh.fit.airsky.enums.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PassengerSeatRequest {
    private Long bookingId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String passportNumber;
    private PassengerType type;
    private Long seatId;

    private BaggagePackage baggagePackage;

    // Thêm các trường mới (không bắt buộc)
    private String email;
    private String phone;
    private Gender gender;
}
