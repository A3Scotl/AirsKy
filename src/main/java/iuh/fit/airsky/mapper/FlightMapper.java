package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.model.Flight;


import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {StopMapper.class, AirportMapper.class, AirlineMapper.class, AircraftMapper.class})
public abstract class FlightMapper {

    @Autowired
    protected AircraftMapper aircraftMapper;

    @Mapping(target = "businessName",
            expression = "java(entity.getBusiness() != null ? (entity.getBusiness().getBusinessName() != null ? entity.getBusiness().getBusinessName() : entity.getBusiness().getFirstName() + ' ' + entity.getBusiness().getLastName()) : null)")
    @Mapping(target = "gate", expression = "java(entity.getGate() != null ? entity.getGate().getGateName() : null)")
    @Mapping(target = "terminal", expression = "java(entity.getGate() != null ? entity.getGate().getTerminal() : null)")
    @Mapping(target = "aircraft", expression = "java(entity.getAircraft() != null ? aircraftMapper.toResponseDTO(entity.getAircraft()) : null)")
    @Mapping(target = "stops", source = "stops")
    @Mapping(target = "tripType", source = "tripType")
    @Mapping(target = "roundTripGroupId", source = "roundTripGroupId")
    @Mapping(target = "departureAirport", source = "departureAirport")
    @Mapping(target = "arrivalAirport", source = "arrivalAirport")
    @Mapping(target = "airline", source = "airline")
    @Mapping(target = "stopsList", source = "stopsList")
    public abstract FlightResponse toResponseDTO(Flight entity);

    @Mapping(target = "business.id", source = "businessId")
    @Mapping(target = "flightId", ignore = true)
    @Mapping(target = "airline", ignore = true)
    @Mapping(target = "departureAirport", ignore = true)
    @Mapping(target = "arrivalAirport", ignore = true)
    @Mapping(target = "gate", ignore = true)
    @Mapping(target = "tripType", source = "tripType")
    @Mapping(target = "roundTripGroupId", source = "roundTripGroupId")
    @Mapping(target = "stopsList", source = "stopsList")
    public abstract Flight toEntity(FlightRequest dto);

    public List<FlightResponse> toResponseDTOList(List<Flight> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
}
