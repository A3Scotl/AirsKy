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
import iuh.fit.airsky.service.CheckinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;
    private final CheckinMapper checkinMapper;
    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;

    public CheckinServiceImpl(CheckinRepository checkinRepository, CheckinMapper checkinMapper, BookingRepository bookingRepository, PassengerRepository passengerRepository) {
        this.checkinRepository = checkinRepository;
        this.checkinMapper = checkinMapper;
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
    }

    @Override
    public CheckinResponse createCheckin(CheckinRequest request) {
        log.info("Creating new checkin for booking ID: {}", request.getBookingId());
        CheckIn checkIn = checkinMapper.toEntity(request);
        checkIn.setBooking(bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + request.getBookingId())));
        checkIn.setPassenger(passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id " + request.getPassengerId())));
        CheckIn saved = checkinRepository.save(checkIn);
        log.info("Checkin created with ID: {}", saved.getCheckInId());
        return checkinMapper.toResponseDTO(saved);
    }

    @Override
    public CheckinResponse updateCheckin(Long id, CheckinRequest request) {
        log.info("Updating checkin with ID: {}", id);
        CheckIn checkIn = checkinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found with id " + id));
        checkIn.setSeatNumber(request.getSeatNumber());
        checkIn.setTicketPrice(request.getTicketPrice());
        checkIn.setCheckedAt(request.getIssueDate());
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