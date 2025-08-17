package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.PassengerSeatResponse;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Passenger;
import iuh.fit.airsky.model.User;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", uses = PassengerMapper.class)
public interface BookingMapper {

    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "flight", ignore = true)
    @Mapping(target = "travelClass", ignore = true)
    @Mapping(target = "passengers", ignore = true)
    Booking toEntity(BookingRequest dto);

    @Mapping(target = "userId", expression = "java(entity.getUserId() != null ? entity.getUserId().getId() : null)")
    @Mapping(target = "flightId", expression = "java(entity.getFlight() != null ? entity.getFlight().getFlightId() : null)")
    @Mapping(target = "classId", expression = "java(entity.getTravelClass() != null ? entity.getTravelClass().getClassId() : null)")
    @Mapping(target = "passengers", source = "passengers")
    BookingResponse toResponseDTO(Booking entity);

    @IterableMapping(qualifiedByName = "toPassengerSeatResponse")
    List<PassengerSeatResponse> mapPassengers(List<Passenger> passengers);
}

