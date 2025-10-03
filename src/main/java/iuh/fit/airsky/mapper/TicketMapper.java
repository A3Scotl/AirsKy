package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.TicketRequest;
import iuh.fit.airsky.dto.response.TicketResponse;
import iuh.fit.airsky.model.CheckIn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {
    @Mapping(target = "checkInId", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "passenger", ignore = true)
    CheckIn toEntity(TicketRequest dto);

    TicketResponse toResponseDTO(CheckIn entity);
}