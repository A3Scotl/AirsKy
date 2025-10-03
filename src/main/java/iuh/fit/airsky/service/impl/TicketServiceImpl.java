package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.TicketRequest;
import iuh.fit.airsky.dto.response.TicketResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.TicketMapper;
import iuh.fit.airsky.model.CheckIn;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.repository.PassengerRepository;
import iuh.fit.airsky.repository.TicketRepository;
import iuh.fit.airsky.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;

    public TicketServiceImpl(TicketRepository ticketRepository, TicketMapper ticketMapper, BookingRepository bookingRepository, PassengerRepository passengerRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
    }

    @Override
    public TicketResponse createTicket(TicketRequest request) {
        log.info("Creating new ticket for booking ID: {}", request.getBookingId());
        CheckIn checkIn = ticketMapper.toEntity(request);
        checkIn.setBooking(bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + request.getBookingId())));
        checkIn.setPassenger(passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id " + request.getPassengerId())));
        CheckIn saved = ticketRepository.save(checkIn);
        log.info("Ticket created with ID: {}", saved.getCheckInId());
        return ticketMapper.toResponseDTO(saved);
    }

    @Override
    public TicketResponse updateTicket(Long id, TicketRequest request) {
        log.info("Updating ticket with ID: {}", id);
        CheckIn checkIn = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id " + id));
        checkIn.setSeatNumber(request.getSeatNumber());
        checkIn.setTicketPrice(request.getTicketPrice());
        checkIn.setCheckedAt(request.getIssueDate());
        CheckIn updated = ticketRepository.save(checkIn);
        log.info("Ticket updated with ID: {}", updated.getCheckInId());
        return ticketMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<TicketResponse> findById(Long id) {
        log.info("Finding ticket by ID: {}", id);
        return ticketRepository.findById(id).map(ticketMapper::toResponseDTO);
    }

    @Override
    public PageResponse<TicketResponse> findAll(Pageable pageable) {
        log.info("Finding all tickets with pagination: {}", pageable);
        Page<CheckIn> page = ticketRepository.findAll(pageable);
        return new PageResponse<>(page.map(ticketMapper::toResponseDTO));
    }

    @Override
    public void softDelete(Long id) {
        log.info("Soft deleting ticket with ID: {}", id);
        if (ticketRepository.findById(id).isEmpty()) {
            log.warn("Ticket not found for soft delete: {}", id);
            throw new ResourceNotFoundException("Ticket not found with id " + id);
        }
        ticketRepository.softDeleteById(id, LocalDateTime.now());
        log.info("Ticket soft deleted: {}", id);
    }
}