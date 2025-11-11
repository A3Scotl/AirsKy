package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.PassengerSeatRequest;
import iuh.fit.airsky.dto.response.PassengerResponse;
import iuh.fit.airsky.dto.response.PassengerSeatResponse;
import iuh.fit.airsky.dto.response.PassengerSeatAssignmentResponse;
import iuh.fit.airsky.model.Passenger;
import iuh.fit.airsky.model.PassengerSeatAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PassengerMapper {

    @Mapping(target = "passengerId", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "seatAssignments", ignore = true) // Sẽ được set riêng trong service
    @Mapping(target = "tempBaggagePackage", ignore = true)
    Passenger toEntity(PassengerSeatRequest dto);

    PassengerResponse toResponseDTO(Passenger entity);

    @Named("toPassengerSeatResponse")
    @Mapping(target = "seatAssignments", expression = "java(mapSeatAssignments(passenger.getSeatAssignments()))")
    @Mapping(target = "className", expression = "java(!passenger.getSeatAssignments().isEmpty() && passenger.getSeatAssignments().get(0).getSeat().getTravelClass() != null ? passenger.getSeatAssignments().get(0).getSeat().getTravelClass().getClassName() : null)")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "nationality", source = "nationality")
    @Mapping(target = "currentResidence", source = "currentResidence")
    PassengerSeatResponse toPassengerSeatResponse(Passenger passenger);

    @Mapping(target = "segmentOrder", expression = "java(assignment.getFlightSegment().getSegmentOrder())")
    @Mapping(target = "seatNumber", expression = "java(assignment.getSeat().getSeatNumber())")
    @Mapping(target = "seatType", expression = "java(assignment.getSeat().getType() != null ? assignment.getSeat().getType().name() : null)")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "flightNumber", expression = "java(assignment.getFlightSegment().getFlight().getFlightNumber())")
    @Mapping(target = "departureAirport", expression = "java(assignment.getFlightSegment().getDepartureAirport().getAirportCode())")
    @Mapping(target = "arrivalAirport", expression = "java(assignment.getFlightSegment().getArrivalAirport().getAirportCode())")
    PassengerSeatAssignmentResponse toSeatAssignmentResponse(PassengerSeatAssignment assignment);

    default List<PassengerSeatAssignmentResponse> mapSeatAssignments(List<PassengerSeatAssignment> assignments) {
        if (assignments == null) return null;
        return assignments.stream()
                .map(this::toSeatAssignmentResponse)
                .toList();
    }

}
