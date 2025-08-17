package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.GateRequest;
import iuh.fit.airsky.dto.response.GateResponse;
import iuh.fit.airsky.model.Gate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GateMapper {
    @Mapping(target = "gateId", ignore = true)
    @Mapping(target = "airport", ignore = true)
    Gate toEntity(GateRequest dto);

    GateResponse toResponseDTO(Gate entity);
}