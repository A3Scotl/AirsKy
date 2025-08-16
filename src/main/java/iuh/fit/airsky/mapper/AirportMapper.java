package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.AirportRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.GateResponse;
import iuh.fit.airsky.model.Airport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AirportMapper {
    @Mapping(target = "airportId", ignore = true)
    Airport toEntity(AirportRequest dto);

    default AirportResponse toResponseDTO(Airport airport) {
        if (airport == null) return null;

        AirportResponse response = new AirportResponse();
        response.setAirportId(airport.getAirportId());
        response.setAirportCode(airport.getAirportCode());
        response.setAirportName(airport.getAirportName());
        response.setCity(airport.getCity());
        response.setCountry(airport.getCountry());

        if (airport.getGates() != null) {
            List<GateResponse> gates = airport.getGates().stream()
                    .map(g -> {
                        GateResponse gr = new GateResponse();
                        gr.setGateId(g.getGateId());
                        gr.setGateName(g.getGateName());
                        return gr;
                    })
                    .toList();
            response.setGates(gates);
        }

        return response;
    }
}