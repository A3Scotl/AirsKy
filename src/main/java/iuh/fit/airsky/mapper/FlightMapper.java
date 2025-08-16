package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.model.Flight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FlightMapper {
    @Mapping(target = "flightId", ignore = true)
    @Mapping(target = "airline", ignore = true)
    @Mapping(target = "departureAirport", ignore = true)
    @Mapping(target = "arrivalAirport", ignore = true)
    @Mapping(target = "gate", ignore = true)
    Flight toEntity(FlightRequest dto);

    FlightResponse toResponseDTO(Flight entity);
}