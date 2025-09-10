package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.FlightType;
import iuh.fit.airsky.enums.TripType;
import iuh.fit.airsky.model.Aircraft;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FlightRequest {
    private String flightNumber;

    @NotNull(message = "Airline ID is required")
    private Long airlineId;

    @NotNull(message = "Departure airport ID is required")
    private Long departureAirportId;

    @NotNull(message = "Arrival airport ID is required")
    private Long arrivalAirportId;

    @NotNull(message = "Aircraft ID is required")
    private Long aircraftId;

    @NotNull(message = "Departure time is required")
    @Future(message = "Departure time must be in the future")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalDateTime arrivalTime;

    private String stops;
    private List<StopRequest> stopsList;

    @NotNull(message = "Gate ID is required")
    private Long gateId; 

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    private BigDecimal basePrice;

    @NotNull(message = "Flight status is required")
    private FlightStatus status;

    @NotNull(message = "Flight type is required")
    private FlightType type;

    @NotNull(message = "Business ID is required")
    private Long businessId;

    @NotNull(message = "Trip type is required")
    private TripType tripType;

    private String roundTripGroupId;

    // Thêm danh sách travel class với giá tùy chỉnh
    private List<FlightTravelClassRequest> flightTravelClasses;
}