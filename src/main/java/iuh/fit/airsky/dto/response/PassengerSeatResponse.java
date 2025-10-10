package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.Gender;
import iuh.fit.airsky.enums.PassengerType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PassengerSeatResponse {
    private Long passengerId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String passportNumber;
    private PassengerType type;
    private String seatNumber;
    private String className;

    private Gender gender;
    private String email;
    private String phone;
}

