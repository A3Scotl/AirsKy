package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.TicketRequest;
import iuh.fit.airsky.dto.response.TicketResponse;
import iuh.fit.airsky.model.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {
    @Mapping(target = "ticketId", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "passenger", ignore = true)
    Ticket toEntity(TicketRequest dto);

    TicketResponse toResponseDTO(Ticket entity);
}