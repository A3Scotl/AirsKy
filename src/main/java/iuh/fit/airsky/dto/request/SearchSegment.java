package iuh.fit.airsky.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchSegment {
    @NotNull(message = "Departure airport ID is required")
    private Long departureAirportId;

    @NotNull(message = "Arrival airport ID is required")
    private Long arrivalAirportId;

    @NotNull(message = "Departure date is required")
    @FutureOrPresent(message = "Departure date must be today or in the future")
    private LocalDate departureDate;
}
