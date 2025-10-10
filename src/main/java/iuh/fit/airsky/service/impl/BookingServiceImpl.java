package iuh.fit.airsky.service.impl;
import iuh.fit.airsky.dto.request.BookingAncillaryServiceRequest;
import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.request.CheckinRequest;
import iuh.fit.airsky.dto.request.FlightSegmentRequest;
import iuh.fit.airsky.dto.request.PassengerSeatRequest;
import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.response.BaggageResponse;
import iuh.fit.airsky.dto.response.BookingAncillaryServiceResponse;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.PassengerSeatResponse;
import iuh.fit.airsky.dto.response.SeatTypePricingDetail;
import iuh.fit.airsky.dto.response.CheckinEligiblePassengerResponse;
import iuh.fit.airsky.dto.response.CheckinResponse;
import iuh.fit.airsky.enums.BaggageType;
import iuh.fit.airsky.enums.BaggagePackage;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.PaymentStatus;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.enums.LoyaltyTier;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.SeatTypePrice;
import iuh.fit.airsky.enums.CheckinStatus;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.BookingMapper;
import iuh.fit.airsky.mapper.PassengerMapper;
import iuh.fit.airsky.model.*;
import iuh.fit.airsky.repository.*;
import iuh.fit.airsky.service.BookingService;
import iuh.fit.airsky.service.DealService;
import iuh.fit.airsky.service.EmailService;
import iuh.fit.airsky.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final UserRepository userRepository;
    private final FlightRepository flightRepository;
    private final TravelClassRepository travelClassRepository;
    private final SeatRepository seatRepository;
    private final PaymentRepository paymentRepository;
    private final CheckinRepository checkinRepository;
    private final BaggageRepository baggageRepository;
    private final AirportRepository airportRepository;
    private final EmailService emailService;
    private final DealService dealService;
    private final PaymentService paymentService;
    private final DealUsageRepository dealUsageRepository;
    private final PassengerRepository passengerRepository;
    private final PassengerMapper passengerMapper;
    private final AncillaryServiceRepository ancillaryServiceRepository;
    private final BookingAncillaryServiceRepository bookingAncillaryServiceRepository;
    private final FlightTravelClassRepository flightTravelClassRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            BookingMapper bookingMapper,
            UserRepository userRepository,
            FlightRepository flightRepository,
            TravelClassRepository travelClassRepository,
            SeatRepository seatRepository,
            PaymentRepository paymentRepository,
            CheckinRepository checkinRepository,
            BaggageRepository baggageRepository,
            AirportRepository airportRepository,
            EmailService emailService,
            DealService dealService,
            PaymentService paymentService,
            DealUsageRepository dealUsageRepository,
            PassengerRepository passengerRepository,
            PassengerMapper passengerMapper,
            AncillaryServiceRepository ancillaryServiceRepository,
            BookingAncillaryServiceRepository bookingAncillaryServiceRepository,
            FlightTravelClassRepository flightTravelClassRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.userRepository = userRepository;
        this.flightRepository = flightRepository;
        this.travelClassRepository = travelClassRepository;
        this.seatRepository = seatRepository;
        this.paymentRepository = paymentRepository;
        this.checkinRepository = checkinRepository;
        this.baggageRepository = baggageRepository;
        this.airportRepository = airportRepository;
        this.emailService = emailService;
        this.dealService = dealService;
        this.paymentService = paymentService;
        this.dealUsageRepository = dealUsageRepository;
        this.passengerRepository = passengerRepository;
        this.passengerMapper = passengerMapper;
        this.ancillaryServiceRepository = ancillaryServiceRepository;
        this.bookingAncillaryServiceRepository = bookingAncillaryServiceRepository;
        this.flightTravelClassRepository = flightTravelClassRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        try {
            log.info("Starting booking creation for userId: {}, totalAmount: {}, passengers: {}",
                    request.getUserId(), request.getTotalAmount(), request.getPassengers().size());

        // Kiểm tra flightSegments không rỗng
        if (request.getFlightSegments() == null || request.getFlightSegments().isEmpty()) {
            log.error("Flight segments cannot be empty");
            throw new IllegalArgumentException("Flight segments cannot be empty");
        }

        // Lấy flight đầu tiên làm flight chính cho booking
        FlightSegmentRequest firstSegment = request.getFlightSegments().get(0);
        log.info("Processing first segment - flightId: {}, classId: {}", firstSegment.getFlightId(), firstSegment.getClassId());

        Flight flight = flightRepository.findById(firstSegment.getFlightId())
                .orElseThrow(() -> {
                    log.error("Flight not found with id: {}", firstSegment.getFlightId());
                    return new ResourceNotFoundException("Flight not found");
                });

        // Kiểm tra thời gian đặt vé: phải trước giờ khởi hành ít nhất 2 tiếng
        long hoursUntilDeparture = Duration.between(LocalDateTime.now(), flight.getDepartureTime()).toHours();
        log.info("Hours until departure: {}", hoursUntilDeparture);
        if (hoursUntilDeparture < 2) {
            log.error("Booking too close to departure time: {} hours", hoursUntilDeparture);
            throw new IllegalArgumentException("Bạn chỉ có thể đặt vé trước giờ khởi hành ít nhất 2 tiếng");
        }

        // Kiểm tra số lượng ghế trống (cho tất cả segments)
        int totalPassengers = request.getPassengers().size();
        log.info("Total passengers: {}", totalPassengers);
        for (FlightSegmentRequest segment : request.getFlightSegments()) {
            log.info("Checking availability for segment flightId: {}", segment.getFlightId());
            Flight segFlight = flightRepository.findById(segment.getFlightId())
                    .orElseThrow(() -> {
                        log.error("Flight not found for segment: {}", segment.getFlightId());
                        return new ResourceNotFoundException("Flight not found for segment");
                    });
            if (segFlight.getAvailableSeats() < totalPassengers) {
                log.error("Not enough seats for segment {}: available={}, needed={}",
                        segment.getSegmentOrder(), segFlight.getAvailableSeats(), totalPassengers);
                throw new IllegalStateException("Không đủ ghế trống cho segment " + segment.getSegmentOrder());
            }
        }

        Booking booking = bookingMapper.toEntity(request);
        log.info("Created booking entity with code: {}", booking.getBookingCode());

        if (request.getUserId() != null) {
            try {
                User user = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
                booking.setUserId(user);
                log.info("Booking created for authenticated user: {}", user.getEmail());
            } catch (ResourceNotFoundException e) {
                log.warn("User with id {} not found, treating as guest booking", request.getUserId());
                booking.setUserId(null);
            }
        } else {
            log.info("Creating booking for guest user");
            booking.setUserId(null);
        }

        booking.setFlight(flight);
        // Lấy class từ segment đầu tiên
//        booking.setTravelClass(travelClassRepository.findById(firstSegment.getClassId())
//                .orElseThrow(() -> {
//                    log.error("TravelClass not found with id: {}", firstSegment.getClassId());
//                    return new ResourceNotFoundException("Không tìm thấy TravelClass");
//                }));

            FlightTravelClass flightTravelClass = flightTravelClassRepository.findById(firstSegment.getClassId())
                    .orElseThrow(() -> {
                        log.error("FlightTravelClass not found with id: {}", firstSegment.getClassId());
                        return new ResourceNotFoundException("Không tìm thấy FlightTravelClass");
                    });
            booking.setTravelClass(flightTravelClass.getTravelClass());
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setHoldTime(LocalDateTime.now());
        booking.setPaymentTimeout(LocalDateTime.now().plusMinutes(45)); // 45 phút tổng thời gian (30 phút hold + 15 phút payment)

        log.info("Mapping passengers...");
        List<Passenger> passengers = mapPassengers(request, booking);
        booking.getPassengers().addAll(passengers);
        log.info("Mapped {} passengers", passengers.size());

        // Cập nhật số lượng ghế trống cho tất cả segments
        for (FlightSegmentRequest segment : request.getFlightSegments()) {
            Flight segFlight = flightRepository.findById(segment.getFlightId())
                    .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
            updateAvailableSeats(segFlight, passengers.size());
        }

        // Save booking first to avoid transient object issues
        log.info("Saving booking...");
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking saved with id: {}", savedBooking.getBookingId());

        // Now create flight segments after booking is saved
        log.info("Creating flight segments...");
        List<FlightSegment> flightSegments = createFlightSegments(request, savedBooking);
        savedBooking.getFlightSegments().addAll(flightSegments);
        bookingRepository.save(savedBooking);
        log.info("Created {} flight segments", flightSegments.size());

        // Lưu các ghế đã được đặt sau khi booking và passengers đã được lưu
        log.info("Updating seat assignments...");
        for (Passenger passenger : savedBooking.getPassengers()) {
            if (passenger.getSeat() != null) {
                passenger.getSeat().setBookedBy(passenger);
                seatRepository.save(passenger.getSeat());
                log.debug("Updated seat {} for passenger {}", passenger.getSeat().getSeatNumber(), passenger.getFirstName());
            }
        }

        // Tính base amount từ FlightSegments đã được tạo (đảm bảo nhất quán với giá hiển thị)
        BigDecimal baseAmount = savedBooking.getFlightSegments().stream()
                .map(segment -> {
                    BigDecimal segmentPrice = segment.getPrice().multiply(BigDecimal.valueOf(request.getPassengers().size()));
                    log.info("Segment {} - Flight {}: price per person = {}, passengers = {}, total = {}", 
                            segment.getSegmentOrder(), segment.getFlight().getFlightNumber(), 
                            segment.getPrice(), request.getPassengers().size(), segmentPrice);
                    return segmentPrice;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Total base amount from all segments: {}", baseAmount);
        
        BigDecimal baggageAmount = BigDecimal.ZERO;

        // Tính tổng giá baggage packages
        for (PassengerSeatRequest passengerReq : request.getPassengers()) {
            if (passengerReq.getBaggagePackage() != null) {
                BaggagePackage baggagePackage = passengerReq.getBaggagePackage();
                baggageAmount = baggageAmount.add(baggagePackage.getPrice());
            }
        }

        // Tính tổng giá seat types (loại ghế)
        BigDecimal seatTypeAmount = calculateSeatTypeAmount(request, savedBooking);
        log.info("Seat type amount: {}", seatTypeAmount);

        // Calculate ancillary services amount
        BigDecimal ancillaryServicesAmount = calculateAncillaryServicesAmount(request.getAncillaryServices(), savedBooking.getPassengers());
        
        BigDecimal finalAmount = baseAmount.add(baggageAmount).add(seatTypeAmount).add(ancillaryServicesAmount);
        log.info("Base amount: {}, Baggage amount: {}, Seat type amount: {}, Ancillary services amount: {}, Total before deal: {}", 
                baseAmount, baggageAmount, seatTypeAmount, ancillaryServicesAmount, finalAmount);

        if (request.getDealCode() != null && !request.getDealCode().trim().isEmpty()) {
            log.info("Applying deal: {} with orderAmount: {}", request.getDealCode(), finalAmount);
            try {
                var dealUsage = dealService.applyDeal(request.getDealCode(), request.getUserId(), savedBooking.getBookingId(), finalAmount);
                BigDecimal oldFinalAmount = finalAmount;
                finalAmount = dealUsage.getFinalAmount();
                log.info("Applied deal {} to booking {}, discount: {}, amount changed: {} -> {}",
                        request.getDealCode(), savedBooking.getBookingId(), dealUsage.getDiscountAmount(), oldFinalAmount, finalAmount);
            } catch (Exception e) {
                log.error("Failed to apply deal {} for booking {}: {}", request.getDealCode(), savedBooking.getBookingId(), e.getMessage(), e);
                // Tiếp tục với amount gốc nếu deal thất bại
            }
        } else {
            log.info("No deal code provided in request");
        }

        // Tích điểm loyalty cho user nếu có userId
        if (savedBooking.getUserId() != null) {
            log.info("Accumulating loyalty points...");
            User user = savedBooking.getUserId();
            accumulateLoyaltyPoints(user, finalAmount);
        }

        // Tạo thanh toán với final amount
        log.info("Creating payment with amount: {}", finalAmount);
        Payment payment = createPayment(finalAmount, request, savedBooking);
        savedBooking.setPayment(payment);
        savedBooking.setTotalAmount(finalAmount); // Cập nhật total amount sau deal
        
        // Cập nhật lại payment amount nếu có deal được áp dụng
        if (request.getDealCode() != null && !request.getDealCode().trim().isEmpty()) {
            payment.setAmount(finalAmount);
            paymentRepository.save(payment);
        }

        // Create ancillary services records
        log.info("Creating ancillary services for booking");
        createBookingAncillaryServices(request, savedBooking);

        // Tạo CheckIn cho mỗi hành khách
        log.info("Creating check-ins for {} passengers", passengers.size());
        passengers.forEach(passenger -> {
            try {
                createCheckIn(savedBooking, passenger, request);
                log.debug("Created check-in for passenger: {}", passenger.getFirstName());
            } catch (Exception e) {
                log.error("Failed to create check-in for passenger {}: {}", passenger.getFirstName(), e.getMessage());
                throw e; // Re-throw to rollback transaction
            }
        });

        // Save booking once at the end with all updates
        log.info("Final booking save...");
        bookingRepository.save(savedBooking);

        // Gửi email xác nhận nếu có userId
        if (savedBooking.getUserId() != null) {
            log.info("Sending confirmation email...");
            try {
                String email = savedBooking.getUserId().getEmail(); // lấy email từ user
                String subject = "Xác nhận đặt vé thành công";
                String body = "<h3>Xin chào " + savedBooking.getUserId().getLastName() + "</h3>"
                        + "<p>Bạn đã đặt vé thành công cho chuyến bay: " + flight.getFlightNumber() + "</p>"
                        + "<p>Mã đặt vé: " + savedBooking.getBookingCode() + "</p>"
                        + "<p>Khởi hành: " + flight.getDepartureTime() + "</p>"
                        + "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.</p>";

                emailService.sendEmail(email, subject, body);
                log.info("Confirmation email sent to: {}", email);
            } catch (Exception e) {
                log.error("Failed to send confirmation email: {}", e.getMessage());
                // Không throw exception để không rollback transaction vì booking đã thành công
            }
        }

        log.info("Booking creation completed successfully for bookingId: {}", savedBooking.getBookingId());
        
        // Force flush to ensure all changes are persisted before reload
        entityManager.flush();
        
        // Flush and clear to ensure CheckIn/Baggage are persisted and can be loaded fresh
        entityManager.flush();
        entityManager.clear();
        
        // Reload booking with basic details first
        Booking reloadedBooking = bookingRepository.findById(savedBooking.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        
        // Load passengers separately if needed
        if (reloadedBooking.getPassengers().isEmpty()) {
            reloadedBooking = bookingRepository.findByIdWithPassengers(savedBooking.getBookingId())
                    .orElse(reloadedBooking);
        }
        
        // Load check-ins separately if needed
        if (reloadedBooking.getCheckIns().isEmpty()) {
            reloadedBooking = bookingRepository.findByIdWithCheckIns(savedBooking.getBookingId())
                    .orElse(reloadedBooking);
        }
        
        log.info("Booking {} has {} checkIns", reloadedBooking.getBookingId(), 
                reloadedBooking.getCheckIns() != null ? reloadedBooking.getCheckIns().size() : 0);
        
        BookingResponse response = bookingMapper.toResponseDTO(reloadedBooking);
        populateDealInformation(response, reloadedBooking);
        populateBaggageInformation(response, reloadedBooking);
        populateAncillaryServicesInformation(response, reloadedBooking);
        populateSeatTypeInformation(response, reloadedBooking);
        return response;

    } catch (Exception e) {
        log.error("Booking creation failed with error: {}", e.getMessage(), e);
        throw e; // Re-throw to maintain transaction rollback
    }
}

    @Override
    @Transactional
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        log.info("Updating booking with id: {}", id);

        Booking existingBooking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking với id: " + id));

        // Validate booking status - only allow updates for PENDING bookings
        if (existingBooking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể cập nhật booking ở trạng thái PENDING");
        }

        // Update passengers if provided
        if (request.getPassengers() != null && !request.getPassengers().isEmpty()) {
            // Remove existing passengers and related entities
            existingBooking.getPassengers().forEach(passenger -> {
                checkinRepository.deleteByPassenger(passenger, LocalDateTime.now());
                baggageRepository.deleteByPassenger(passenger);
            });
            existingBooking.getPassengers().clear();

            // Add new passengers
            request.getPassengers().forEach(passengerRequest -> {
                Passenger passenger = passengerMapper.toEntity(passengerRequest);
                passenger.setBooking(existingBooking);
                existingBooking.getPassengers().add(passenger);
            });
        }

        Booking savedBooking = bookingRepository.save(existingBooking);
        log.info("Booking updated successfully with id: {}", savedBooking.getBookingId());

        BookingResponse response = bookingMapper.toResponseDTO(savedBooking);
        populateDealInformation(response, savedBooking);
        return response;
    }

    @Override
    public Optional<BookingResponse> findById(Long id) {
        log.info("Finding booking by id: {}", id);

        Optional<Booking> bookingOpt = bookingRepository.findById(id);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            BookingResponse response = bookingMapper.toResponseDTO(booking);
            populateDealInformation(response, booking);
            populateBaggageInformation(response, booking);
            populateAncillaryServicesInformation(response, booking);
            return Optional.of(response);
        }

        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingWithStatusCheck(Long bookingId) {
        log.info("Getting booking with status check: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking với id: " + bookingId));

        // Kiểm tra và cập nhật trạng thái booking nếu cần
        checkAndUpdateBookingStatus(booking);

        BookingResponse response = bookingMapper.toResponseDTO(booking);
        populateDealInformation(response, booking);
        populateBaggageInformation(response, booking);
        populateAncillaryServicesInformation(response, booking);

        return response;
    }

    private void checkAndUpdateBookingStatus(Booking booking) {
        LocalDateTime now = LocalDateTime.now();

        // Nếu booking đang PENDING, kiểm tra timeout
        if (booking.getStatus() == BookingStatus.PENDING) {
            boolean shouldCancel = false;
            String cancelReason = "";

            // Kiểm tra seat hold time (30 phút - thời gian giữ ghế)
            if (booking.getHoldTime() != null && Duration.between(booking.getHoldTime(), now).toMinutes() > 30) {
                shouldCancel = true;
                cancelReason = "Seat hold time expired (30 minutes) - seats released";
            }
            // Kiểm tra payment timeout (45 phút tổng thời gian)
            else if (booking.getPaymentTimeout() != null && now.isAfter(booking.getPaymentTimeout())) {
                if (booking.getPayment() != null) {
                    PaymentStatus paymentStatus = booking.getPayment().getStatus();
                    if (paymentStatus == PaymentStatus.PENDING || paymentStatus == PaymentStatus.FAILED) {
                        shouldCancel = true;
                        cancelReason = "Payment timeout expired (45 minutes total)";
                    }
                } else {
                    shouldCancel = true;
                    cancelReason = "No payment initiated within timeout";
                }
            }

            if (shouldCancel) {
                cancelBookingAndReleaseSeats(booking, cancelReason);
            }
        }
    }

    @Override
    public PageResponse<BookingResponse> findAll(Pageable pageable) {
        log.info("Finding all bookings with pageable: {}", pageable);

        Page<Booking> bookingPage = bookingRepository.findAll(pageable);
        List<BookingResponse> bookingResponses = bookingPage.getContent().stream()
                .map(booking -> {
                    try {
                        BookingResponse response = bookingMapper.toResponseDTO(booking);
                        populateDealInformation(response, booking);
                        populateBaggageInformation(response, booking);
                        populateAncillaryServicesInformation(response, booking);
                        return response;
                    } catch (Exception e) {
                        log.warn("Could not fully populate booking response for booking {}: {}", booking.getBookingId(), e.getMessage());
                        // Create a minimal response with basic info
                        BookingResponse response = new BookingResponse();
                        response.setBookingId(booking.getBookingId());
                        response.setBookingCode(booking.getBookingCode());
                        response.setStatus(booking.getStatus());
                        response.setTotalAmount(booking.getTotalAmount());
                        response.setBookingDate(booking.getBookingDate());
                        // Try to populate at least basic passenger info if available
                        if (booking.getPassengers() != null && !booking.getPassengers().isEmpty()) {
                            response.setPassengers(booking.getPassengers().stream()
                                    .map((Passenger p) -> {
                                        PassengerSeatResponse psr = new PassengerSeatResponse();
                                        psr.setPassengerId(p.getPassengerId());
                                        psr.setFirstName(p.getFirstName());
                                        psr.setLastName(p.getLastName());
                                        psr.setType(p.getType());
                                        return psr;
                                    })
                                    .collect(java.util.stream.Collectors.toList()));
                        }
                        return response;
                    }
                })
                .collect(java.util.stream.Collectors.toList());

        return new PageResponse<>(
                bookingResponses,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting booking with id: {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking với id: " + id));

        // Validate booking status - only allow deletion for PENDING bookings
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể xóa booking ở trạng thái PENDING");
        }

        // Release seats back to available pool
        if (booking.getFlight() != null) {
            Flight flight = booking.getFlight();
            int passengerCount = booking.getPassengers().size();
            flight.setAvailableSeats(flight.getAvailableSeats() + passengerCount);
            flightRepository.save(flight);
            log.info("Released {} seats back to flight {}", passengerCount, flight.getFlightNumber());
        }

        // Delete related entities first
        booking.getPassengers().forEach(passenger -> {
            checkinRepository.deleteByPassenger(passenger, LocalDateTime.now());
            baggageRepository.deleteByPassenger(passenger);
        });

        // Delete deal usage if exists
        dealUsageRepository.findByBooking(booking).ifPresent(dealUsage -> {
            dealUsageRepository.delete(dealUsage);
            log.info("Deleted deal usage for booking {}", booking.getBookingId());
        });

        // Delete payment if exists
        if (booking.getPayment() != null) {
            paymentRepository.delete(booking.getPayment());
            log.info("Deleted payment for booking {}", booking.getBookingId());
        }

        bookingRepository.delete(booking);
        log.info("Booking deleted successfully with id: {}", id);
    }

    @Override
    @Transactional
    public BookingResponse completeBooking(Long bookingId) {
        log.info("Completing booking with id: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking với id: " + bookingId));

        // Validate booking status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hoàn thành booking ở trạng thái PENDING");
        }

        // Validate payment status
        if (booking.getPayment() == null || booking.getPayment().getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Phải hoàn thành thanh toán trước khi hoàn thành booking");
        }

        // Update booking status to CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);

        // Award loyalty points for completed booking
        if (savedBooking.getUserId() != null) {
            awardLoyaltyPointsForCompletedBooking(savedBooking);
        }

        log.info("Booking completed successfully with id: {}", bookingId);

        BookingResponse response = bookingMapper.toResponseDTO(savedBooking);
        populateDealInformation(response, savedBooking);
        return response;
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void autoCompleteFlightsAndBookings() {
        log.info("Starting automatic flight and booking completion check...");

        LocalDateTime now = LocalDateTime.now();

        try {
            // 1. Update flight statuses based on time
            updateFlightStatuses(now);

            // 2. Auto-complete bookings for departed flights
            autoCompleteBookingsForDepartedFlights(now);

            log.info("Automatic flight and booking completion check completed successfully");
        } catch (Exception e) {
            log.error("Error during automatic flight and booking completion: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 60000) // Run every 1 minute
    @Transactional
    public void checkPendingBookingsAndPayments() {
        log.info("Starting check for expired bookings and payments...");

        LocalDateTime now = LocalDateTime.now();
        List<Booking> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING);

        for (Booking booking : pendingBookings) {
            try {
                // Kiểm tra thời hạn giữ chỗ (30 phút)
                if (booking.getHoldTime() != null && Duration.between(booking.getHoldTime(), now).toMinutes() > 30) {
                    cancelBookingAndReleaseSeats(booking, "Seat hold time expired (30 minutes)");
                    continue;
                }

                // Cảnh báo sớm khi còn 10 phút nữa hết holdTime
                if (booking.getHoldTime() != null &&
                    Duration.between(booking.getHoldTime(), now).toMinutes() <= 20 &&
                    Duration.between(booking.getHoldTime(), now).toMinutes() > 19) {
                    sendHoldTimeWarningEmail(booking, 10);
                }

                // Kiểm tra thời hạn thanh toán (45 phút tổng)
                if (booking.getPaymentTimeout() != null && now.isAfter(booking.getPaymentTimeout())) {
                    // Kiểm tra trạng thái payment
                    if (booking.getPayment() != null) {
                        PaymentStatus paymentStatus = booking.getPayment().getStatus();
                        if (paymentStatus == PaymentStatus.PENDING || paymentStatus == PaymentStatus.FAILED) {
                            cancelBookingAndReleaseSeats(booking, "Payment timeout expired (45 minutes total)");
                            // Cập nhật payment status nếu cần
                            if (paymentStatus == PaymentStatus.PENDING) {
                                booking.getPayment().setStatus(PaymentStatus.FAILED);
                                paymentRepository.save(booking.getPayment());
                            }
                            continue;
                        }
                    } else {
                        // Không có payment record, hủy booking
                        cancelBookingAndReleaseSeats(booking, "No payment initiated within 45 minutes");
                        continue;
                    }
                }

                // Gửi cảnh báo khi còn 5 phút nữa là hết hạn thanh toán (sau 40 phút)
                if (booking.getPaymentTimeout() != null &&
                    Duration.between(now, booking.getPaymentTimeout()).toMinutes() <= 5 &&
                    Duration.between(now, booking.getPaymentTimeout()).toMinutes() > 0) {

                    if (booking.getUserId() != null && booking.getPayment() != null &&
                        booking.getPayment().getStatus() == PaymentStatus.PENDING) {
                        sendPaymentReminderEmail(booking);
                    }
                }

            } catch (Exception e) {
                log.error("Error processing booking {}: {}", booking.getBookingId(), e.getMessage(), e);
            }
        }

        log.info("Completed check for expired bookings and payments");
    }

    private void cancelBookingAndReleaseSeats(Booking booking, String reason) {
        log.info("Cancelling booking {} due to: {}", booking.getBookingId(), reason);

        // Hủy booking
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Giải phóng ghế và cập nhật availableSeats
        int releasedSeats = 0;
        List<Passenger> passengersToDelete = new ArrayList<>(booking.getPassengers());

        // Đầu tiên release tất cả seats trước khi xóa passengers
        for (Passenger passenger : passengersToDelete) {
            if (passenger.getSeat() != null) {
                // Reset seat về AVAILABLE và xóa bookedBy
                passenger.getSeat().setStatus(SeatStatus.AVAILABLE);
                passenger.getSeat().setBookedBy(null);
                seatRepository.save(passenger.getSeat());
                releasedSeats++;
                log.debug("Released seat {} for passenger {}", passenger.getSeat().getSeatNumber(), passenger.getFirstName());
            }
        }

        // Sau đó xóa tất cả passengers của booking này
        if (!passengersToDelete.isEmpty()) {
            passengerRepository.deleteAll(passengersToDelete);
            log.info("Deleted {} passenger records for cancelled booking {}", passengersToDelete.size(), booking.getBookingId());
        }

        // Cập nhật availableSeats của flight
        if (releasedSeats > 0 && booking.getFlight() != null) {
            Flight flight = booking.getFlight();
            flight.setAvailableSeats(flight.getAvailableSeats() + releasedSeats);
            flightRepository.save(flight);
            log.info("Updated available seats for flight {}: +{}", flight.getFlightNumber(), releasedSeats);
        }

        // Xử lý payment status khi cancel booking
        if (booking.getPayment() != null) {
            Payment payment = booking.getPayment();
            PaymentStatus currentStatus = payment.getStatus();

            // Cập nhật payment status dựa trên trạng thái hiện tại
            if (currentStatus == PaymentStatus.COMPLETED) {
                // Nếu đã hoàn thành thì xử lý refund
                paymentService.processRefundForCancelledBooking(booking, reason);
            } else if (currentStatus == PaymentStatus.PENDING) {
                // Nếu đang pending thì đánh dấu là expired/cancelled
                payment.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(payment);
                log.info("Marked pending payment {} as cancelled for booking {}", payment.getPaymentId(), booking.getBookingId());
            } else if (currentStatus == PaymentStatus.FAILED) {
                // Nếu đã failed thì giữ nguyên status
                log.info("Payment {} already failed for booking {}, no action needed", payment.getPaymentId(), booking.getBookingId());
            }
        }

        // Gửi email thông báo hủy booking
        if (booking.getUserId() != null) {
            sendCancellationEmail(booking, reason);
        }

        log.info("Successfully cancelled booking {} and released {} seats", booking.getBookingId(), releasedSeats);
    }

    private void sendPaymentReminderEmail(Booking booking) {
        try {
            String email = booking.getUserId().getEmail();
            String subject = "Cảnh báo: Thời hạn thanh toán sắp hết";
            String body = String.format(
                "<h3>Xin chào %s</h3>" +
                "<p>Mã đặt vé: <strong>%s</strong></p>" +
                "<p>Thời hạn thanh toán cho booking của bạn sẽ hết trong 5 phút nữa.</p>" +
                "<p>Vui lòng hoàn thành thanh toán để tránh việc hủy booking tự động.</p>" +
                "<p>Chuyến bay: %s</p>" +
                "<p>Số tiền: $%.2f</p>" +
                "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.</p>",
                booking.getUserId().getLastName(),
                booking.getBookingCode(),
                booking.getFlight().getFlightNumber(),
                booking.getTotalAmount()
            );

            emailService.sendEmail(email, subject, body);
            log.info("Payment reminder email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send payment reminder email for booking {}: {}", booking.getBookingId(), e.getMessage());
        }
    }

    private void sendCancellationEmail(Booking booking, String reason) {
        try {
            String email = booking.getUserId().getEmail();
            String subject = "Thông báo: Booking đã bị hủy";
            String body = String.format(
                "<h3>Xin chào %s</h3>" +
                "<p>Mã đặt vé: <strong>%s</strong></p>" +
                "<p>Booking của bạn đã bị hủy vì: <strong>%s</strong></p>" +
                "<p>Chuyến bay: %s</p>" +
                "<p>Số tiền đã hoàn: $%.2f</p>" +
                "<p>Ghế đã được giải phóng và có thể đặt lại.</p>" +
                "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.</p>",
                booking.getUserId().getLastName(),
                booking.getBookingCode(),
                reason,
                booking.getFlight().getFlightNumber(),
                booking.getTotalAmount()
            );

            emailService.sendEmail(email, subject, body);
            log.info("Cancellation email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send cancellation email for booking {}: {}", booking.getBookingId(), e.getMessage());
        }
    }

    private void updateFlightStatuses(LocalDateTime now) {
        log.info("Updating flight statuses...");

        // Find flights that should be departed (departure time has passed)
        List<Flight> departedFlights = flightRepository.findFlightsByDepartureTimeBeforeAndStatusNot(
                now, FlightStatus.DEPARTED);

        for (Flight flight : departedFlights) {
            flight.setStatus(FlightStatus.DEPARTED);
            flightRepository.save(flight);
            log.info("Updated flight {} status to DEPARTED", flight.getFlightNumber());
        }

        // Find flights that should be delayed (departure time passed but not departed)
        List<Flight> delayedFlights = flightRepository.findFlightsByDepartureTimeBeforeAndStatus(
                now.minusMinutes(30), FlightStatus.ON_TIME); // Delayed if 30+ minutes past scheduled time

        for (Flight flight : delayedFlights) {
            flight.setStatus(FlightStatus.DELAYED);
            flightRepository.save(flight);
            log.info("Updated flight {} status to DELAYED", flight.getFlightNumber());
        }
    }

    private void autoCompleteBookingsForDepartedFlights(LocalDateTime now) {
        log.info("Auto-completing bookings for departed flights...");

        // Find bookings for departed flights that are still PENDING
        List<Booking> pendingBookingsForDepartedFlights = bookingRepository
                .findBookingsByFlightStatusAndBookingStatus(FlightStatus.DEPARTED, BookingStatus.PENDING);

        for (Booking booking : pendingBookingsForDepartedFlights) {
            try {
                // Auto-complete the booking
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);

                // Award loyalty points
                if (booking.getUserId() != null) {
                    awardLoyaltyPointsForCompletedBooking(booking);
                }

                log.info("Auto-completed booking {} for departed flight {}",
                        booking.getBookingId(), booking.getFlight().getFlightNumber());
            } catch (Exception e) {
                log.error("Failed to auto-complete booking {}: {}", booking.getBookingId(), e.getMessage());
            }
        }
    }

    private void populateDealInformation(BookingResponse response, Booking booking) {
        // Find deal usage for this booking
        Optional<DealUsage> dealUsageOpt = dealUsageRepository.findByBooking(booking);
        log.info("Looking for deal usage for booking {}: found={}, bookingId={}",
                booking.getBookingId(), dealUsageOpt.isPresent(), booking.getBookingId());
        if (dealUsageOpt.isPresent()) {
            DealUsage dealUsage = dealUsageOpt.get();
            log.info("Found deal usage: id={}, dealCode={}, discountAmount={}",
                    dealUsage.getUsageId(), dealUsage.getDeal().getDealCode(), dealUsage.getDiscountAmount());
            response.setAppliedDealCode(dealUsage.getDeal().getDealCode());
            response.setDiscountPercentage(dealUsage.getDeal().getDiscountPercentage());
            response.setDiscountAmount(dealUsage.getDiscountAmount());
        } else {
            log.warn("No deal usage found for booking {} - deal may not have been applied successfully", booking.getBookingId());
            response.setAppliedDealCode(null);
            response.setDiscountPercentage(null);
            response.setDiscountAmount(BigDecimal.ZERO);
        }
    }
    
    private void populateBaggageInformation(BookingResponse response, Booking booking) {
        List<BaggageResponse> baggageList = new ArrayList<>();

        // Check if checkIns are loaded, if not fetch them for baggage population
        List<CheckIn> checkIns = booking.getCheckIns();
        if (checkIns == null || !org.hibernate.Hibernate.isInitialized(checkIns)) {
            log.debug("CheckIns not loaded for booking {}, fetching for baggage population", booking.getBookingId());
            // Fetch checkIns with baggage information
            checkIns = checkinRepository.findByBookingIdWithBaggage(booking.getBookingId());
        }

        if (checkIns != null && !checkIns.isEmpty()) {
            log.info("Populating baggage info for {} checkIns", checkIns.size());

            for (CheckIn checkIn : checkIns) {
                if (checkIn.getBaggage() != null) {
                    Baggage baggage = checkIn.getBaggage();
                    BaggageResponse baggageResponse = new BaggageResponse();
                    baggageResponse.setBaggageId(baggage.getBaggageId());
                    baggageResponse.setCheckinId(checkIn.getCheckInId());
                    baggageResponse.setType(baggage.getType());
                    baggageResponse.setPurchasedPackage(baggage.getPurchasedPackage());
                    baggageResponse.setPackagePrice(baggage.getPackagePrice());
                    baggageResponse.setActualWeight(baggage.getActualWeight());
                    baggageResponse.setExcessWeight(baggage.getExcessWeight());
                    baggageResponse.setExcessFee(baggage.getExcessFee());
                    baggageList.add(baggageResponse);

                    log.info("Added baggage {} for checkIn {}", baggage.getBaggageId(), checkIn.getCheckInId());
                }
            }
        }

        response.setBaggage(baggageList);
        log.info("Set {} baggage items in response", baggageList.size());
    }

    private List<Passenger> mapPassengers(BookingRequest request, Booking booking) {
        return request.getPassengers().stream().map(p -> {
            Passenger passenger = new Passenger();
            passenger.setFirstName(p.getFirstName());
            passenger.setLastName(p.getLastName());
            passenger.setDateOfBirth(p.getDateOfBirth());
            passenger.setPassportNumber(p.getPassportNumber());
            passenger.setType(p.getType());
            // Map thêm các thông tin cá nhân
            passenger.setGender(p.getGender());
            passenger.setEmail(p.getEmail());
            passenger.setPhone(p.getPhone());

            if (p.getSeatId() != null) {
                Seat seat = seatRepository.findById(p.getSeatId())
                        .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));

                // Kiểm tra seat có available không
                if (seat.getStatus() != SeatStatus.AVAILABLE || seat.getBookedBy() != null) {
                    throw new IllegalStateException("Seat " + seat.getSeatNumber() + " is not available for booking");
                }

                // Kiểm tra không có passenger nào đang reference đến seat này từ booking active
                List<Passenger> existingPassengers = passengerRepository.findBySeat(seat);
                boolean hasActivePassengers = existingPassengers.stream()
                        .anyMatch(existingPassenger -> existingPassenger.getBooking() != null && 
                                     existingPassenger.getBooking().getStatus() != BookingStatus.CANCELLED);
                if (hasActivePassengers) {
                    throw new IllegalStateException("Seat " + seat.getSeatNumber() + " is still referenced by an existing active passenger record");
                }

                seat.setStatus(SeatStatus.PENDING_PAYMENT);
                seat.setBookedBy(passenger);
                passenger.setSeat(seat);
            }

            // ✅ Gắn baggagePackage tạm vào Passenger qua BookingRequest (để xử lý tiếp ở createCheckIn)
            passenger.setBooking(booking);
            passenger.setTempBaggagePackage(p.getBaggagePackage()); // cần thêm field transient
            return passenger;
        }).toList();
    }

    private void updateAvailableSeats(Flight flight, int passengerCount) {
        flight.setAvailableSeats(flight.getAvailableSeats() - passengerCount);
        flightRepository.save(flight);
    }

    private BigDecimal calculateAncillaryServicesAmount(List<BookingAncillaryServiceRequest> ancillaryServiceRequests, 
                                                        List<Passenger> passengers) {
        if (ancillaryServiceRequests == null || ancillaryServiceRequests.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (BookingAncillaryServiceRequest serviceRequest : ancillaryServiceRequests) {
            // Get ancillary service details
            AncillaryService ancillaryService = ancillaryServiceRepository.findById(serviceRequest.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại với ID: " + serviceRequest.getServiceId()));
            
            // Verify service is active
            if (!ancillaryService.isActive()) {
                throw new IllegalArgumentException("Dịch vụ không còn hoạt động: " + ancillaryService.getServiceName());
            }
            
            // Calculate amount for this service
            BigDecimal serviceAmount = ancillaryService.getPrice().multiply(BigDecimal.valueOf(serviceRequest.getQuantity()));
            totalAmount = totalAmount.add(serviceAmount);
            
            log.info("Ancillary service: {} - Quantity: {} - Unit price: {} - Total: {}", 
                    ancillaryService.getServiceName(), serviceRequest.getQuantity(), 
                    ancillaryService.getPrice(), serviceAmount);
        }
        
        return totalAmount;
    }
    
    private void createBookingAncillaryServices(BookingRequest request, Booking savedBooking) {
        if (request.getAncillaryServices() == null || request.getAncillaryServices().isEmpty()) {
            return;
        }

        // Create a map from passengerId in passengerSeats to passenger object
        // passengerId can be any value, we map them sequentially to passengers array
        Map<Long, Passenger> passengerMap = new HashMap<>();
        int passengerIndex = 0;
        for (FlightSegmentRequest segmentRequest : request.getFlightSegments()) {
            if (segmentRequest.getPassengerSeats() != null) {
                for (FlightSegmentRequest.PassengerSeatAssignment seatAssignment : segmentRequest.getPassengerSeats()) {
                    if (seatAssignment.getPassengerId() != null && passengerIndex < savedBooking.getPassengers().size()) {
                        // Map passengerId to passenger by sequential order of passengerSeats
                        passengerMap.put(seatAssignment.getPassengerId(), savedBooking.getPassengers().get(passengerIndex));
                        passengerIndex++;
                    }
                }
            }
        }

        for (BookingAncillaryServiceRequest serviceRequest : request.getAncillaryServices()) {
            AncillaryService ancillaryService = ancillaryServiceRepository.findById(serviceRequest.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại với ID: " + serviceRequest.getServiceId()));

            // Find passenger using the passengerId from passengerSeats mapping
            Passenger passenger = null;
            if (serviceRequest.getPassengerId() != null) {
                passenger = passengerMap.get(serviceRequest.getPassengerId());
                if (passenger == null) {
                    throw new ResourceNotFoundException("Hành khách không tồn tại với passengerId: " + serviceRequest.getPassengerId());
                }
            }

            // Create booking ancillary service record
            BookingAncillaryService bookingAncillaryService = new BookingAncillaryService();
            bookingAncillaryService.setBooking(savedBooking);
            bookingAncillaryService.setAncillaryService(ancillaryService);
            bookingAncillaryService.setPassenger(passenger);
            bookingAncillaryService.setQuantity(serviceRequest.getQuantity());
            bookingAncillaryService.setUnitPrice(ancillaryService.getPrice());
            bookingAncillaryService.setTotalPrice(ancillaryService.getPrice().multiply(BigDecimal.valueOf(serviceRequest.getQuantity())));
            bookingAncillaryService.setNotes(serviceRequest.getNotes());

            bookingAncillaryServiceRepository.save(bookingAncillaryService);

            log.info("Created booking ancillary service: {} for booking: {}",
                    ancillaryService.getServiceName(), savedBooking.getBookingId());
        }
    }    private void populateAncillaryServicesInformation(BookingResponse response, Booking booking) {
        List<BookingAncillaryService> bookingAncillaryServices = 
                bookingAncillaryServiceRepository.findByBookingId(booking.getBookingId());
        
        List<BookingAncillaryServiceResponse> ancillaryServiceResponses = bookingAncillaryServices.stream()
                .map(bas -> {
                    BookingAncillaryServiceResponse serviceResponse = new BookingAncillaryServiceResponse();
                    serviceResponse.setBookingServiceId(bas.getBookingServiceId());
                    serviceResponse.setServiceId(bas.getAncillaryService().getServiceId());
                    serviceResponse.setServiceName(bas.getAncillaryService().getServiceName());
                    serviceResponse.setServiceType(bas.getAncillaryService().getServiceType().name());
                    serviceResponse.setServiceTypeDisplayName(bas.getAncillaryService().getServiceType().getVietnameseName());
                    serviceResponse.setPassengerId(bas.getPassenger() != null ? bas.getPassenger().getPassengerId() : null);
                    serviceResponse.setPassengerName(bas.getPassenger() != null ? 
                            bas.getPassenger().getFirstName() + " " + bas.getPassenger().getLastName() : null);
                    serviceResponse.setQuantity(bas.getQuantity());
                    serviceResponse.setUnitPrice(bas.getUnitPrice());
                    serviceResponse.setTotalPrice(bas.getTotalPrice());
                    serviceResponse.setNotes(bas.getNotes());
                    return serviceResponse;
                })
                .collect(java.util.stream.Collectors.toList());
        
        BigDecimal ancillaryServicesAmount = bookingAncillaryServices.stream()
                .map(BookingAncillaryService::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        response.setAncillaryServices(ancillaryServiceResponses);
        response.setAncillaryServicesAmount(ancillaryServicesAmount);
        
        log.info("Populated {} ancillary services with total amount: {}", 
                ancillaryServiceResponses.size(), ancillaryServicesAmount);
    }

    private List<FlightSegment> createFlightSegments(BookingRequest request, Booking booking) {
        return request.getFlightSegments().stream()
                .map(segmentRequest -> {
                    FlightSegment segment = new FlightSegment();
                    segment.setBooking(booking);
                    // Set segment order from request to maintain sequence
                    segment.setSegmentOrder(segmentRequest.getSegmentOrder());
                    Flight flight = flightRepository.findById(segmentRequest.getFlightId())
                            .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
                    
                    // Ensure airports are loaded
                    Airport departureAirport = flight.getDepartureAirport();
                    Airport arrivalAirport = flight.getArrivalAirport();
                    
                    segment.setFlight(flight);
//                    segment.setTravelClass(travelClassRepository.findById(segmentRequest.getClassId())
//                            .orElseThrow(() -> new ResourceNotFoundException("Travel class not found")));

                    FlightTravelClass flightTravelClass = flightTravelClassRepository.findById(segmentRequest.getClassId())
                            .orElseThrow(() -> new ResourceNotFoundException("FlightTravelClass not found"));
                    segment.setTravelClass(flightTravelClass.getTravelClass());
                    
                    // Set airport information from the flight
                    segment.setDepartureAirport(departureAirport);
                    segment.setArrivalAirport(arrivalAirport);
                    
                    // Set price from database FlightTravelClass, not from request
                    BigDecimal segmentPrice = flight.getFlightTravelClasses().stream()
                            .filter(ftc -> ftc.getTravelClass().getId().equals(segmentRequest.getClassId()))
                            .map(ftc -> ftc.getPrice() != null ? ftc.getPrice() : flight.getBasePrice())
                            .findFirst()
                            .orElse(flight.getBasePrice());
                    segment.setPrice(segmentPrice);
                    
                    // Set additional flight information
                    segment.setDepartureTime(flight.getDepartureTime());
                    segment.setArrivalTime(flight.getArrivalTime());
                    segment.setAircraft(flight.getAircraft() != null ? flight.getAircraft().getAircraftName() : null);
                    segment.setDuration(flight.getDuration() != null ? flight.getDuration() + " minutes" : null);
                    
                    return segment;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private void accumulateLoyaltyPoints(User user, BigDecimal amount) {
        // Simple loyalty points calculation: 1 point per 1000 VND
        int points = amount.divide(BigDecimal.valueOf(1000)).intValue();
        if (points > 0) {
            Integer currentPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
            user.setLoyaltyPoints(currentPoints + points);
            userRepository.save(user);
            log.info("Accumulated {} loyalty points for user {}", points, user.getEmail());
        }
    }

    private Payment createPayment(BigDecimal amount, BookingRequest request, Booking booking) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.saveAndFlush(payment);
    }

    private void createCheckIn(Booking booking, Passenger passenger, BookingRequest request) {
        Baggage baggage = null;

        if (passenger.getTempBaggagePackage() != null) {
            log.info("Creating baggage for passenger {} with package {}", 
                    passenger.getFirstName(), passenger.getTempBaggagePackage());
            baggage = Baggage.builder()
                    .type(BaggageType.CHECK_IN)
                    .purchasedPackage(passenger.getTempBaggagePackage())
                    .packagePrice(passenger.getTempBaggagePackage().getPrice())
                    .build();
            baggage = baggageRepository.save(baggage);
            log.info("Baggage created with ID: {}", baggage.getBaggageId());
        } else {
            log.info("No baggage package selected for passenger {}", passenger.getFirstName());
        }

        String seatNumber = passenger.getSeat() != null ? passenger.getSeat().getSeatNumber() : null;
        
        CheckIn checkIn = CheckIn.builder()
                .booking(booking)
                .passenger(passenger)
                .baggage(baggage) // có thể null nếu không chọn
                .seatNumber(seatNumber)
                .checkedAt(booking.getFlight().getDepartureTime())
                .status(CheckinStatus.PENDING) // Set status to PENDING when booking is created
                .build();
        checkIn = checkinRepository.save(checkIn);
        log.info("CheckIn created with ID: {} for passenger {}, baggage: {}", 
                checkIn.getCheckInId(), passenger.getFirstName(), 
                baggage != null ? baggage.getBaggageId() : "null");

        if (baggage != null) {
            baggage.setCheckIn(checkIn);
            baggageRepository.save(baggage);
            log.info("Baggage-CheckIn relationship established: baggage {} -> checkIn {}", 
                    baggage.getBaggageId(), checkIn.getCheckInId());
        }
    }

    private void awardLoyaltyPointsForCompletedBooking(Booking booking) {
        User user = booking.getUserId();
        BigDecimal bookingAmount = booking.getTotalAmount();

        // Award points based on booking amount: 1 point per 1000 VND for completed bookings
        int points = bookingAmount.divide(BigDecimal.valueOf(1000)).intValue();

        // Bonus points for completed bookings (additional 10% bonus)
        int bonusPoints = points / 10;
        int totalPoints = points + bonusPoints;

        if (totalPoints > 0) {
            Integer currentPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
            user.setLoyaltyPoints(currentPoints + totalPoints);
            userRepository.save(user);
            log.info("Awarded {} loyalty points ({} base + {} bonus) for completed booking {} to user {}",
                    totalPoints, points, bonusPoints, booking.getBookingId(), user.getEmail());
        }
    }

    @Override
    public Optional<BookingResponse> findByBookingCodeAndPassengerName(String bookingCode, String fullName) {
        log.info("Finding booking by booking code: {} and passenger name: {}", bookingCode, fullName);

        Optional<Booking> bookingOpt = bookingRepository.findByBookingCodeAndPassengerFullName(bookingCode, fullName);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            try {
                BookingResponse response = bookingMapper.toResponseDTO(booking);
                populateDealInformation(response, booking);
                try {
                    populateBaggageInformation(response, booking);
                } catch (Exception e) {
                    log.warn("Could not populate baggage information for guest booking lookup: {}", e.getMessage());
                    // Baggage information is optional for guest booking lookup
                }
                populateAncillaryServicesInformation(response, booking);
                populateSeatTypeInformation(response, booking);
                response.setCheckinEligiblePassengers(getPassengersWithCheckinStatus(bookingCode, fullName));
                
                // Populate available seats for the travel class
                if (booking.getTravelClass() != null && booking.getFlight() != null) {
                    List<Seat> availableSeats = seatRepository.findAvailableSeatsByFlightIdAndTravelClassId(
                        booking.getFlight().getFlightId(), 
                        booking.getTravelClass().getId()
                    );
                    List<String> availableSeatNumbers = availableSeats.stream()
                        .map(Seat::getSeatNumber)
                        .collect(java.util.stream.Collectors.toList());
                    response.setAvailableSeats(availableSeatNumbers);
                }
                return Optional.of(response);
            } catch (Exception e) {
                log.warn("Could not fully populate booking response for lookup {}: {}", bookingCode, e.getMessage());
                // Create a minimal response for lookup
                BookingResponse response = new BookingResponse();
                response.setBookingId(booking.getBookingId());
                response.setBookingCode(booking.getBookingCode());
                response.setStatus(booking.getStatus());
                response.setTotalAmount(booking.getTotalAmount());
                response.setBookingDate(booking.getBookingDate());
                if (booking.getPassengers() != null && !booking.getPassengers().isEmpty()) {
                    response.setPassengers(booking.getPassengers().stream()
                            .map((Passenger p) -> {
                                PassengerSeatResponse psr = new PassengerSeatResponse();
                                psr.setPassengerId(p.getPassengerId());
                                psr.setFirstName(p.getFirstName());
                                psr.setLastName(p.getLastName());
                                psr.setType(p.getType());
                                return psr;
                            })
                            .collect(java.util.stream.Collectors.toList()));
                }
                return Optional.of(response);
            }
        }

        return Optional.empty();
    }

    @Override
    public BookingResponse processPaymentForGuestBooking(Long bookingId, PaymentRequest paymentRequest) {
        log.info("Processing payment for guest booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking với id: " + bookingId));

        // Validate booking status - only allow payment for PENDING bookings
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể thanh toán booking ở trạng thái PENDING");
        }

        // Create or update payment
        Payment payment;
        if (booking.getPayment() != null) {
            payment = booking.getPayment();
            payment.setAmount(paymentRequest.getTotalAmount());
            payment.setPaymentMethod(paymentRequest.getPaymentMethod());
            payment.setStatus(paymentRequest.getPaymentStatus());
            payment.setPaymentDate(paymentRequest.getPaymentDate());
        } else {
            payment = new Payment();
            payment.setBooking(booking);
            payment.setAmount(paymentRequest.getTotalAmount());
            payment.setPaymentMethod(paymentRequest.getPaymentMethod());
            payment.setStatus(paymentRequest.getPaymentStatus());
            payment.setPaymentDate(paymentRequest.getPaymentDate());
            booking.setPayment(payment);
        }

        // If payment is successful, update booking status
        if (paymentRequest.getPaymentStatus() == PaymentStatus.COMPLETED) {
            booking.setStatus(BookingStatus.CONFIRMED);

            // Update seat status to OCCUPIED
            for (Passenger passenger : booking.getPassengers()) {
                if (passenger.getSeat() != null) {
                    passenger.getSeat().setStatus(SeatStatus.OCCUPIED);
                    seatRepository.save(passenger.getSeat());
                }
            }

            log.info("Booking {} confirmed and seats occupied after successful payment", bookingId);
        }

        Payment savedPayment = paymentRepository.save(payment);
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Payment processed successfully for guest booking: {}", bookingId);

        BookingResponse response = bookingMapper.toResponseDTO(savedBooking);
        populateDealInformation(response, savedBooking);
        populateBaggageInformation(response, savedBooking);
        populateAncillaryServicesInformation(response, savedBooking);
        return response;
    }

    private BigDecimal calculateSeatTypeAmount(BookingRequest request, Booking booking) {
        BigDecimal totalSeatTypeAmount = BigDecimal.ZERO;

        for (int i = 0; i < request.getPassengers().size(); i++) {
            PassengerSeatRequest passengerReq = request.getPassengers().get(i);
            Passenger passenger = booking.getPassengers().get(i);

            // Nếu có seatType được chọn và ghế đã được assign
            if (passengerReq.getSeatType() != null && passenger.getSeat() != null) {
                try {
                    // Lấy giá seat type từ enum
                    SeatTypePrice seatTypePrice = SeatTypePrice.fromSeatType(passengerReq.getSeatType());
                    BigDecimal additionalPrice = seatTypePrice.getAdditionalPrice();

                    totalSeatTypeAmount = totalSeatTypeAmount.add(additionalPrice);

                    // Cập nhật loại ghế cho seat
                    passenger.getSeat().setType(passengerReq.getSeatType());
                    seatRepository.save(passenger.getSeat());

                    log.debug("Applied seat type {} with additional price {} for passenger {}",
                            passengerReq.getSeatType(), additionalPrice, passenger.getFirstName());
                } catch (Exception e) {
                    log.error("Error calculating seat type amount for passenger {}: {}",
                            passenger.getFirstName(), e.getMessage());
                }
            }
        }

        return totalSeatTypeAmount;
    }

    private void populateSeatTypeInformation(BookingResponse response, Booking booking) {
        BigDecimal totalSeatTypeAmount = BigDecimal.ZERO;
        List<SeatTypePricingDetail> seatTypeDetails = new ArrayList<>();

        for (Passenger passenger : booking.getPassengers()) {
            if (passenger.getSeat() != null && passenger.getSeat().getType() != null) {
                SeatTypePrice seatTypePrice = SeatTypePrice.fromSeatType(passenger.getSeat().getType());
                BigDecimal additionalPrice = seatTypePrice.getAdditionalPrice();

                totalSeatTypeAmount = totalSeatTypeAmount.add(additionalPrice);

                seatTypeDetails.add(new SeatTypePricingDetail(
                    passenger.getFirstName() + " " + passenger.getLastName(),
                    passenger.getSeat().getSeatNumber(),
                    passenger.getSeat().getType(),
                    additionalPrice
                ));
            }
        }

        response.setSeatTypeAmount(totalSeatTypeAmount);
        response.setSeatTypeDetails(seatTypeDetails);

        log.debug("Populated seat type information: {} details, total amount: {}",
                seatTypeDetails.size(), totalSeatTypeAmount);
    }

    private void sendHoldTimeWarningEmail(Booking booking, int minutesLeft) {
        if (booking.getUserId() == null) {
            log.debug("Skipping hold time warning email for guest booking {}", booking.getBookingId());
            return;
        }

        try {
            String email = booking.getUserId().getEmail();
            String subject = "Cảnh báo: Thời gian giữ ghế sắp hết hạn";

            String body = String.format(
                "<h3>Xin chào %s</h3>" +
                "<p>Thời gian giữ ghế cho đơn đặt vé <strong>%s</strong> của bạn sắp hết hạn.</p>" +
                "<p><strong>Thời gian còn lại:</strong> %d phút</p>" +
                "<p><strong>Chuyến bay:</strong> %s</p>" +
                "<p><strong>Khởi hành:</strong> %s</p>" +
                "<p>Vui lòng hoàn tất thanh toán trong thời gian còn lại để giữ ghế của bạn.</p>" +
                "<p>Nếu không thanh toán, ghế sẽ được giải phóng tự động.</p>" +
                "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.</p>",
                booking.getUserId().getLastName(),
                booking.getBookingCode(),
                minutesLeft,
                booking.getFlight().getFlightNumber(),
                booking.getFlight().getDepartureTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );

            emailService.sendEmail(email, subject, body);
            log.info("Sent hold time warning email to {} for booking {} ({} minutes left)",
                    email, booking.getBookingId(), minutesLeft);

        } catch (Exception e) {
            log.error("Failed to send hold time warning email for booking {}: {}",
                    booking.getBookingId(), e.getMessage());
        }
    }

    /**
     * Scheduled job để xử lý các booking đã hết thời hạn thanh toán
     * Chạy mỗi phút để check và cancel expired bookings
     */
    @Scheduled(fixedRate = 60000) // 60 seconds = 1 minute
    @Transactional
    public void processExpiredPayments() {
        log.info("Checking for expired payments...");

        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(now, BookingStatus.PENDING);

        if (expiredBookings.isEmpty()) {
            log.debug("No expired bookings found");
            return;
        }

        log.info("Found {} expired bookings to process", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                cancelExpiredBooking(booking);
                log.info("Successfully cancelled expired booking: {}", booking.getBookingCode());
            } catch (Exception e) {
                log.error("Failed to cancel expired booking {}: {}", booking.getBookingId(), e.getMessage());
            }
        }
    }

    /**
     * Hủy booking đã hết thời hạn thanh toán
     */
    private void cancelExpiredBooking(Booking booking) {
        log.info("Cancelling expired booking: {} (timeout: {})",
                booking.getBookingCode(), booking.getPaymentTimeout());

        // 1. Cập nhật payment status thành EXPIRED
        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(booking.getPayment());
            log.debug("Updated payment status to EXPIRED for booking {}", booking.getBookingCode());
        }

        // 2. Cập nhật booking status thành CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // 3. Xóa passenger records và giải phóng ghế
        int releasedSeats = 0;
        List<Passenger> passengersToDelete = new ArrayList<>(booking.getPassengers());
        for (Passenger passenger : passengersToDelete) {
            if (passenger.getSeat() != null) {
                passenger.getSeat().setStatus(SeatStatus.AVAILABLE);
                passenger.getSeat().setBookedBy(null);
                seatRepository.save(passenger.getSeat());
                releasedSeats++;
                log.debug("Released seat {} for expired booking {}", passenger.getSeat().getSeatNumber(), booking.getBookingCode());
            }
        }

        // Xóa tất cả passengers của booking này
        if (!passengersToDelete.isEmpty()) {
            passengerRepository.deleteAll(passengersToDelete);
            log.info("Deleted {} passenger records for expired booking {}", passengersToDelete.size(), booking.getBookingCode());
        }

        // 4. Cập nhật availableSeats của flight
        if (releasedSeats > 0 && booking.getFlight() != null) {
            Flight flight = booking.getFlight();
            flight.setAvailableSeats(flight.getAvailableSeats() + releasedSeats);
            flightRepository.save(flight);
            log.info("Updated available seats for flight {}: +{} (expired booking {})",
                    flight.getFlightNumber(), releasedSeats, booking.getBookingCode());
        }

        // 5. Gửi email thông báo cho user
        if (booking.getUserId() != null) {
            sendExpiredPaymentEmail(booking);
        }

        log.info("Successfully cancelled expired booking {} and released {} seats",
                booking.getBookingCode(), releasedSeats);
    }

    /**
     * Gửi email thông báo booking bị hủy do hết thời hạn thanh toán
     */
    private void sendExpiredPaymentEmail(Booking booking) {
        try {
            String email = booking.getUserId().getEmail();
            String subject = "Thông báo: Booking đã bị hủy do hết thời hạn thanh toán";

            String body = String.format(
                "<h3>Xin chào %s</h3>" +
                "<p>Mã đặt vé: <strong>%s</strong></p>" +
                "<p>Booking của bạn đã bị <strong>HỦY TỰ ĐỘNG</strong> vì đã hết thời hạn thanh toán.</p>" +
                "<p><strong>Thời hạn thanh toán:</strong> %s</p>" +
                "<p><strong>Chuyến bay:</strong> %s</p>" +
                "<p><strong>Số tiền:</strong> $%.2f</p>" +
                "<p>Ghế đã được giải phóng và có thể được đặt lại bởi khách hàng khác.</p>" +
                "<p>Nếu bạn vẫn muốn đặt vé, vui lòng tạo booking mới.</p>" +
                "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.</p>",
                booking.getUserId().getLastName(),
                booking.getBookingCode(),
                booking.getPaymentTimeout().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                booking.getFlight().getFlightNumber(),
                booking.getTotalAmount()
            );

            emailService.sendEmail(email, subject, body);
            log.info("Sent expired payment email to {} for booking {}", email, booking.getBookingCode());

        } catch (Exception e) {
            log.error("Failed to send expired payment email for booking {}: {}",
                    booking.getBookingId(), e.getMessage());
        }
    }

    @Override
    public List<CheckinEligiblePassengerResponse> getCheckinEligiblePassengers(String bookingCode, String fullName) {
        log.info("Getting check-in eligible passengers for booking: {} and passenger: {}", bookingCode, fullName);

        // First verify booking exists and user has access
        Optional<Booking> bookingOpt = bookingRepository.findByBookingCodeAndPassengerFullName(bookingCode, fullName);
        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException("Booking not found or access denied");
        }

        Booking booking = bookingOpt.get();

        // Check if booking is paid (required for check-in)
        boolean isPaid = booking.getPayment() != null &&
                        booking.getPayment().getStatus() == PaymentStatus.COMPLETED;

        if (!isPaid) {
            throw new IllegalStateException("Booking must be paid before check-in");
        }

        // Check if flight is eligible for check-in (not departed, within check-in window)
        LocalDateTime now = LocalDateTime.now();
        boolean canCheckIn = booking.getFlight().getDepartureTime().isAfter(now.plusHours(1)) &&
                           booking.getFlight().getDepartureTime().isBefore(now.plusDays(1));

        if (!canCheckIn) {
            throw new IllegalStateException("Check-in not available for this flight at this time");
        }

        // Get all passengers and their check-in status
        return booking.getPassengers().stream()
                .map(passenger -> {
                    CheckinEligiblePassengerResponse response = new CheckinEligiblePassengerResponse();
                    response.setPassengerId(passenger.getPassengerId());
                    response.setFirstName(passenger.getFirstName());
                    response.setLastName(passenger.getLastName());
                    response.setFullName(passenger.getFirstName() + " " + passenger.getLastName());
                    response.setPassportNumber(passenger.getPassportNumber());
                    response.setSeatNumber(passenger.getSeat() != null ? passenger.getSeat().getSeatNumber() : null);

                    // Calculate ticket price per passenger based on passenger type
                    BigDecimal ticketPrice = calculatePassengerTicketPrice(passenger, booking);
                    response.setTicketPrice(ticketPrice);

                    // Check if already checked in (COMPLETED status)
                    boolean alreadyCheckedIn = checkinRepository.existsByPassengerAndCompleted(passenger);
                    response.setCheckedIn(alreadyCheckedIn);

                    // Set checkin status based on booking status first
                    if (booking.getStatus() == BookingStatus.CANCELLED) {
                        response.setCheckinStatus(CheckinStatus.BOOKING_CANCELLED);
                    } else if (alreadyCheckedIn) {
                        response.setCheckinStatus(CheckinStatus.ALREADY_CHECKED_IN);
                    } else if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
                        response.setCheckinStatus(CheckinStatus.BOOKING_NOT_CONFIRMED);
                    } else if (!isPaid) {
                        response.setCheckinStatus(CheckinStatus.PAYMENT_PENDING);
                    } else {
                        response.setCheckinStatus(CheckinStatus.ELIGIBLE);
                    }

                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<CheckinEligiblePassengerResponse> getPassengersWithCheckinStatus(String bookingCode, String fullName) {
        log.info("Getting all passengers with check-in status for booking: {} and passenger: {}", bookingCode, fullName);

        // First verify booking exists and user has access
        Optional<Booking> bookingOpt = bookingRepository.findByBookingCodeAndPassengerFullName(bookingCode, fullName);
        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException("Booking not found or access denied");
        }

        Booking booking = bookingOpt.get();

        // Check if booking is paid
        boolean isPaid = booking.getPayment() != null &&
                        booking.getPayment().getStatus() == PaymentStatus.COMPLETED;

        // Get all passengers and their check-in status
        return booking.getPassengers().stream()
                .map(passenger -> {
                    CheckinEligiblePassengerResponse response = new CheckinEligiblePassengerResponse();
                    response.setPassengerId(passenger.getPassengerId());
                    response.setFirstName(passenger.getFirstName());
                    response.setLastName(passenger.getLastName());
                    response.setFullName(passenger.getFirstName() + " " + passenger.getLastName());
                    response.setPassportNumber(passenger.getPassportNumber());
                    response.setSeatNumber(passenger.getSeat() != null ? passenger.getSeat().getSeatNumber() : null);

                    // Calculate ticket price per passenger based on passenger type
                    BigDecimal ticketPrice = calculatePassengerTicketPrice(passenger, booking);
                    response.setTicketPrice(ticketPrice);

                    // Check if already checked in (COMPLETED status)
                    boolean alreadyCheckedIn = checkinRepository.existsByPassengerAndCompleted(passenger);
                    response.setCheckedIn(alreadyCheckedIn);

                    // Set checkin status based on booking status first
                    if (booking.getStatus() == BookingStatus.CANCELLED) {
                        response.setCheckinStatus(CheckinStatus.BOOKING_CANCELLED);
                    } else if (alreadyCheckedIn) {
                        response.setCheckinStatus(CheckinStatus.ALREADY_CHECKED_IN);
                    } else if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
                        response.setCheckinStatus(CheckinStatus.BOOKING_NOT_CONFIRMED);
                    } else if (!isPaid) {
                        response.setCheckinStatus(CheckinStatus.PAYMENT_PENDING);
                    } else {
                        // Check if flight is eligible for check-in
                        LocalDateTime now = LocalDateTime.now();
                        boolean canCheckIn = booking.getFlight().getDepartureTime().isAfter(now.plusHours(1)) &&
                                           booking.getFlight().getDepartureTime().isBefore(now.plusDays(1));
                        response.setCheckinStatus(canCheckIn ? CheckinStatus.ELIGIBLE : CheckinStatus.NOT_AVAILABLE);
                    }

                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Tính giá vé cho từng passenger dựa trên loại passenger
     * Trong thực tế nên có bảng giá riêng cho từng loại passenger
     */
    private BigDecimal calculatePassengerTicketPrice(Passenger passenger, Booking booking) {
        if (booking.getTotalAmount() == null || booking.getPassengers().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Tạm thời chia đều - trong thực tế nên có logic tính giá riêng:
        // - ADULT: 100% giá cơ bản
        // - CHILD: 75% giá cơ bản
        // - INFANT: 10% giá cơ bản
        return booking.getTotalAmount().divide(
            BigDecimal.valueOf(booking.getPassengers().size()),
            2, java.math.RoundingMode.HALF_UP
        );
    }

    @Override
    @Transactional
    public CheckinResponse processCheckin(CheckinRequest request) {
        log.info("Processing check-in for booking: {}, passenger: {}",
                request.getBookingCode(), request.getPassengerFullName());

        // Validate booking and passenger
        Optional<Booking> bookingOpt = bookingRepository.findByBookingCodeAndPassengerFullName(
                request.getBookingCode(), request.getPassengerFullName());
        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException("Booking not found or access denied");
        }

        Booking booking = bookingOpt.get();

        // **CẬP NHẬT: Validate booking status - phải là CONFIRMED hoặc COMPLETED**
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking must be confirmed before check-in. Current status: " + booking.getStatus());
        }

        // Check if booking is paid (required for check-in)
        boolean isPaid = booking.getPayment() != null &&
                        booking.getPayment().getStatus() == PaymentStatus.COMPLETED;
        if (!isPaid) {
            throw new IllegalStateException("Booking must be paid before check-in");
        }

        // Check if flight is eligible for check-in (not departed, within check-in window)
        LocalDateTime now = LocalDateTime.now();
        boolean canCheckIn = booking.getFlight().getDepartureTime().isAfter(now.plusHours(1)) &&
                           booking.getFlight().getDepartureTime().isBefore(now.plusDays(1));
        if (!canCheckIn) {
            throw new IllegalStateException("Check-in not available for this flight at this time");
        }

        Passenger passenger = booking.getPassengers().stream()
                .filter(p -> p.getPassengerId().equals(request.getPassengerId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));

        // Check if passenger is already checked in (COMPLETED status)
        boolean alreadyCheckedIn = checkinRepository.existsByPassengerAndCompleted(passenger);
        if (alreadyCheckedIn) {
            throw new IllegalStateException("Passenger is already checked in");
        }

        CheckinResponse response = new CheckinResponse();
        response.setPassengerId(passenger.getPassengerId());
        response.setPassengerName(passenger.getFirstName() + " " + passenger.getLastName());
        response.setOldSeatNumber(passenger.getSeat() != null ? passenger.getSeat().getSeatNumber() : null);

        BigDecimal totalCharge = BigDecimal.ZERO;

        // Handle seat change if requested
        if (request.getNewSeatNumber() != null && !request.getNewSeatNumber().equals(response.getOldSeatNumber())) {
            BigDecimal seatCharge = handleSeatChange(booking, passenger, request.getNewSeatNumber());
            response.setSeatChangeCharge(seatCharge);
            response.setNewSeatNumber(request.getNewSeatNumber());
            totalCharge = totalCharge.add(seatCharge);
        }

        // Handle ancillary services updates
        if (request.getServicesToAdd() != null && !request.getServicesToAdd().isEmpty()) {
            BigDecimal servicesCharge = handleAddAncillaryServices(booking, passenger, request.getServicesToAdd());
            response.setServicesAddedCharge(servicesCharge);
            totalCharge = totalCharge.add(servicesCharge);
        }

        if (request.getServiceIdsToRemove() != null && !request.getServiceIdsToRemove().isEmpty()) {
            BigDecimal servicesRefund = handleRemoveAncillaryServices(booking, passenger, request.getServiceIdsToRemove());
            response.setServicesRemovedRefund(servicesRefund);
            totalCharge = totalCharge.add(servicesRefund); // Refund is negative
        }

        response.setTotalCharge(totalCharge);

        // Update booking total amount
        BigDecimal newTotal = booking.getTotalAmount().add(totalCharge);
        booking.setTotalAmount(newTotal);
        bookingRepository.save(booking);

        response.setUpdatedTotalAmount(newTotal);
        response.setStatus("SUCCESS");
        response.setMessage("Check-in processed successfully");

        // Update check-in status to COMPLETED
        Optional<CheckIn> existingCheckIn = checkinRepository.findByBookingIdWithBaggage(booking.getBookingId()).stream()
                .filter(ci -> ci.getPassenger().equals(passenger))
                .findFirst();

        if (existingCheckIn.isPresent()) {
            CheckIn checkIn = existingCheckIn.get();
            checkIn.setStatus(CheckinStatus.COMPLETED);
            checkIn.setCheckedAt(LocalDateTime.now());
            checkinRepository.save(checkIn);
        }

        return response;
    }

    private BigDecimal handleSeatChange(Booking booking, Passenger passenger, String newSeatNumber) {
        // Find the new seat
        Seat newSeat = seatRepository.findByFlightIdAndSeatNumberForUpdate(booking.getFlight().getFlightId(), newSeatNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + newSeatNumber));

        // Check if seat is available
        if (!SeatStatus.AVAILABLE.equals(newSeat.getStatus())) {
            throw new IllegalStateException("Seat is not available: " + newSeatNumber);
        }

        // Calculate price difference
        BigDecimal oldSeatPrice = calculateSeatPrice(passenger.getSeat());
        BigDecimal newSeatPrice = calculateSeatPrice(newSeat);
        BigDecimal priceDifference = newSeatPrice.subtract(oldSeatPrice);

        // Update passenger seat
        passenger.setSeat(newSeat);
        passengerRepository.save(passenger);

        // Update seat status
        if (passenger.getSeat() != null) {
            passenger.getSeat().setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(passenger.getSeat());
        }
        newSeat.setStatus(SeatStatus.OCCUPIED);
        seatRepository.save(newSeat);

        return priceDifference;
    }

    private BigDecimal handleAddAncillaryServices(Booking booking, Passenger passenger,
                                                 List<BookingAncillaryServiceRequest> servicesToAdd) {
        BigDecimal totalCharge = BigDecimal.ZERO;

        for (BookingAncillaryServiceRequest serviceRequest : servicesToAdd) {
            AncillaryService service = ancillaryServiceRepository.findById(serviceRequest.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ancillary service not found: " + serviceRequest.getServiceId()));

            // Check if service already exists for this passenger
            List<BookingAncillaryService> existingServices = bookingAncillaryServiceRepository.findByBookingId(booking.getBookingId());
            boolean alreadyExists = existingServices.stream()
                    .anyMatch(bs -> bs.getAncillaryService().getServiceId().equals(serviceRequest.getServiceId()) &&
                                   (bs.getPassenger() == null || bs.getPassenger().equals(passenger)));

            if (alreadyExists) {
                continue; // Skip if already exists
            }

            // Create new booking ancillary service
            BookingAncillaryService bookingService = new BookingAncillaryService();
            bookingService.setBooking(booking);
            bookingService.setAncillaryService(service);
            bookingService.setPassenger(passenger);
            bookingService.setQuantity(serviceRequest.getQuantity());
            bookingService.setUnitPrice(service.getPrice());
            bookingService.setTotalPrice(service.getPrice().multiply(BigDecimal.valueOf(serviceRequest.getQuantity())));
            bookingService.setNotes(serviceRequest.getNotes());

            bookingAncillaryServiceRepository.save(bookingService);

            totalCharge = totalCharge.add(bookingService.getTotalPrice());
        }

        return totalCharge;
    }

    private BigDecimal handleRemoveAncillaryServices(Booking booking, Passenger passenger,
                                                   List<Long> serviceIdsToRemove) {
        BigDecimal totalRefund = BigDecimal.ZERO;

        List<BookingAncillaryService> existingServices = bookingAncillaryServiceRepository.findByBookingId(booking.getBookingId());

        for (Long serviceId : serviceIdsToRemove) {
            BookingAncillaryService bookingService = existingServices.stream()
                    .filter(bs -> bs.getAncillaryService().getServiceId().equals(serviceId) &&
                                (bs.getPassenger() == null || bs.getPassenger().equals(passenger)))
                    .findFirst()
                    .orElse(null);

            if (bookingService != null) {
                totalRefund = totalRefund.add(bookingService.getTotalPrice());
                bookingAncillaryServiceRepository.delete(bookingService);
            }
        }

        return totalRefund.negate(); // Return as negative for refund
    }

    private BigDecimal calculateSeatPrice(Seat seat) {
        if (seat == null) return BigDecimal.ZERO;

        // Use the predefined seat type pricing from enum
        SeatTypePrice seatTypePrice = SeatTypePrice.fromSeatType(seat.getType());
        return seatTypePrice.getAdditionalPrice();
    }

}

