package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.PassengerType;
import iuh.fit.airsky.enums.SeatTypes;
import iuh.fit.airsky.enums.BaggagePackage;
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
    private Long seatId;

    private BaggagePackage baggagePackage;

}
