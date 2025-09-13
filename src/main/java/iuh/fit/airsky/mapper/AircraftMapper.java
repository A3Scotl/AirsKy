package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.AircraftRequest;
import iuh.fit.airsky.dto.response.AircraftResponse;
import iuh.fit.airsky.model.Aircraft;

import java.util.List;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AircraftMapper {

    AircraftResponse toResponseDTO(Aircraft aircraft);

     List<AircraftResponse> toResponseDTOList(List<Aircraft> aircrafts);

    Aircraft toEntity(AircraftRequest request);
}
