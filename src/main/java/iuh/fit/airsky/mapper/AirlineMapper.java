package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.AirlineRequest;
import iuh.fit.airsky.dto.response.AirlineResponse;
import iuh.fit.airsky.model.Airline;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AirlineMapper {
    @Mapping(target = "airlineId", ignore = true)
    @Mapping(target = "thumbnail", source = "thumbnail")
    Airline toEntity(AirlineRequest dto);

    @Mapping(target = "thumbnail", source = "thumbnail")
    AirlineResponse toResponseDTO(Airline entity);

    List<AirlineResponse> toResponseDTOList(List<Airline> airlines);
}