package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.AirportRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.GateResponse;
import iuh.fit.airsky.model.Airport;

import iuh.fit.airsky.model.Country;
import iuh.fit.airsky.model.Gate;
import iuh.fit.airsky.repository.CountryRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AirportMapper {

    @Mapping(target = "airportId", ignore = true)
    @Mapping(target = "cityNames", source = "cityNames")
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "gates", ignore = true)
    @Mapping(target = "thumbnail", source = "thumbnail")
    default Airport toEntity(AirportRequest dto) {
        Airport airport = new Airport();
        airport.setAirportCode(dto.getAirportCode());
        airport.setAirportName(dto.getAirportName());
        airport.setThumbnail(dto.getThumbnail());
        airport.setActive(dto.getActive());
        airport.setCityNames(dto.getCityNames());
        return airport;
    }

    @Mapping(target = "country", expression = "java(airport.getCountry() != null ? airport.getCountry().getCountryName() : null)")
    @Mapping(target = "gates", expression = "java(mapGates(airport.getGates()))")
    @Mapping(target = "thumbnail", source = "thumbnail")
    default AirportResponse toResponseDTO(Airport airport) {
        AirportResponse response = new AirportResponse();
        response.setAirportId(airport.getAirportId());
        response.setAirportCode(airport.getAirportCode());
        response.setAirportName(airport.getAirportName());
        response.setCountry(airport.getCountry() != null ? airport.getCountry().getCountryName() : null);
        response.setThumbnail(airport.getThumbnail());
        response.setActive(airport.isActive());
        response.setCityNames(airport.getCityNames());
        response.setGates(mapGates(airport.getGates()));
        return response;
    }

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
