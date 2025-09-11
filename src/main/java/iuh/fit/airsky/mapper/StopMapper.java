package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.StopRequest;
import iuh.fit.airsky.dto.response.StopResponse;
import iuh.fit.airsky.model.Stop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StopMapper {

    @Mapping(target = "flight", ignore = true)
    @Mapping(target = "airport", ignore = true)
    @Mapping(target = "stopId", ignore = true)
    @Mapping(target = "stopDuration", ignore = true)
    Stop toEntity(StopRequest dto);

    @Mapping(target = "airportName", expression = "java(entity.getAirport() != null ? entity.getAirport().getAirportName() : null)")
    @Mapping(target = "airportCode", expression = "java(entity.getAirport() != null ? entity.getAirport().getAirportCode() : null)")
    @Mapping(target = "airportId", expression = "java(entity.getAirport() != null ? entity.getAirport().getAirportId() : null)")
    StopResponse toResponseDTO(Stop entity);
}