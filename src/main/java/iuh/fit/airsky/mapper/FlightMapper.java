package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.model.Flight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FlightMapper {

    @Mapping(target = "business", ignore = true)
    @Mapping(target = "flightId", ignore = true)
    @Mapping(target = "airline", ignore = true)
    @Mapping(target = "departureAirport", ignore = true)
    @Mapping(target = "arrivalAirport", ignore = true)
    @Mapping(target = "gate", ignore = true)
    Flight toEntity(FlightRequest dto);

    @Mapping(target = "businessName",
            expression = "java(entity.getBusiness() != null ? (entity.getBusiness().getBusinessName() != null ? entity.getBusiness().getBusinessName() : entity.getBusiness().getFirstName() + ' ' + entity.getBusiness().getLastName()) : null)")

    @Mapping(target = "airlineName", expression = "java(entity.getAirline() != null ? entity.getAirline().getAirlineName() : null)")
    @Mapping(target = "departureAirportName", expression = "java(entity.getDepartureAirport() != null ? entity.getDepartureAirport().getAirportName() : null)")
    @Mapping(target = "arrivalAirportName", expression = "java(entity.getArrivalAirport() != null ? entity.getArrivalAirport().getAirportName() : null)")
    @Mapping(target = "gateName", expression = "java(entity.getGate() != null ? entity.getGate().getGateName() : null)")
    FlightResponse toResponseDTO(Flight entity);
}


