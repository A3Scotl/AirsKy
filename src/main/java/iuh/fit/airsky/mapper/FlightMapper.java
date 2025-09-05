package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.model.Aircraft;
import iuh.fit.airsky.model.Flight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {StopMapper.class})
public interface FlightMapper {

    @Mapping(target = "business.id", source = "businessId")
    @Mapping(target = "flightId", ignore = true)
    @Mapping(target = "airline", ignore = true)
    @Mapping(target = "departureAirport", ignore = true)
    @Mapping(target = "arrivalAirport", ignore = true)
    @Mapping(target = "gate", ignore = true)
    @Mapping(target = "stopsList", source = "stopsList")
    Flight toEntity(FlightRequest dto);

    @Mapping(target = "businessName",
            expression = "java(entity.getBusiness() != null ? (entity.getBusiness().getBusinessName() != null ? entity.getBusiness().getBusinessName() : entity.getBusiness().getFirstName() + ' ' + entity.getBusiness().getLastName()) : null)")

    @Mapping(target = "airlineName", expression = "java(entity.getAirline() != null ? entity.getAirline().getAirlineName() : null)")
    @Mapping(target = "from", expression = "java(entity.getDepartureAirport() != null ? entity.getDepartureAirport().getAirportName() : null)")
    @Mapping(target = "to", expression = "java(entity.getArrivalAirport() != null ? entity.getArrivalAirport().getAirportName() : null)")
    @Mapping(target = "fromCode", expression = "java(entity.getDepartureAirport() != null ? entity.getDepartureAirport().getAirportCode() : null)")
    @Mapping(target = "toCode", expression = "java(entity.getArrivalAirport() != null ? entity.getArrivalAirport().getAirportCode() : null)")
    @Mapping(target = "gate", expression = "java(entity.getGate() != null ? entity.getGate().getGateName() : null)")
    @Mapping(target = "terminal", expression = "java(entity.getGate() != null ? entity.getGate().getTerminal() : null)")
    @Mapping(target = "aircraft", expression = "java(entity.getAircraft() != null ? entity.getAircraft().getAircraftName() : null)")
    @Mapping(target = "stopsList", source = "stopsList")
    FlightResponse toResponseDTO(Flight entity);

}


