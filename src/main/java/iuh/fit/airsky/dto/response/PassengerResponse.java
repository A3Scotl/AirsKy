package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.Gender;
import iuh.fit.airsky.enums.PassengerType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PassengerResponse {
    private Long passengerId;
    private Long bookingId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String passportNumber;
    private PassengerType type;
    private String email;
    private String phone;
    private Gender gender;
    private String membershipCode;

    // Thêm thông tin quốc gia và nơi ở
    private String nationality;
    private String currentResidence;
}