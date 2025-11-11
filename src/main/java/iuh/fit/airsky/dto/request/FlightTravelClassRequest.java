package iuh.fit.airsky.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FlightTravelClassRequest {
    @NotNull(message = "Travel class ID is required")
    private Long classId;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Capacity cannot be negative")
    private Integer capacity;
    
    @Min(value = 0, message = "Booked seats cannot be negative")
    private Integer bookedSeat;
}
