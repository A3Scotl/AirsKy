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

    BookingResponse toResponseDTO(Booking entity);

    // Support mapping
    default Long map(User user) {
        return user != null ? user.getId() : null;
    }
}
