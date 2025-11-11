package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.response.SeatResponse;
import iuh.fit.airsky.model.Seat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeatMapper {

    @Mapping(target = "seatId", source = "seatId")
    @Mapping(target = "seatNumber", source = "seatNumber")
    @Mapping(target = "className", source = "travelClass.className")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "bookedBy", expression = "java(seat.getBookedByUser() != null ? seat.getBookedByUser().getDisplayName() : (seat.getBookedByPassenger() != null ? seat.getBookedByPassenger().getFirstName() + \" \" + seat.getBookedByPassenger().getLastName() : null))")
    @Mapping(target = "seatType", source = "type")
    @Mapping(target = "flightId", source = "flight.flightId")
    @Mapping(target = "travelClassId", source = "travelClass.id")
    @Mapping(target = "bookedByUserId", expression = "java(seat.getBookedByUser() != null ? seat.getBookedByUser().getId() : null)")
    @Mapping(target = "bookedByPassengerId", expression = "java(seat.getBookedByPassenger() != null ? seat.getBookedByPassenger().getPassengerId() : null)")

    SeatResponse toResponseDTO(Seat seat);
}
