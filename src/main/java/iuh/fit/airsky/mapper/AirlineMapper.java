package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.AirlineRequest;
import iuh.fit.airsky.dto.response.AirlineResponse;
import iuh.fit.airsky.model.Airline;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AirlineMapper {
    @Mapping(target = "airlineId", ignore = true)
    Airline toEntity(AirlineRequest dto);

    AirlineResponse toResponseDTO(Airline entity);
}