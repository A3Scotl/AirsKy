package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.response.SeatResponse;
import iuh.fit.airsky.model.Seat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeatMapper {

    @Mapping(target = "className", source = "travelClass.className")
    @Mapping(target = "bookedBy", expression = "java(seat.getBookedBy() != null ? seat.getBookedBy().getFirstName() + \" \" + seat.getBookedBy().getLastName() : null)")

    SeatResponse toResponseDTO(Seat seat);
}
