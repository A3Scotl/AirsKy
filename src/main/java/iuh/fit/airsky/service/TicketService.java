package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.TicketRequest;
import iuh.fit.airsky.dto.response.TicketResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TicketService {
    TicketResponse createTicket(TicketRequest request);
    TicketResponse updateTicket(Long id, TicketRequest request);
    Optional<TicketResponse> findById(Long id);
    PageResponse<TicketResponse> findAll(Pageable pageable);
    void softDelete(Long id);
}