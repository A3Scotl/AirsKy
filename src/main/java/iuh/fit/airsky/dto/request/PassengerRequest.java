package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.PassengerType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PassengerRequest {
    private Long bookingId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String passportNumber;
    private PassengerType type;
}