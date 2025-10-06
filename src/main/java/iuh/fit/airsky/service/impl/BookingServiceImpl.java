package iuh.fit.airsky.service.impl;
import iuh.fit.airsky.dto.request.BookingAncillaryServiceRequest;
import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.request.FlightSegmentRequest;
import iuh.fit.airsky.dto.request.PassengerSeatRequest;
import iuh.fit.airsky.dto.response.BaggageResponse;
import iuh.fit.airsky.dto.response.BookingAncillaryServiceResponse;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.BaggageType;
import iuh.fit.airsky.enums.BaggagePackage;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.PaymentStatus;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.enums.LoyaltyTier;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.BookingMapper;
import iuh.fit.airsky.mapper.PassengerMapper;
import iuh.fit.airsky.model.*;
import iuh.fit.airsky.repository.*;
import iuh.fit.airsky.service.BookingService;
import iuh.fit.airsky.service.DealService;
import iuh.fit.airsky.service.EmailService;
import lombok.extern.slf4j.Slf4j;
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
    private final DealUsageRepository dealUsageRepository;
    private final PassengerMapper passengerMapper;
    private final AncillaryServiceRepository ancillaryServiceRepository;
    private final BookingAncillaryServiceRepository bookingAncillaryServiceRepository;
    
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
            DealUsageRepository dealUsageRepository,
            PassengerMapper passengerMapper,
            AncillaryServiceRepository ancillaryServiceRepository,
            BookingAncillaryServiceRepository bookingAncillaryServiceRepository
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
        this.dealUsageRepository = dealUsageRepository;
        this.passengerMapper = passengerMapper;
        this.ancillaryServiceRepository = ancillaryServiceRepository;
        this.bookingAncillaryServiceRepository = bookingAncillaryServiceRepository;
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
        booking.setTravelClass(travelClassRepository.findById(firstSegment.getClassId())
                .orElseThrow(() -> {
                    log.error("TravelClass not found with id: {}", firstSegment.getClassId());
                    return new ResourceNotFoundException("Không tìm thấy TravelClass");
                }));
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setHoldTime(LocalDateTime.now());

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

        // Calculate ancillary services amount
        BigDecimal ancillaryServicesAmount = calculateAncillaryServicesAmount(request.getAncillaryServices(), savedBooking.getPassengers());
        
        BigDecimal finalAmount = baseAmount.add(baggageAmount).add(ancillaryServicesAmount);
        log.info("Base amount: {}, Baggage amount: {}, Ancillary services amount: {}, Total before deal: {}", 
                baseAmount, baggageAmount, ancillaryServicesAmount, finalAmount);

        if (request.getDealCode() != null && !request.getDealCode().trim().isEmpty()) {
            log.info("Applying deal: {}", request.getDealCode());
            try {
                var dealUsage = dealService.applyDeal(request.getDealCode(), request.getUserId(), savedBooking.getBookingId(), finalAmount);
                finalAmount = dealUsage.getFinalAmount();
                log.info("Applied deal {} to booking {}, discount: {}", request.getDealCode(), savedBooking.getBookingId(), dealUsage.getDiscountAmount());
            } catch (Exception e) {
                log.warn("Failed to apply deal {}: {}", request.getDealCode(), e.getMessage());
                // Tiếp tục với amount gốc nếu deal thất bại
            }
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
        
        // Reload booking with full details for response (EntityGraph will load checkIns.baggage)
        Booking reloadedBooking = bookingRepository.findById(savedBooking.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        
        log.info("Booking {} has {} checkIns", reloadedBooking.getBookingId(), 
                reloadedBooking.getCheckIns() != null ? reloadedBooking.getCheckIns().size() : 0);
        
        BookingResponse response = bookingMapper.toResponseDTO(reloadedBooking);
        populateDealInformation(response, reloadedBooking);
        populateBaggageInformation(response, reloadedBooking);
        populateAncillaryServicesInformation(response, reloadedBooking);
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
            return Optional.of(response);
        }

        return Optional.empty();
    }

    @Override
    public PageResponse<BookingResponse> findAll(Pageable pageable) {
        log.info("Finding all bookings with pageable: {}", pageable);

        Page<Booking> bookingPage = bookingRepository.findAll(pageable);
        List<BookingResponse> bookingResponses = bookingPage.getContent().stream()
                .map(booking -> {
                    BookingResponse response = bookingMapper.toResponseDTO(booking);
                    populateDealInformation(response, booking);
                    return response;
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
        if (booking.getPayment() == null || booking.getPayment().getStatus() != PaymentStatus.SUCCESS) {
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

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkPendingBookings() {
        List<Booking> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING);
        for (Booking booking : pendingBookings) {
            if (booking.getHoldTime() == null) continue;
            if (Duration.between(booking.getHoldTime(), LocalDateTime.now()).toMinutes() > 15) {
                booking.setStatus(BookingStatus.CANCELLED);
                for (Passenger passenger : booking.getPassengers()) {
                    if (passenger.getSeat() != null) {
                        passenger.getSeat().setStatus(SeatStatus.AVAILABLE);
                    }
                }
                bookingRepository.save(booking);
            }
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
        if (dealUsageOpt.isPresent()) {
            DealUsage dealUsage = dealUsageOpt.get();
            response.setAppliedDealCode(dealUsage.getDeal().getDealCode());
            response.setDiscountPercentage(dealUsage.getDeal().getDiscountPercentage());
            response.setDiscountAmount(dealUsage.getDiscountAmount());
        } else {
            response.setAppliedDealCode(null);
            response.setDiscountPercentage(null);
            response.setDiscountAmount(BigDecimal.ZERO);
        }
    }
    
    private void populateBaggageInformation(BookingResponse response, Booking booking) {
        List<BaggageResponse> baggageList = new ArrayList<>();
        
        if (booking.getCheckIns() != null && !booking.getCheckIns().isEmpty()) {
            log.info("Populating baggage info for {} checkIns", booking.getCheckIns().size());
            
            for (CheckIn checkIn : booking.getCheckIns()) {
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
                    segment.setTravelClass(travelClassRepository.findById(segmentRequest.getClassId())
                            .orElseThrow(() -> new ResourceNotFoundException("Travel class not found")));
                    
                    // Set airport information from the flight
                    segment.setDepartureAirport(departureAirport);
                    segment.setArrivalAirport(arrivalAirport);
                    
                    // Set price from database FlightTravelClass, not from request
                    BigDecimal segmentPrice = flight.getFlightTravelClasses().stream()
                            .filter(ftc -> ftc.getTravelClass().getClassId().equals(segmentRequest.getClassId()))
                            .map(ftc -> ftc.getCustomPrice() != null ? ftc.getCustomPrice() : flight.getBasePrice())
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
}


