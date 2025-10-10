package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.response.BaggageResponse;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.PassengerSeatResponse;
import iuh.fit.airsky.model.Baggage;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.CheckIn;
import iuh.fit.airsky.model.Passenger;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = {PassengerMapper.class, FlightSegmentMapper.class, PaymentMapper.class})
public interface BookingMapper {

    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "flight", ignore = true)
    @Mapping(target = "travelClass", ignore = true)
    @Mapping(target = "passengers", expression = "java(new ArrayList<>())")
    @Mapping(target = "bookingCode", ignore = true)
    @Mapping(target = "payment", ignore = true)
    @Mapping(target = "checkIns", ignore = true)
    @Mapping(target = "flightSegments", ignore = true)
    Booking toEntity(BookingRequest dto);

    @Mapping(target = "userEmail", expression = "java(entity.getUserId() != null ? entity.getUserId().getEmail() : (entity.getPassengers() != null && !entity.getPassengers().isEmpty() ? entity.getPassengers().get(0).getEmail() : null))")
    @Mapping(target = "flightNumber", expression = "java(entity.getFlight() != null ? entity.getFlight().getFlightNumber() : null)")
    @Mapping(target = "travelClass", expression = "java(entity.getTravelClass() != null ? entity.getTravelClass().getClassName() : null)")
    @Mapping(target = "passengers", source = "passengers")
    @Mapping(target = "bookingCode", source = "bookingCode")
    @Mapping(target = "baggage", ignore = true)
    @Mapping(target = "bookingId", source = "bookingId")
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "flightSegments", source = "flightSegments")
    @Mapping(target = "appliedDealCode", ignore = true)
    @Mapping(target = "discountPercentage", ignore = true)
    @Mapping(target = "discountAmount", ignore = true)
    @Mapping(target = "ancillaryServices", ignore = true)
    @Mapping(target = "ancillaryServicesAmount", ignore = true)
    @Mapping(target = "seatTypeAmount", ignore = true)
    @Mapping(target = "seatTypeDetails", ignore = true)
    BookingResponse toResponseDTO(Booking entity);

    List<BookingResponse> toResponseDTOList(List<Booking> bookings);

        @IterableMapping(qualifiedByName = "toPassengerSeatResponse")
    List<PassengerSeatResponse> mapPassengers(List<Passenger> passengers);


}




