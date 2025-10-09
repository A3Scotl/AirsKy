package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CheckinEligiblePassengerResponse {
    private Long passengerId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String passportNumber;
    private String seatNumber;
    private BigDecimal ticketPrice;
    private boolean isCheckedIn;
    private String checkinStatus; // "ELIGIBLE", "ALREADY_CHECKED_IN", "PAYMENT_PENDING"
}