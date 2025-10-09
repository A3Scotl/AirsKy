package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.CheckinRequest;
import iuh.fit.airsky.dto.response.CheckinResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.CheckinMapper;
import iuh.fit.airsky.model.CheckIn;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.repository.CheckinRepository;
import iuh.fit.airsky.repository.PassengerRepository;
import iuh.fit.airsky.service.BoardingPassService;
import iuh.fit.airsky.service.CheckinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;
    private final CheckinMapper checkinMapper;
    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final BoardingPassService boardingPassService;

    public CheckinServiceImpl(CheckinRepository checkinRepository, CheckinMapper checkinMapper,
                            BookingRepository bookingRepository, PassengerRepository passengerRepository,
                            BoardingPassService boardingPassService) {
        this.checkinRepository = checkinRepository;
        this.checkinMapper = checkinMapper;
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
        this.boardingPassService = boardingPassService;
    }

    @Override
    public CheckinResponse createCheckin(CheckinRequest request) {
        log.info("Creating new checkin for booking ID: {} and passenger ID: {}", request.getBookingId(), request.getPassengerId());

        // Validate booking exists and is paid
        var booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + request.getBookingId()));

        if (booking.getPayment() == null || !"COMPLETED".equals(booking.getPayment().getStatus())) {
            throw new IllegalStateException("Booking must be paid before check-in");
        }

        // Validate passenger belongs to booking
        var passenger = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id " + request.getPassengerId()));

        if (!passenger.getBooking().getBookingId().equals(request.getBookingId())) {
            throw new IllegalStateException("Passenger does not belong to this booking");
        }

        // Check if already checked in
        if (checkinRepository.existsByPassenger(passenger)) {
            throw new IllegalStateException("Passenger is already checked in");
        }

        // Validate seat is assigned and available
        if (passenger.getSeat() == null) {
            throw new IllegalStateException("Seat must be assigned before check-in");
        }

        // Create check-in record
        CheckIn checkIn = CheckIn.builder()
                .booking(booking)
                .passenger(passenger)
                .seatNumber(passenger.getSeat().getSeatNumber())
                .ticketPrice(request.getTicketPrice() != null ? request.getTicketPrice() : BigDecimal.ZERO)
                .checkedAt(LocalDateTime.now())
                .checkInType(iuh.fit.airsky.enums.CheckInType.ONLINE)
                .build();

        CheckIn saved = checkinRepository.save(checkIn);

        // Generate boarding pass PDF and send email
        String boardingPassUrl = boardingPassService.generateAndSendBoardingPass(saved);
        saved.setBoardingPassUrl(boardingPassUrl);

        // Update with boarding pass URL
        CheckIn updated = checkinRepository.save(saved);

        log.info("Checkin created with ID: {} and boarding pass URL: {}", updated.getCheckInId(), boardingPassUrl);

        return checkinMapper.toResponseDTO(updated);
    }

    @Override
    public CheckinResponse updateCheckin(Long id, CheckinRequest request) {
        log.info("Updating checkin with ID: {}", id);
        CheckIn checkIn = checkinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found with id " + id));
        checkIn.setSeatNumber(request.getSeatNumber());
        checkIn.setTicketPrice(request.getTicketPrice());
        // checkedAt is not updated during checkin update
        CheckIn updated = checkinRepository.save(checkIn);
        log.info("Checkin updated with ID: {}", updated.getCheckInId());
        return checkinMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<CheckinResponse> findById(Long id) {
        log.info("Finding checkin by ID: {}", id);
        return checkinRepository.findById(id).map(checkinMapper::toResponseDTO);
    }

    @Override
    public PageResponse<CheckinResponse> findAll(Pageable pageable) {
        log.info("Finding all checkins with pagination: {}", pageable);
        Page<CheckIn> page = checkinRepository.findAll(pageable);
        return new PageResponse<>(page.map(checkinMapper::toResponseDTO));
    }

    @Override
    public void softDelete(Long id) {
        log.info("Soft deleting checkin with ID: {}", id);
        if (checkinRepository.findById(id).isEmpty()) {
            log.warn("Checkin not found for soft delete: {}", id);
            throw new ResourceNotFoundException("Checkin not found with id " + id);
        }
        checkinRepository.softDeleteById(id, LocalDateTime.now());
        log.info("Checkin soft deleted: {}", id);
    }


}