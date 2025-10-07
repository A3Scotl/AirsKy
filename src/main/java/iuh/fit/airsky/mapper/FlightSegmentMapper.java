package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.response.FlightSegmentResponse;
import iuh.fit.airsky.dto.response.GateResponse;
import iuh.fit.airsky.model.FlightSegment;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AirportMapper.class})
public interface FlightSegmentMapper {

    @Mapping(target = "segmentOrder", source = "segmentOrder")
    @Mapping(target = "flightId", source = "flight.flightId")
    @Mapping(target = "flightNumber", source = "flight.flightNumber")
        @Mapping(target = "classId", source = "travelClass.id")
    @Mapping(target = "className", source = "travelClass.className")
    @Mapping(target = "departureAirport", source = "departureAirport")
    @Mapping(target = "arrivalAirport", source = "arrivalAirport")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "departureTime", source = "flight.departureTime")
    @Mapping(target = "arrivalTime", source = "flight.arrivalTime")
    FlightSegmentResponse toResponseDTO(FlightSegment entity);

    @AfterMapping
    default void setGateAndTerminalInfo(@MappingTarget FlightSegmentResponse response, FlightSegment entity) {
        if (response.getDepartureAirport() != null && entity.getDepartureGate() != null) {
            GateResponse departureGate = new GateResponse();
            departureGate.setGateName(entity.getDepartureGate());
            departureGate.setTerminal(entity.getDepartureTerminal());
            response.getDepartureAirport().setGates(List.of(departureGate));
        }
        if (response.getArrivalAirport() != null && entity.getArrivalGate() != null) {
            GateResponse arrivalGate = new GateResponse();
            arrivalGate.setGateName(entity.getArrivalGate());
            arrivalGate.setTerminal(entity.getArrivalTerminal());
            response.getArrivalAirport().setGates(List.of(arrivalGate));
        }
    }

    List<FlightSegmentResponse> toResponseDTOList(List<FlightSegment> entities);
}