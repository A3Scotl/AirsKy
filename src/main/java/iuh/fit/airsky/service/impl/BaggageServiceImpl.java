package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.BaggageRequest;
import iuh.fit.airsky.dto.response.BaggageResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.BaggageMapper;
import iuh.fit.airsky.model.Baggage;
import iuh.fit.airsky.repository.BaggageRepository;
import iuh.fit.airsky.repository.TicketRepository;
import iuh.fit.airsky.service.BaggageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class BaggageServiceImpl implements BaggageService {

    private final BaggageRepository baggageRepository;
    private final BaggageMapper baggageMapper;
    private final TicketRepository ticketRepository; // Để map quan hệ ticketId

    public BaggageServiceImpl(BaggageRepository baggageRepository, BaggageMapper baggageMapper, TicketRepository ticketRepository) {
        this.baggageRepository = baggageRepository;
        this.baggageMapper = baggageMapper;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public BaggageResponse createBaggage(BaggageRequest request) {
        log.info("Creating new baggage for ticket ID: {}", request.getTicketId());
        Baggage baggage = baggageMapper.toEntity(request);
        baggage.setTicket(ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id " + request.getTicketId())));
        Baggage saved = baggageRepository.save(baggage);
        log.info("Baggage created with ID: {}", saved.getBaggageId());
        return baggageMapper.toResponseDTO(saved);
    }

    @Override
    public BaggageResponse updateBaggage(Long id, BaggageRequest request) {
        log.info("Updating baggage with ID: {}", id);
        Baggage baggage = baggageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Baggage not found with id " + id));
        baggage.setWeight(request.getWeight());
        baggage.setType(request.getType());
        baggage.setAllowance(request.getAllowance());
        Baggage updated = baggageRepository.save(baggage);
        log.info("Baggage updated with ID: {}", updated.getBaggageId());
        return baggageMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<BaggageResponse> findById(Long id) {
        log.info("Finding baggage by ID: {}", id);
        return baggageRepository.findById(id).map(baggageMapper::toResponseDTO);
    }

    @Override
    public PageResponse<BaggageResponse> findAll(Pageable pageable) {
        log.info("Finding all baggage with pagination: {}", pageable);
        Page<Baggage> page = baggageRepository.findAll(pageable);
        return new PageResponse<>(page.map(baggageMapper::toResponseDTO));
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting baggage with ID: {}", id);
        if (!baggageRepository.existsById(id)) {
            log.warn("Baggage not found for delete: {}", id);
            throw new ResourceNotFoundException("Baggage not found with id " + id);
        }
        baggageRepository.deleteById(id);
        log.info("Baggage deleted: {}", id);
    }
}