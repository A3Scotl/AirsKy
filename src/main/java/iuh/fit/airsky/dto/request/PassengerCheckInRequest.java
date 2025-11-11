package iuh.fit.airsky.dto.request;

import lombok.Data;

@Data
public class PassengerCheckInRequest {
    private String bookingCode;
    private String firstName;
    private String lastName;
}
