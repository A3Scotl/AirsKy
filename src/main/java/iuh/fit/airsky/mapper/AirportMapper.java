package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.AirportRequest;
import iuh.fit.airsky.dto.request.GateRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.GateResponse;
import iuh.fit.airsky.model.Airport;
import iuh.fit.airsky.model.Gate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface AirportMapper {

    @Mapping(target = "airportId", ignore = true)
    @Mapping(target = "cityNames", source = "cityNames")
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "gates", ignore = true)
    @Mapping(target = "thumbnail", source = "thumbnailUrl")
    default Airport toEntity(AirportRequest dto) {
        Airport airport = new Airport();
        airport.setAirportCode(dto.getAirportCode());
        airport.setAirportName(dto.getAirportName());
        airport.setThumbnail(dto.getThumbnailUrl());
        // Set active: nếu null thì mặc định true
        airport.setActive(dto.getActive() != null ? dto.getActive() : true);
        airport.setCityNames(dto.getCityNames());
        // Map gates from request
        if (dto.getGates() != null && !dto.getGates().isEmpty()) {
            List<Gate> gates = new ArrayList<>();
            for (GateRequest gateRequest : dto.getGates()) {
                Gate gate = new Gate();
                gate.setGateName(gateRequest.getGateName());
                gate.setTerminal(gateRequest.getTerminal());
                gate.setAirport(airport); // Gán airport cho gate
                gates.add(gate);
            }
            airport.setGates(gates);
        }
        return airport;
    }

    default Airport toEntity(AirportRequest dto, String imageUrl) {
        Airport airport = new Airport();
        airport.setAirportCode(dto.getAirportCode());
        airport.setAirportName(dto.getAirportName());
        // Ưu tiên imageUrl (file upload), nếu không có thì lấy thumbnailUrl (url do client nhập)
        if (imageUrl != null && !imageUrl.isEmpty()) {
            airport.setThumbnail(imageUrl);
        } else {
            airport.setThumbnail(dto.getThumbnailUrl());
        }
        airport.setActive(dto.getActive());
        airport.setCityNames(dto.getCityNames());
        // Map gates from request
        if (dto.getGates() != null && !dto.getGates().isEmpty()) {
            List<Gate> gates = new ArrayList<>();
            for (GateRequest gateRequest : dto.getGates()) {
                Gate gate = new Gate();
                gate.setGateName(gateRequest.getGateName());
                gate.setTerminal(gateRequest.getTerminal());
                gate.setAirport(airport); // Gán airport cho gate
                gates.add(gate);
            }
            airport.setGates(gates);
        }
        return airport;
    }

    @Mapping(target = "country", expression = "java(airport != null && airport.getCountry() != null ? airport.getCountry().getCountryName() : null)")
    @Mapping(target = "gates", expression = "java(airport != null ? mapGates(airport.getGates()) : null)")
    @Mapping(target = "thumbnail", source = "thumbnail")
    default AirportResponse toResponseDTO(Airport airport) {
        if (airport == null) {
            return null;
        }
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

    // Airport mapping without gates for booking contexts to avoid lazy loading issues
    @Mapping(target = "country", expression = "java(airport.getCountry() != null ? airport.getCountry().getCountryName() : null)")
    @Mapping(target = "gates", ignore = true)
    @Mapping(target = "thumbnail", source = "thumbnail")
    @Named("toResponseDTOWithoutGates")
    AirportResponse toResponseDTOWithoutGates(Airport airport);

    default List<AirportResponse> toResponseDTOList(List<Airport> airports) {
        if (airports == null) return null;
        return airports.stream().map(this::toResponseDTO).toList();
    }

    // Map List<Gate> -> List<GateResponse>
    default List<GateResponse> mapGates(List<Gate> gates) {
        if (gates == null) return null;
        return gates.stream().map(g -> {
            GateResponse gr = new GateResponse();
            gr.setGateId(g.getGateId());
            gr.setGateName(g.getGateName());
            gr.setTerminal(g.getTerminal());
//            gr.setAirportId(
//                    g.getAirport() != null ? g.getAirport().getAirportId() : null
//            );
            return gr;
        }).toList();
    }

}
