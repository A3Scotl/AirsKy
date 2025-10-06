package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.PassengerSeatRequest;
import iuh.fit.airsky.dto.response.PassengerResponse;
import iuh.fit.airsky.dto.response.PassengerSeatResponse;
import iuh.fit.airsky.model.Passenger;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PassengerMapper {

    @Mapping(target = "passengerId", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "seat", ignore = true)
    @Mapping(target = "tempBaggagePackage", ignore = true)
    Passenger toEntity(PassengerSeatRequest dto);

    PassengerResponse toResponseDTO(Passenger entity);

    @Named("toPassengerSeatResponse")
    @Mapping(target = "seatNumber", expression = "java(passenger.getSeat() != null ? passenger.getSeat().getSeatNumber() : null)")
    @Mapping(target = "className", expression = "java(passenger.getSeat() != null && passenger.getSeat().getTravelClass() != null ? passenger.getSeat().getTravelClass().getClassName() : null)")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    PassengerSeatResponse toPassengerSeatResponse(Passenger passenger);

}
