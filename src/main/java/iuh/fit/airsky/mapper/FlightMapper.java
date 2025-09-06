package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.model.Aircraft;
import iuh.fit.airsky.model.Flight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {StopMapper.class, AirportMapper.class, AirlineMapper.class})
public interface FlightMapper {

    @Mapping(target = "business.id", source = "businessId")
    @Mapping(target = "flightId", ignore = true)
    @Mapping(target = "gate", ignore = true)
    @Mapping(target = "tripType", source = "tripType")
    @Mapping(target = "roundTripGroupId", source = "roundTripGroupId")
    Flight toEntity(FlightRequest dto);

    @Mapping(target = "aircraft", expression = "java(entity.getAircraft() != null ? entity.getAircraft().getAircraftName() : null)")
    @Mapping(target = "totalSeats", expression = "java(entity.getAircraft() != null ? entity.getAircraft().getTotalSeats() : null)")
    @Mapping(target = "stops", source = "stops")
    @Mapping(target = "tripType", source = "tripType")
    @Mapping(target = "roundTripGroupId", source = "roundTripGroupId")
    @Mapping(target = "gate", expression = "java(entity.getGate() != null ? entity.getGate().getGateName() : null)")
    @Mapping(target = "terminal", expression = "java(entity.getGate() != null ? entity.getGate().getTerminal() : null)")
    @Mapping(target = "businessName", expression = "java(entity.getBusiness() != null ? (entity.getBusiness().getBusinessName() != null ? entity.getBusiness().getBusinessName() : entity.getBusiness().getFirstName() + ' ' + entity.getBusiness().getLastName()) : null)")
    @Mapping(target = "departureAirport", source = "departureAirport")
    @Mapping(target = "arrivalAirport", source = "arrivalAirport")
    @Mapping(target = "airline", source = "airline")
    FlightResponse toResponseDTO(Flight entity);

}
