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
    @Mapping(target = "passengers", expression = "java(new ArrayList<>())")
    Booking toEntity(BookingRequest dto);

    @Mapping(target = "userEmail", expression = "java(entity.getUserId() != null ? entity.getUserId().getEmail() : null)")
    @Mapping(target = "flightNumber", expression = "java(entity.getFlight() != null ? entity.getFlight().getFlightNumber() : null)")
    @Mapping(target = "travelClass", expression = "java(entity.getTravelClass() != null ? entity.getTravelClass().getClassName() : null)")
    @Mapping(target = "passengers", source = "passengers")
    @Mapping(target = "bookingCode", source = "bookingCode")
    BookingResponse toResponseDTO(Booking entity);

    List<BookingResponse> toResponseDTOList(List<Booking> bookings);

    @IterableMapping(qualifiedByName = "toPassengerSeatResponse")
    List<PassengerSeatResponse> mapPassengers(List<Passenger> passengers);
}




