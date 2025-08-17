package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.BaggageRequest;
import iuh.fit.airsky.dto.response.BaggageResponse;
import iuh.fit.airsky.model.Baggage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BaggageMapper {
    @Mapping(target = "baggageId", ignore = true)
    @Mapping(target = "ticket", ignore = true) // Map riêng trong service
    Baggage toEntity(BaggageRequest dto);

    BaggageResponse toResponseDTO(Baggage entity);
}