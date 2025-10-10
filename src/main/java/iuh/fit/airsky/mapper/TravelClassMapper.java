package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.TravelClassRequest;
import iuh.fit.airsky.dto.response.TravelClassResponse;
import iuh.fit.airsky.model.TravelClass;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TravelClassMapper {
    @Mapping(target = "id", ignore = true)
    TravelClass toEntity(TravelClassRequest dto);

    TravelClassResponse toResponseDTO(TravelClass entity);
}