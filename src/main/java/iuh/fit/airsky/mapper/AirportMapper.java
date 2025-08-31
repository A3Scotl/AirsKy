package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.AirportRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.GateResponse;
import iuh.fit.airsky.model.Airport;

import iuh.fit.airsky.model.Gate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AirportMapper {
    @Mapping(target = "airportId", ignore = true)
    @Mapping(target = "cityNames", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "gates", ignore = true)
    @Mapping(target = "thumbnail", source = "thumbnail")
    Airport toEntity(AirportRequest dto);

    @Mapping(target = "country", expression = "java(airport.getCountry() != null ? airport.getCountry().getCountryName() : null)")
    @Mapping(target = "gates", expression = "java(mapGates(airport.getGates()))")
    @Mapping(target = "thumbnail", source = "thumbnail")
    AirportResponse toResponseDTO(Airport airport);

    // Map List<Gate> -> List<GateResponse>
    default List<GateResponse> mapGates(List<Gate> gates) {
        if (gates == null) return null;
        return gates.stream().map(g -> {
            GateResponse gr = new GateResponse();
            gr.setGateId(g.getGateId());
            gr.setGateName(g.getGateName());
//            gr.setAirportId(
//                    g.getAirport() != null ? g.getAirport().getAirportId() : null
//            );
            return gr;
        }).toList();
    }

}

