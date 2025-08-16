package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.AirportRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.model.Airport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AirportMapper {
    @Mapping(target = "airportId", ignore = true)
    Airport toEntity(AirportRequest dto);

    AirportResponse toResponseDTO(Airport entity);
}