package iuh.fit.airsky.dto.response;

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
}