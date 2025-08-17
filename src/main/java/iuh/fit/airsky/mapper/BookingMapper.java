package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "flight", ignore = true)
    @Mapping(target = "travelClass", ignore = true)
    Booking toEntity(BookingRequest dto);

    @Mapping(target = "userId", expression = "java(entity.getUserId() != null ? entity.getUserId().getId() : null)")
    @Mapping(target = "flightId", expression = "java(entity.getFlight() != null ? entity.getFlight().getFlightId() : null)")
    @Mapping(target = "classId", expression = "java(entity.getTravelClass() != null ? entity.getTravelClass().getClassId() : null)")
    BookingResponse toResponseDTO(Booking entity);

    // Support mapping
    default Long map(User user) {
        return user != null ? user.getId() : null;
    }
}
