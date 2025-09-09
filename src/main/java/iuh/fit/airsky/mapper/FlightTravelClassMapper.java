package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.FlightTravelClassRequest;
import iuh.fit.airsky.dto.response.FlightTravelClassResponse;
import iuh.fit.airsky.model.FlightTravelClass;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {TravelClassMapper.class})
public interface FlightTravelClassMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "flight", ignore = true)
    @Mapping(target = "travelClass", ignore = true)
    FlightTravelClass toEntity(FlightTravelClassRequest dto);

    FlightTravelClassResponse toResponseDTO(FlightTravelClass entity);
}
