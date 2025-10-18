package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.CheckinRequest;
import iuh.fit.airsky.dto.response.CheckinResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.CheckinStatus;
import iuh.fit.airsky.enums.PaymentStatus;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.CheckinMapper;
import iuh.fit.airsky.model.CheckIn;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.repository.CheckinRepository;
import iuh.fit.airsky.repository.PassengerRepository;
import iuh.fit.airsky.service.BoardingPassService;
import iuh.fit.airsky.service.CheckinService;
import iuh.fit.airsky.service.NotificationService; 
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

    private static final int CHECKIN_WINDOW_START_HOURS_BEFORE_DEPARTURE = 24;
    private static final int CHECKIN_WINDOW_CLOSE_HOURS_BEFORE_DEPARTURE = 1;

    private final CheckinRepository checkinRepository;
    private final CheckinMapper checkinMapper;
    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final BoardingPassService boardingPassService;
    private final NotificationService notificationService; // Thêm NotificationService

    public CheckinServiceImpl(CheckinRepository checkinRepository, CheckinMapper checkinMapper,
                            BookingRepository bookingRepository, PassengerRepository passengerRepository,
                            BoardingPassService boardingPassService, NotificationService notificationService) {
        this.checkinRepository = checkinRepository;
        this.checkinMapper = checkinMapper;
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
        this.boardingPassService = boardingPassService;
        this.notificationService = notificationService;
    }

    @Override
    public CheckinResponse createCheckin(CheckinRequest request) {
        log.info("Creating new checkin for booking ID: {} and passenger ID: {}", request.getBookingId(), request.getPassengerId());

        // Validate booking exists and is paid
        var booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + request.getBookingId()));

        // **THÊM: Validate booking status - phải là CONFIRMED hoặc COMPLETED**
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking must be confirmed before check-in. Current status: " + booking.getStatus());
        }

        // Validate payment status
        if (booking.getPayment() == null || booking.getPayment().getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Booking must be paid before check-in");
        }

        // **THÊM: Validate flight timing for check-in**
        if (!isCheckinWindowOpen(booking)) {
            throw new IllegalStateException("Check-in not available for this flight at this time");
        }

        // Validate passenger belongs to booking
        var passenger = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id " + request.getPassengerId()));

        if (!passenger.getBooking().getBookingId().equals(request.getBookingId())) {
            throw new IllegalStateException("Passenger does not belong to this booking");
        }

        // Check if already checked in (COMPLETED status)
        if (checkinRepository.existsByPassengerAndCompleted(passenger)) {
            throw new IllegalStateException("Passenger is already checked in");
        }

        // Find existing CheckIn record (should be PENDING status)
        Optional<CheckIn> existingCheckInOpt = checkinRepository.findByBookingIdWithBaggage(booking.getBookingId()).stream()
                .filter(ci -> ci.getPassenger().equals(passenger))
                .findFirst();

        if (existingCheckInOpt.isEmpty()) {
            throw new IllegalStateException("CheckIn record not found for passenger");
        }

        CheckIn checkIn = existingCheckInOpt.get();

        // Validate that the existing checkin is in PENDING status
        if (checkIn.getStatus() != CheckinStatus.PENDING) {
            throw new IllegalStateException("CheckIn is not in pending status");
        }

        // Update check-in record with COMPLETED status and current timestamp
        checkIn.setStatus(CheckinStatus.COMPLETED);
        checkIn.setCheckedAt(LocalDateTime.now());
        checkIn.setTicketPrice(request.getTicketPrice() != null ? request.getTicketPrice() : BigDecimal.ZERO);
        checkIn.setCheckInType(iuh.fit.airsky.enums.CheckInType.ONLINE);

        CheckIn saved = checkinRepository.save(checkIn);

        // Generate boarding pass PDF and send email
        try {
            // Flush to ensure all changes are persisted before generating boarding pass
            checkinRepository.flush();

            String boardingPassUrl = boardingPassService.generateAndSendBoardingPass(saved);
            log.info("Generated boarding pass URL: {}", boardingPassUrl);
            if (boardingPassUrl != null && !boardingPassUrl.isEmpty()) {
                saved.setBoardingPassUrl(boardingPassUrl);
                log.info("Setting boardingPassUrl on checkIn entity: {}", boardingPassUrl);
                saved = checkinRepository.save(saved);
                log.info("Saved checkIn with boardingPassUrl: {}, entity boardingPassUrl: {}", boardingPassUrl, saved.getBoardingPassUrl());
                log.info("Checkin created with ID: {} and boarding pass URL: {}", saved.getCheckInId(), boardingPassUrl);
            } else {
                log.warn("Boarding pass URL is null or empty for check-in {}", saved.getCheckInId());
            }

            // GỬI THÔNG BÁO SOCKET
            if (booking.getUserId() != null) {
                String message = String.format("Bạn đã check-in thành công cho chuyến bay %s. Boarding pass đã được gửi đến email của bạn.", booking.getFlight().getFlightNumber());
                notificationService.sendNotificationToUserWithRelatedId(
                    booking.getUserId().getId(),
                    "CHECKIN_SUCCESSFUL",
                    message,
                    saved.getCheckInId()
                );
            }
        } catch (Exception e) {
            log.error("Failed to generate boarding pass for check-in {}: {}", saved.getCheckInId(), e.getMessage(), e);
            // Continue without boarding pass - checkin is still successful
            log.info("Checkin created with ID: {} (boarding pass generation failed)", saved.getCheckInId());
        }

        return checkinMapper.toResponseDTO(saved);
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

    private boolean isCheckinWindowOpen(Booking booking) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departureTime = booking.getFlight().getDepartureTime();
        LocalDateTime checkinStartTime = departureTime.minusHours(CHECKIN_WINDOW_START_HOURS_BEFORE_DEPARTURE);
        LocalDateTime checkinEndTime = departureTime.minusHours(CHECKIN_WINDOW_CLOSE_HOURS_BEFORE_DEPARTURE);

        return now.isAfter(checkinStartTime) && now.isBefore(checkinEndTime);
    }
}