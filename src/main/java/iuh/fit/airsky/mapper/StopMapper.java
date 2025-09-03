package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.StopRequest;
import iuh.fit.airsky.dto.response.StopResponse;
import iuh.fit.airsky.model.Airport;
import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.model.Stop;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StopMapper {

    @Mapping(target = "stopId", ignore = true)
    @Mapping(target = "flight", source = "flight")
    @Mapping(target = "airport", expression = "java(toAirport(request.getAirportId()))")
    @Mapping(target = "arrivalTime", source = "request.arrivalTime")
    @Mapping(target = "departureTime", source = "request.departureTime")
    @Mapping(target = "note", source = "request.note")
    Stop toEntity(StopRequest request, Flight flight);

    @Mapping(target = "flightId", source = "flight.flightId")
    @Mapping(target = "airportId", source = "airport.airportId")
    @Mapping(target = "airportName", source = "airport.airportName") // sửa lại đúng field của Airport
    StopResponse toResponse(Stop stop);

    // helper method
    default Airport toAirport(Long airportId) {
        if (airportId == null) return null;
        Airport airport = new Airport();
        airport.setAirportId(airportId);
        return airport;
    }
}

