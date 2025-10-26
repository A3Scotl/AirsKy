package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.BookingAncillaryServiceRequest;
import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.request.CheckinRequest;
import iuh.fit.airsky.dto.request.FlightSegmentRequest;
import iuh.fit.airsky.dto.request.PassengerSeatRequest;
import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.request.UpdateBookingTotalRequest;
import iuh.fit.airsky.dto.request.PointsRedemptionRequest;
import iuh.fit.airsky.dto.request.SeatChangeCalculationRequest;
import iuh.fit.airsky.dto.response.AvailableSeatsResponse;
import iuh.fit.airsky.dto.response.BaggageResponse;
import iuh.fit.airsky.dto.response.BookingAncillaryServiceResponse;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.FlightSegmentResponse;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.PassengerSeatResponse;
import iuh.fit.airsky.dto.response.PassengerSeatAssignmentResponse;
import iuh.fit.airsky.dto.response.PaymentResponse;
import iuh.fit.airsky.dto.response.SeatTypePricingDetail;
import iuh.fit.airsky.dto.response.CheckinEligiblePassengerResponse;
import iuh.fit.airsky.dto.response.CheckinResponse;
import iuh.fit.airsky.dto.response.SeatChangeCalculationResponse;
import iuh.fit.airsky.dto.response.UpdateBookingTotalResponse;
import iuh.fit.airsky.dto.response.DealResponse;
import iuh.fit.airsky.service.PointsRedemptionService;
import iuh.fit.airsky.enums.BaggageType;
import iuh.fit.airsky.enums.BaggagePackage;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.PaymentStatus;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.enums.LoyaltyTier;
import iuh.fit.airsky.enums.PaymentMethod;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.SeatTypePrice;
import iuh.fit.airsky.enums.SeatTypes;
import iuh.fit.airsky.enums.CheckinStatus;
import iuh.fit.airsky.enums.PassengerType;
import iuh.fit.airsky.enums.NotificationType;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.exception.SeatNotAvailableException;
import iuh.fit.airsky.event.BookingCancelledEvent;
import iuh.fit.airsky.mapper.AirportMapper;
import iuh.fit.airsky.mapper.BookingMapper;
import iuh.fit.airsky.mapper.PaymentMapper;
import iuh.fit.airsky.mapper.PassengerMapper;
import iuh.fit.airsky.model.*;
import iuh.fit.airsky.repository.*;
import iuh.fit.airsky.service.BookingService;
import iuh.fit.airsky.service.DealService;
import iuh.fit.airsky.service.EmailService;
import iuh.fit.airsky.service.LoyaltyService;
import iuh.fit.airsky.service.NotificationService;
import iuh.fit.airsky.service.PaymentService;
import iuh.fit.airsky.service.BoardingPassService;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import iuh.fit.airsky.service.impl.EmailTemplateGenerator;
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
    private final BoardingPassService boardingPassService;
    private final LoyaltyService loyaltyService;
    private final DealUsageRepository dealUsageRepository;
    private final PassengerRepository passengerRepository;
    private final PassengerMapper passengerMapper;
    private final PaymentMapper paymentMapper;
    private final AirportMapper airportMapper;
    private final AncillaryServiceRepository ancillaryServiceRepository;
    private final BookingAncillaryServiceRepository bookingAncillaryServiceRepository;
    private final FlightTravelClassRepository flightTravelClassRepository;
    private final PassengerSeatAssignmentRepository passengerSeatAssignmentRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final PointsRedemptionService pointsRedemptionService;


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
            BoardingPassService boardingPassService,
            LoyaltyService loyaltyService,
            DealUsageRepository dealUsageRepository,
            PassengerRepository passengerRepository,
            PassengerMapper passengerMapper,
            PaymentMapper paymentMapper,
            AirportMapper airportMapper,
            AncillaryServiceRepository ancillaryServiceRepository,
            BookingAncillaryServiceRepository bookingAncillaryServiceRepository,
            FlightTravelClassRepository flightTravelClassRepository,
            PassengerSeatAssignmentRepository passengerSeatAssignmentRepository,
            NotificationService notificationService,
            ApplicationEventPublisher eventPublisher,
            PointsRedemptionServiceImpl pointsRedemptionService
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
        this.boardingPassService = boardingPassService;
        this.loyaltyService = loyaltyService;
        this.dealUsageRepository = dealUsageRepository;
        this.passengerRepository = passengerRepository;
        this.passengerMapper = passengerMapper;
        this.paymentMapper = paymentMapper;
        this.airportMapper = airportMapper;
        this.ancillaryServiceRepository = ancillaryServiceRepository;
        this.bookingAncillaryServiceRepository = bookingAncillaryServiceRepository;
        this.flightTravelClassRepository = flightTravelClassRepository;
        this.passengerSeatAssignmentRepository = passengerSeatAssignmentRepository;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.pointsRedemptionService = pointsRedemptionService;
    }

    /**
     * Tạo một booking mới.
     * 1. Xác thực yêu cầu (request validation).
     * 2. Khởi tạo và lưu các thực thể chính (Booking, Passenger).
     * 3. Giữ chỗ và cập nhật số lượng ghế trống của chuyến bay.
     * 4. Tạo các thực thể liên quan (FlightSegment, SeatAssignment, AncillaryService).
     * 5. Tính toán tổng số tiền cuối cùng, bao gồm cả giảm giá (deal, tier).
     * 6. Tạo bản ghi thanh toán (Payment).
     * 7. Tạo các bản ghi check-in ban đầu (trạng thái PENDING).
     * 8. Lưu lại toàn bộ và trả về kết quả.
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        try {
            log.info("Starting booking creation for userId: {}", request.getUserId());

            // Bước 1: Xác thực yêu cầu và kiểm tra tính khả dụng của chuyến bay
            validateBookingRequest(request);

            // Bước 2: Khởi tạo thực thể Booking và các Passenger liên quan
            Booking booking = initializeBooking(request);
            List<Passenger> passengers = mapPassengers(request, booking);
            booking.setPassengers(passengers);

            // Bước 3: Giữ chỗ bằng cách giảm số lượng ghế trống trên các chuyến bay
            for (FlightSegmentRequest segment : request.getFlightSegments()) {
                Flight segFlight = findFlightById(segment.getFlightId());
                updateAvailableSeats(segFlight, passengers.size());
            }

            // Lưu Booking để lấy ID, cần thiết cho các thực thể liên quan
            Booking savedBooking = bookingRepository.save(booking);

            // Bước 4: Tạo các thực thể liên quan: FlightSegments, SeatAssignments, AncillaryServices
            List<FlightSegment> flightSegments = createFlightSegments(request, savedBooking);
            savedBooking.setFlightSegments(flightSegments);
            createSeatAssignments(request, savedBooking);
            createBookingAncillaryServices(request, savedBooking);

            // Xử lý điểm thưởng trước khi tính toán tổng tiền
            Integer pointsToRedeem = request.getPointsToRedeem();
            DealResponse redeemedDeal = null;
            if (pointsToRedeem != null && pointsToRedeem > 0 && request.getUserId() != null) {
                if (!pointsRedemptionService.canRedeemPoints(request.getUserId(), pointsToRedeem)) {
                    throw new IllegalArgumentException("Không đủ điểm để sử dụng cho booking này");
                }
                // Redeem points và tạo deal
                PointsRedemptionRequest redemptionRequest = new PointsRedemptionRequest();
                redemptionRequest.setUserId(request.getUserId());
                redemptionRequest.setPointsToRedeem(pointsToRedeem);
                BigDecimal discountAmount = pointsRedemptionService.calculateDiscountFromPoints(pointsToRedeem);
                redemptionRequest.setDiscountAmount(discountAmount.longValue());
                redeemedDeal = pointsRedemptionService.redeemPointsForDeal(redemptionRequest);
                log.info("Successfully redeemed {} points for user {}, created deal: {}", pointsToRedeem, request.getUserId(), redeemedDeal.getDealCode());
            }

            // Bước 5: Tính toán tổng số tiền cuối cùng, bao gồm các dịch vụ và giảm giá
            BigDecimal finalAmount = calculateFinalAmount(request, savedBooking, redeemedDeal);
            savedBooking.setTotalAmount(finalAmount);

            // Bước 6: Tạo bản ghi thanh toán cho booking
            Payment paymentEntity = createPaymentForBooking(savedBooking, finalAmount, request.getPaymentMethod());
            savedBooking.setPayment(paymentEntity);

            // Bước 7: Tạo các bản ghi check-in ban đầu với trạng thái PENDING
            createInitialCheckinRecords(savedBooking, passengers, request);

            // Bước 7: Lưu lại toàn bộ các thay đổi và chuẩn bị response
            Booking finalBooking = bookingRepository.save(savedBooking);
            log.info("Booking creation completed successfully for bookingId: {}", finalBooking.getBookingId());

            // Dọn dẹp EntityManager để đảm bảo response trả về dữ liệu mới nhất từ DB
            entityManager.flush();
            entityManager.clear();

            Booking reloadedBooking = bookingRepository.findById(finalBooking.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Failed to reload booking after creation"));

            BookingResponse response = buildBookingResponse(reloadedBooking);
            // Gắn thông tin điểm đã sử dụng vào response
            if (redeemedDeal != null) {
                response.setPointsRedeemed(redeemedDeal.getPointsRequired());
                response.setPointsDiscountAmount(redeemedDeal.getFixedDiscountAmount());
            }
            return response;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure during booking creation: {}", e.getMessage());
            // Ném ra một ngoại lệ tùy chỉnh hoặc một thông báo lỗi rõ ràng hơn cho lớp controller
            throw new IllegalStateException("Thông tin chuyến bay vừa được cập nhật bởi một người dùng khác. Vui lòng thử lại.");
        } catch (Exception e) {
        log.error("Booking creation failed with error: {}", e.getMessage(), e);
        throw e; // Re-throw to maintain transaction rollback
    }
}

    private void validateBookingRequest(BookingRequest request) {
        if (request.getFlightSegments() == null || request.getFlightSegments().isEmpty()) {
            throw new IllegalArgumentException("Flight segments cannot be empty");
        } 

        Flight firstFlight = findFlightById(request.getFlightSegments().get(0).getFlightId());

        if (Duration.between(LocalDateTime.now(), firstFlight.getDepartureTime()).toHours() < 2) {
            throw new IllegalArgumentException("Bạn chỉ có thể đặt vé trước giờ khởi hành ít nhất 2 tiếng");
        }

        int totalPassengers = request.getPassengers().size();
        if (totalPassengers == 0) throw new IllegalArgumentException("Booking must have at least one passenger.");
        for (FlightSegmentRequest segment : request.getFlightSegments()) {
            Flight segFlight = flightRepository.findById(segment.getFlightId())
                    .orElseThrow(() -> new ResourceNotFoundException("Flight not found for segment"));
            if (segFlight.getAvailableSeats() < totalPassengers) {
                throw new IllegalStateException("Không đủ ghế trống cho chặng bay " + segment.getSegmentOrder());
            }
        }

    // Thêm validation cho guest booking: phải có contactEmail và contactName
    if (request.getUserId() == null) {
        if (request.getContactEmail() == null || request.getContactEmail().isBlank()) {
            throw new IllegalArgumentException("Email liên hệ là bắt buộc đối với đặt vé của khách.");
        }
        if (request.getContactName() == null || request.getContactName().isBlank()) {
            throw new IllegalArgumentException("Tên người liên hệ là bắt buộc đối với đặt vé của khách.");
        }
    }
    }

    private Booking initializeBooking(BookingRequest request) {
        Booking booking = bookingMapper.toEntity(request);

        if (request.getUserId() != null) {
            userRepository.findById(request.getUserId()).ifPresent(booking::setUserId);
        }

        // Thêm email liên hệ từ request
        if (request.getContactEmail() != null && !request.getContactEmail().isBlank()) {
            booking.setContactEmail(request.getContactEmail());
        }
        // Thêm tên liên hệ từ request
        if (request.getContactName() != null && !request.getContactName().isBlank()) {
            booking.setContactName(request.getContactName());
        }

        Flight firstFlight = findFlightById(request.getFlightSegments().get(0).getFlightId());
        booking.setFlight(firstFlight);

        FlightTravelClass flightTravelClass = flightTravelClassRepository.findById(request.getFlightSegments().get(0).getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("FlightTravelClass not found"));
        booking.setTravelClass(flightTravelClass.getTravelClass());

        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setHoldTime(LocalDateTime.now());
        booking.setPaymentTimeout(LocalDateTime.now().plusMinutes(45));

        return booking;
    }

    private Flight findFlightById(Long flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + flightId));
    }

    private BigDecimal calculateFinalAmount(BookingRequest request, Booking savedBooking, DealResponse redeemedDeal) {
        // 1. Base amount from flight segments - tính theo từng loại hành khách
        BigDecimal baseAmount = BigDecimal.ZERO;
        for (FlightSegment segment : savedBooking.getFlightSegments()) {
            BigDecimal segmentBasePrice = segment.getPrice();
            for (PassengerSeatRequest passengerReq : request.getPassengers()) {
                BigDecimal passengerPrice = calculatePassengerBasePrice(segmentBasePrice, passengerReq.getType());
                baseAmount = baseAmount.add(passengerPrice);
            }
        }

        // 2. Baggage amount
        BigDecimal baggageAmount = calculateBaggageAmount(request);

        // 3. Seat type amount
        BigDecimal seatTypeAmount = calculateSeatTypeAmount(request, savedBooking);

        // 4. Ancillary services amount - consider roundtrip multiplier
        int segmentCount = savedBooking.getFlightSegments() != null ? savedBooking.getFlightSegments().size() : 1;
        BigDecimal ancillaryServicesAmount = calculateAncillaryServicesAmount(request.getAncillaryServices(), savedBooking.getPassengers(), segmentCount);

        BigDecimal totalBeforeDiscounts = baseAmount.add(baggageAmount).add(seatTypeAmount).add(ancillaryServicesAmount);
        log.info("Total before discounts: {}", totalBeforeDiscounts);

        BigDecimal finalAmount = totalBeforeDiscounts;

        // 5. Apply Deal Discount from request (if any)
        if (request.getDealCode() != null && !request.getDealCode().trim().isEmpty()) {
            finalAmount = applyDealDiscount(request.getDealCode(), request.getUserId(), savedBooking, finalAmount);
        }

        // 6. Apply Points Redemption Deal (if redeemed)
        if (redeemedDeal != null) {
            finalAmount = applyDealDiscount(redeemedDeal.getDealCode(), request.getUserId(), savedBooking, finalAmount);
        }

        // 7. Apply Tier Discount
        if (request.getUserId() != null) {
            finalAmount = applyTierDiscount(request.getUserId(), finalAmount);
        }

        // Đảm bảo tổng tiền cuối cùng không âm
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        return finalAmount;
    }

    /**
     * Tính giá vé cơ bản cho từng loại hành khách
     * - ADULT: 100% giá cơ bản
     * - CHILD: 75% giá cơ bản
     * - INFANT: 10% giá cơ bản
     */
    private BigDecimal calculatePassengerBasePrice(BigDecimal basePrice, PassengerType passengerType) {
        if (basePrice == null || passengerType == null) {
            return BigDecimal.ZERO;
        }

        return switch (passengerType) {
            case ADULT -> basePrice; // 100%
            case CHILD -> basePrice.multiply(BigDecimal.valueOf(0.75)); // 75%
            case INFANT -> basePrice.multiply(BigDecimal.valueOf(0.10)); // 10%
        };
    }

    private BigDecimal calculateBaggageAmount(BookingRequest request) {
        BigDecimal baggagePerSegment = request.getPassengers().stream()
                .filter(p -> p.getBaggagePackage() != null)
                .map(p -> p.getBaggagePackage().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Nhân với số segment để tính baggage cho roundtrip
        int segmentCount = request.getFlightSegments() != null ? request.getFlightSegments().size() : 1;
        BigDecimal totalBaggageAmount = baggagePerSegment.multiply(BigDecimal.valueOf(segmentCount));
        
        log.info("Baggage calculation: {} per segment x {} segments = {}", 
                baggagePerSegment, segmentCount, totalBaggageAmount);
        
        return totalBaggageAmount;
    }

    private BigDecimal applyDealDiscount(String dealCode, Long userId, Booking booking, BigDecimal currentAmount) {
        try {
            if (dealService.canUserUseDeal(dealCode, userId)) {
                var dealUsage = dealService.applyDeal(dealCode, userId, booking, currentAmount);
                log.info("Applied deal {}. New amount: {}", dealCode, dealUsage.getFinalAmount());
                return dealUsage.getFinalAmount();
            }
        } catch (IllegalArgumentException e) {
            log.warn("Failed to apply deal {}: {}. Continuing with original amount.", dealCode, e.getMessage());
        }
        return currentAmount;
    }

    private BigDecimal applyTierDiscount(Long userId, BigDecimal currentAmount) {
        return userRepository.findById(userId)
                .map(user -> {
                    LoyaltyTier tier = user.getLoyaltyTier();
                    if (tier != null && tier.getDiscountRate() != null && tier.getDiscountRate().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal tierDiscountRate = tier.getDiscountRate();
                        BigDecimal tierDiscountAmount = currentAmount.multiply(tierDiscountRate);
                        BigDecimal newAmount = currentAmount.subtract(tierDiscountAmount);

                        log.info("Applied tier discount for user {}: Tier={}, Rate={}%, Discount={}, New Amount={}",
                                userId, tier.getDisplayName(), tierDiscountRate.multiply(BigDecimal.valueOf(100)),
                                tierDiscountAmount, newAmount);

                        return newAmount;
                    }
                    log.debug("User {} has no applicable tier discount (Tier: {}).", userId, tier);
                    return currentAmount;
                })
                .orElse(currentAmount); // Trả về số tiền hiện tại nếu không tìm thấy user
    }

    private Payment createPaymentForBooking(Booking booking, BigDecimal finalAmount, PaymentMethod paymentMethod) {
        PaymentRequest pr = new PaymentRequest();
        pr.setBookingId(booking.getBookingId());
        pr.setTotalAmount(finalAmount);
        pr.setPaymentMethod(paymentMethod);
        pr.setPaymentStatus(PaymentStatus.PENDING);

        PaymentResponse paymentResponse = paymentService.createPayment(pr);

        return paymentRepository.findById(paymentResponse.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found after creation"));
    }

    private BookingResponse buildBookingResponse(Booking reloadedBooking) {
        BookingResponse response = bookingMapper.toResponseDTO(reloadedBooking);
        populateDealInformation(response, reloadedBooking);
        populateBaggageInformation(response, reloadedBooking);
        populateAncillaryServicesInformation(response, reloadedBooking);
        populateSeatTypeInformation(response, reloadedBooking);
        populateContactInformation(response, reloadedBooking);
        populateAvailableSeatsInformation(response, reloadedBooking);
        return response;
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
            populateContactInformation(response, booking);
            populateAvailableSeatsInformation(response, booking);
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
        populateContactInformation(response, booking);
        populateAvailableSeatsInformation(response, booking);

        return response;
    }

    private void checkAndUpdateBookingStatus(Booking booking) {
        LocalDateTime now = LocalDateTime.now();

        // Nếu booking đang PENDING, kiểm tra timeout
        if (booking.getStatus() == BookingStatus.PENDING) {
            // Chỉ kiểm tra xem đã hết hạn chưa, không thực hiện hủy ở đây.
            // Việc hủy sẽ do tác vụ định kỳ processExpiredPayments() đảm nhiệm.
            if (booking.getPaymentTimeout() != null && now.isAfter(booking.getPaymentTimeout())) {
                if (booking.getPayment() != null) {
                    PaymentStatus paymentStatus = booking.getPayment().getStatus();
                    if (paymentStatus == PaymentStatus.PENDING || paymentStatus == PaymentStatus.FAILED) {
                        log.warn("Booking {} has expired but is being checked. The scheduled task will handle cancellation.", booking.getBookingId());
                    }
                } else {
                    log.warn("Booking {} has expired (no payment initiated). The scheduled task will handle cancellation.", booking.getBookingId());
                }
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
                        populateContactInformation(response, booking);
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

        // Update seat status to OCCUPIED when booking is completed
        for (Passenger passenger : booking.getPassengers()) {
            // Cập nhật ghế chính của passenger (nếu có)
            if (passenger.getSeat() != null && passenger.getSeat().getStatus() == SeatStatus.PENDING_PAYMENT) {
                passenger.getSeat().setStatus(SeatStatus.OCCUPIED);
                seatRepository.save(passenger.getSeat());
                log.debug("Updated passenger's main seat {} status to OCCUPIED for completed booking {}",
                        passenger.getSeat().getSeatNumber(), bookingId);
            }

            // Cập nhật tất cả ghế trong seat assignments
            for (PassengerSeatAssignment assignment : passenger.getSeatAssignments()) {
                Seat seat = assignment.getSeat();
                if (seat.getStatus() == SeatStatus.PENDING_PAYMENT) {
                    seat.setStatus(SeatStatus.OCCUPIED);
                    seatRepository.save(seat);
                    log.debug("Updated seat {} status to OCCUPIED for passenger {} in completed booking {}",
                            seat.getSeatNumber(), passenger.getFirstName(), bookingId);
                }

                // Cũng cập nhật status trong assignment
                if (assignment.getStatus() == SeatStatus.PENDING_PAYMENT) {
                    assignment.setStatus(SeatStatus.OCCUPIED);
                    passengerSeatAssignmentRepository.save(assignment);
                    log.debug("Updated seat assignment status to OCCUPIED for passenger {} seat {} in completed booking {}",
                            passenger.getFirstName(), seat.getSeatNumber(), bookingId);
                }
            }
        }

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

    private void cancelBookingAndReleaseSeats(Booking booking, String reason) {
        log.info("Cancelling booking {} due to: {}", booking.getBookingId(), reason);

        // Hủy booking
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        bookingRepository.save(booking);

        // Giải phóng ghế và cập nhật availableSeats
        int passengerCount = booking.getPassengers().size();

        // Giải phóng ghế thông qua các bản ghi gán ghế (PassengerSeatAssignment)
        for (Passenger passenger : booking.getPassengers()) {
            for (PassengerSeatAssignment assignment : passenger.getSeatAssignments()) {
                Seat seat = assignment.getSeat();
                if (seat != null && seat.getStatus() != SeatStatus.AVAILABLE) {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setBookedByUser(null);
                    seat.setBookedByPassenger(null);
                    seatRepository.save(seat);
                    log.debug("Released seat {} for cancelled booking {}", seat.getSeatNumber(), booking.getBookingId());
                }
                // Xóa bản ghi gán ghế
                passengerSeatAssignmentRepository.delete(assignment);
            }
            passenger.getSeatAssignments().clear();
        }

        // Cập nhật số lượng ghế trống cho tất cả các chặng bay của booking
        if (passengerCount > 0 && booking.getFlightSegments() != null) {
            for (FlightSegment segment : booking.getFlightSegments()) {
                Flight flight = segment.getFlight();
                flight.setAvailableSeats(flight.getAvailableSeats() + passengerCount);
                flightRepository.save(flight);
                log.info("Updated available seats for flight {}: +{}", flight.getFlightNumber(), passengerCount);
            }
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

        // Hoàn điểm thưởng nếu booking đã sử dụng deal đổi điểm
        refundPointsForCancelledBooking(booking);

        // Phát sự kiện BookingCancelledEvent để các listener khác xử lý (gửi email, socket,...)
        eventPublisher.publishEvent(new BookingCancelledEvent(this, booking, reason));

        log.info("Successfully cancelled booking {} and released {} seats", booking.getBookingId(), passengerCount);
    }

    private void updateFlightStatuses(LocalDateTime now) {
        log.info("Updating flight statuses...");

        // Chỉ xem xét các chuyến bay trong khoảng thời gian gần đây để tránh quét toàn bộ DB

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

            // GỬI THÔNG BÁO SOCKET CHO TẤT CẢ HÀNH KHÁCH CỦA CHUYẾN BAY
            List<Booking> affectedBookings = bookingRepository.findByFlightAndStatus(flight, BookingStatus.CONFIRMED);
            for (Booking booking : affectedBookings) {
                if (booking.getUserId() != null) {
                    String message = "Chuyến bay " + flight.getFlightNumber() + " của bạn đã bị trễ. Vui lòng kiểm tra lại thông tin.";
                    String title = "Thông báo chuyến bay bị trễ";
                    notificationService.createAndSendNotification(
                        booking.getUserId().getId(),
                        NotificationType.FLIGHT_DELAYED.toString(),
                        message,
                        flight.getFlightId(),
                        title
                    );
                }
            }
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
        // Find all deal usages for this booking
        List<DealUsage> dealUsages = dealUsageRepository.findAllByBooking(booking);
        log.info("Looking for deal usages for booking {}: found {} usages",
                booking.getBookingId(), dealUsages.size());

        if (!dealUsages.isEmpty()) {
            // Calculate total discount from all deals
            BigDecimal totalDiscountAmount = dealUsages.stream()
                    .map(DealUsage::getDiscountAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // For display, use the first deal's code and percentage (or could be enhanced to show all)
            DealUsage firstUsage = dealUsages.get(0);
            response.setAppliedDealCode(firstUsage.getDeal().getDealCode());
            response.setDiscountPercentage(firstUsage.getDeal().getDiscountPercentage());
            response.setDiscountAmount(totalDiscountAmount);

            log.info("Found {} deal usages for booking {}, total discount: {}",
                    dealUsages.size(), booking.getBookingId(), totalDiscountAmount);
        } else {
            log.warn("No deal usages found for booking {} - deals may not have been applied successfully", booking.getBookingId());
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
        } else {
            log.debug("No checkIns found for booking {}", booking.getBookingId());
        }

        response.setBaggage(baggageList);
        log.info("Set {} baggage items in response", baggageList.size());
    }

    /**
     * Map thông tin từ BookingRequest sang danh sách các thực thể Passenger.
     * Loại bỏ việc sử dụng trường tạm `tempBaggagePackage`.
     */
    private List<Passenger> mapPassengers(BookingRequest request, Booking booking) {
        return request.getPassengers().stream().map(p -> {
            Passenger passenger = new Passenger();
            passenger.setFirstName(p.getFirstName());
            passenger.setLastName(p.getLastName());
            passenger.setDateOfBirth(p.getDateOfBirth());
            passenger.setPassportNumber(p.getPassportNumber());
            passenger.setType(p.getType());
            passenger.setGender(p.getGender());
            passenger.setEmail(p.getEmail());
            passenger.setPhone(p.getPhone());
            passenger.setBooking(booking);

            // LƯU Ý: Không gán seat hoặc baggage package trực tiếp ở đây.
            // Việc này sẽ được xử lý trong các phương thức `createSeatAssignments` và `createInitialCheckinRecords`
            // để đảm bảo luồng dữ liệu rõ ràng và tránh sử dụng các trường tạm.

            return passenger;
        }).collect(Collectors.toList());
    }

    private void updateAvailableSeats(Flight flight, int passengerCount) {
        flight.setAvailableSeats(flight.getAvailableSeats() - passengerCount);
        flightRepository.save(flight);
    }

    /**
     * Tạo các bản ghi gán ghế (PassengerSeatAssignment) cho mỗi hành khách trên mỗi chặng bay.
     * Đồng thời cập nhật trạng thái của ghế thành PENDING_PAYMENT để tránh bị đặt trùng.
     */
    private void createSeatAssignments(BookingRequest request, Booking savedBooking) {
        log.info("Creating seat assignments for {} passengers and {} segments",
                savedBooking.getPassengers().size(), savedBooking.getFlightSegments().size());

        // Validate that each passenger has seat assignments for all segments
        boolean seatsProvided = request.getPassengers().stream()
                .allMatch(p -> p.getSeatAssignments() != null && !p.getSeatAssignments().isEmpty());

        for (int i = 0; i < request.getPassengers().size(); i++) {
            PassengerSeatRequest passengerReq = request.getPassengers().get(i);
            Passenger passenger = savedBooking.getPassengers().get(i);

            // Create seat assignments for this passenger
            if (seatsProvided && passengerReq.getSeatAssignments() != null) {
                if (passengerReq.getSeatAssignments().size() != savedBooking.getFlightSegments().size()) {
                    throw new IllegalArgumentException("Passenger " + passenger.getFirstName() + " " + passenger.getLastName() +
                            " must have exactly " + savedBooking.getFlightSegments().size() + " seat assignments (one for each segment) or none at all.");
                }
                for (PassengerSeatRequest.SeatAssignmentRequest seatReq : passengerReq.getSeatAssignments()) {
                // Find the corresponding flight segment
                FlightSegment segment = savedBooking.getFlightSegments().stream()
                        .filter(s -> s.getSegmentOrder().equals(seatReq.getSegmentOrder()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Flight segment with order " +
                                seatReq.getSegmentOrder() + " not found"));

                // Validate seat
                // SỬ DỤNG KHÓA BI QUAN: Tìm và khóa ghế để ngăn đặt trùng
                Seat seat = seatRepository.findByIdForUpdate(seatReq.getSeatId())
                        .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatReq.getSeatId()));

                // Kiểm tra xem ghế có thực sự khả dụng không.
                // Ngay cả khi có khóa bi quan, một giao dịch khác có thể đã hoàn tất
                // và thay đổi trạng thái ghế trước khi giao dịch này bắt đầu.
                if (seat.getStatus() != SeatStatus.AVAILABLE) {
                    String errorMessage = String.format("Seat %s is not available. Current status: %s. Possibly booked by another user.",
                                                          seat.getSeatNumber(), seat.getStatus());
                    throw new IllegalStateException(errorMessage);
                }

                // Create seat assignment
                PassengerSeatAssignment assignment = PassengerSeatAssignment.builder()
                        .passenger(passenger)
                        .flightSegment(segment)
                        .seat(seat)
                        .status(SeatStatus.PENDING_PAYMENT)
                        .build();

                // Update seat status to PENDING_PAYMENT to prevent double booking
                seat.setStatus(SeatStatus.PENDING_PAYMENT);
                // Set bookedBy to the user who made the booking (if authenticated) or passenger
                if (savedBooking.getUserId() != null) {
                    seat.setBookedByUser(savedBooking.getUserId());
                } else {
                    seat.setBookedByPassenger(passenger);
                }

                // Set seat type for this assignment (from seat assignment or fallback to passenger level)
                SeatTypes assignmentSeatType = seatReq.getSeatType() != null ?
                        seatReq.getSeatType() : passengerReq.getSeatType();
                if (assignmentSeatType != null) {
                    seat.setType(assignmentSeatType);
                }

                seatRepository.save(seat);

                passengerSeatAssignmentRepository.save(assignment);
                passenger.getSeatAssignments().add(assignment);

                log.debug("Created seat assignment: passenger={}, segment={}, seat={}",
                        passenger.getFirstName(), segment.getSegmentOrder(), seat.getSeatNumber());
                }
            }
        }
        if (seatsProvided) {
            log.info("Successfully created all seat assignments as provided in the request.");
        } else {
            log.info("No seats were selected during booking. Seats can be assigned later during check-in.");
        }
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

    /**
     * Calculate ancillary services amount with consideration for roundtrip flights
     * For roundtrip bookings, some services should be counted per segment
     */
    private BigDecimal calculateAncillaryServicesAmount(List<BookingAncillaryServiceRequest> ancillaryServiceRequests,
                                                        List<Passenger> passengers, int segmentCount) {
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

            // Calculate base amount for this service
            BigDecimal serviceBaseAmount = ancillaryService.getPrice().multiply(BigDecimal.valueOf(serviceRequest.getQuantity()));

            // Initialize service total amount
            BigDecimal serviceTotalAmount = serviceBaseAmount;

            // Multiply by passenger count if service is per-passenger
            if (ancillaryService.getIsPerPassenger() && serviceRequest.getPassengerId() != null) {
                // Per-passenger service: already handled in individual requests
                serviceTotalAmount = serviceBaseAmount;
                log.info("Per-passenger ancillary service: {} - Total: {}",
                        ancillaryService.getServiceName(), serviceTotalAmount);
            } else if (ancillaryService.getIsPerPassenger() && serviceRequest.getPassengerId() == null) {
                // Per-passenger service selected for booking: multiply by passenger count
                serviceTotalAmount = serviceBaseAmount.multiply(BigDecimal.valueOf(passengers.size()));
                log.info("Per-passenger ancillary service for all passengers: {} - Base: {} x {} passengers = {}",
                        ancillaryService.getServiceName(), serviceBaseAmount, passengers.size(), serviceTotalAmount);
            } else {
                // Per-booking service: no multiplication
                serviceTotalAmount = serviceBaseAmount;
                log.info("Per-booking ancillary service: {} - Total: {}",
                        ancillaryService.getServiceName(), serviceTotalAmount);
            }

            // Multiply by segment count if service is per-segment (for roundtrip flights)
            if (ancillaryService.getIsPerSegment() != null && ancillaryService.getIsPerSegment()) {
                serviceTotalAmount = serviceTotalAmount.multiply(BigDecimal.valueOf(segmentCount));
                log.info("Per-segment ancillary service: {} - Multiplied by {} segments = {}",
                        ancillaryService.getServiceName(), segmentCount, serviceTotalAmount);
            }

            totalAmount = totalAmount.add(serviceTotalAmount);
        }

        return totalAmount;
    }

    /**
     * Determine if an ancillary service should be charged per segment (roundtrip x2)
     * or per booking (roundtrip x1)
     */
    private boolean isPerSegmentAncillaryService(AncillaryService ancillaryService) {
        // Services that should be charged per segment (need for each flight)
        return switch (ancillaryService.getServiceType()) {
            case MEAL -> true;              // Meals needed for each flight
            case SEAT -> true;              // Seat selection for each flight  
            case PRIORITY_BOARDING -> true; // Priority boarding for each flight
            case WIFI -> true;              // WiFi for each flight
            case EXTRA_LEGROOM -> true;     // Extra legroom for each flight
            case INFANT_MEAL -> true;       // Infant meals for each flight
            // Services that are per booking (once for entire journey)
            case TRAVEL_INSURANCE -> false; // Insurance covers entire trip
            case LOUNGE_ACCESS -> false;    // Usually covers entire journey
            case ENTERTAINMENT -> false;    // Usually per journey
            case PET_TRANSPORT -> false;    // Once per journey
            case SPECIAL_ASSISTANCE -> false; // Usually per journey
            case OTHER -> false;            // Default to per booking
        };
    }
    
    private void createBookingAncillaryServices(BookingRequest request, Booking savedBooking) {
        if (request.getAncillaryServices() == null || request.getAncillaryServices().isEmpty()) {
            return;
        }

        // Create a map from passengerId (index in passengers array) to passenger object
        Map<Long, Passenger> passengerMap = new HashMap<>();
        for (int i = 0; i < savedBooking.getPassengers().size(); i++) {
            passengerMap.put((long) i, savedBooking.getPassengers().get(i));
        }

        for (BookingAncillaryServiceRequest serviceRequest : request.getAncillaryServices()) {
            AncillaryService ancillaryService = ancillaryServiceRepository.findById(serviceRequest.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại với ID: " + serviceRequest.getServiceId()));

            // Determine if this is a per-passenger service
            boolean isPerPassengerService = ancillaryService.getIsPerPassenger() != null ? ancillaryService.getIsPerPassenger() : true;

            if (isPerPassengerService) {
                // Create service records for each passenger
                createPerPassengerAncillaryServices(serviceRequest, ancillaryService, savedBooking, passengerMap);
            } else {
                // Create single service record for entire booking (but adjust quantity based on passenger count if needed)
                createPerBookingAncillaryService(serviceRequest, ancillaryService, savedBooking, passengerMap);
            }
        }
    }

    private void createPerPassengerAncillaryServices(BookingAncillaryServiceRequest serviceRequest, 
                                                   AncillaryService ancillaryService, 
                                                   Booking savedBooking, 
                                                   Map<Long, Passenger> passengerMap) {
        
        if (serviceRequest.getPassengerId() != null) {
            // Service for specific passenger
            Passenger passenger = passengerMap.get(serviceRequest.getPassengerId());
            if (passenger == null) {
                throw new ResourceNotFoundException("Hành khách không tồn tại với passengerId: " + serviceRequest.getPassengerId());
            }
            createSingleAncillaryServiceRecord(serviceRequest, ancillaryService, savedBooking, passenger);
        } else {
            // Service for all passengers
            for (Passenger passenger : savedBooking.getPassengers()) {
                createSingleAncillaryServiceRecord(serviceRequest, ancillaryService, savedBooking, passenger);
            }
        }
    }

    private void createPerBookingAncillaryService(BookingAncillaryServiceRequest serviceRequest,
                                                AncillaryService ancillaryService,
                                                Booking savedBooking,
                                                Map<Long, Passenger> passengerMap) {

        Passenger passenger = null;
        if (serviceRequest.getPassengerId() != null) {
            passenger = passengerMap.get(serviceRequest.getPassengerId());
            if (passenger == null) {
                throw new ResourceNotFoundException("Hành khách không tồn tại với passengerId: " + serviceRequest.getPassengerId());
            }
        }

        // Simplified logic: only multiply by passenger count if isPerPassenger is true
        int effectiveQuantity = serviceRequest.getQuantity();

        // If service is per-passenger and selected for whole booking, multiply by passenger count
        if (serviceRequest.getPassengerId() == null && ancillaryService.getIsPerPassenger()) {
            effectiveQuantity = serviceRequest.getQuantity() * savedBooking.getPassengers().size();
            log.info("Per-passenger service selected for whole booking: {} - Quantity: {} x {} passengers = {}",
                    ancillaryService.getServiceName(), serviceRequest.getQuantity(),
                    savedBooking.getPassengers().size(), effectiveQuantity);
        } else {
            // Per-booking service or per-passenger service for specific passenger: keep original quantity
            log.info("Per-booking service or specific passenger service: {} - Quantity: {}",
                    ancillaryService.getServiceName(), effectiveQuantity);
        }

        createSingleAncillaryServiceRecord(serviceRequest, ancillaryService, savedBooking, passenger, effectiveQuantity);
    }

    private void createSingleAncillaryServiceRecord(BookingAncillaryServiceRequest serviceRequest, 
                                                  AncillaryService ancillaryService, 
                                                  Booking savedBooking, 
                                                  Passenger passenger) {
        createSingleAncillaryServiceRecord(serviceRequest, ancillaryService, savedBooking, passenger, serviceRequest.getQuantity());
    }

    private void createSingleAncillaryServiceRecord(BookingAncillaryServiceRequest serviceRequest, 
                                                  AncillaryService ancillaryService, 
                                                  Booking savedBooking, 
                                                  Passenger passenger, 
                                                  int quantity) {
        
        BookingAncillaryService bookingAncillaryService = new BookingAncillaryService();
        bookingAncillaryService.setBooking(savedBooking);
        bookingAncillaryService.setAncillaryService(ancillaryService);
        bookingAncillaryService.setPassenger(passenger);
        bookingAncillaryService.setQuantity(quantity);
        bookingAncillaryService.setUnitPrice(ancillaryService.getPrice());
        bookingAncillaryService.setTotalPrice(ancillaryService.getPrice().multiply(BigDecimal.valueOf(quantity)));
        bookingAncillaryService.setNotes(serviceRequest.getNotes());

        bookingAncillaryServiceRepository.save(bookingAncillaryService);

        log.info("Created booking ancillary service: {} for booking: {} (passenger: {}, quantity: {})",
                ancillaryService.getServiceName(), savedBooking.getBookingId(), 
                passenger != null ? passenger.getPassengerId() : "all", quantity);
    }

    /**
     * Determine if a per-booking service should have its quantity multiplied by passenger count
     */
    private void populateAncillaryServicesInformation(BookingResponse response, Booking booking) {
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

    /**
     * Populate baggage information chỉ cho một hành khách cụ thể
     */
    /**
     * Populate baggage information cho toàn bộ booking (không phải per passenger)
     */
    private void populateBaggageInformationForBooking(BookingResponse response, Booking booking) {
        List<BaggageResponse> baggageList = new ArrayList<>();

        // Check if checkIns are loaded, if not fetch them for baggage population
        List<CheckIn> checkIns = booking.getCheckIns();
        if (checkIns == null || !org.hibernate.Hibernate.isInitialized(checkIns)) {
            log.debug("CheckIns not loaded for booking {}, fetching for baggage population", booking.getBookingId());
            // Fetch checkIns with baggage information
            checkIns = checkinRepository.findByBookingIdWithBaggage(booking.getBookingId());
        }

        if (checkIns != null && !checkIns.isEmpty()) {
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

                    // Thêm thông tin passenger để biết baggage này của ai
                    if (checkIn.getPassenger() != null) {
                        baggageResponse.setPassengerId(checkIn.getPassenger().getPassengerId());
                        baggageResponse.setPassengerName(checkIn.getPassenger().getFirstName() + " " + checkIn.getPassenger().getLastName());
                    }

                    baggageList.add(baggageResponse);
                    log.debug("Added baggage {} for passenger {}", baggage.getBaggageId(),
                        checkIn.getPassenger() != null ? checkIn.getPassenger().getFirstName() : "Unknown");
                }
            }
        }

        response.setBaggage(baggageList);
        log.info("Populated {} baggage items for booking {}", baggageList.size(), booking.getBookingId());
    }

    /**
     * Populate ancillary services information cho toàn bộ booking
     */
    private void populateAncillaryServicesInformationForBooking(BookingResponse response, Booking booking) {
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

        BigDecimal ancillaryServicesAmount = ancillaryServiceResponses.stream()
                .map(BookingAncillaryServiceResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setAncillaryServices(ancillaryServiceResponses);
        response.setAncillaryServicesAmount(ancillaryServicesAmount);

        log.info("Populated {} ancillary services for booking {} with total amount: {}",
                ancillaryServiceResponses.size(), booking.getBookingId(), ancillaryServicesAmount);
    }

    /**
     * Tạo các chặng bay (FlightSegment) cho booking dựa trên request.
     * Giá của mỗi chặng được lấy từ database (FlightTravelClass) để đảm bảo tính nhất quán.
     */
    private List<FlightSegment> createFlightSegments(BookingRequest request, Booking booking) {
        return request.getFlightSegments().stream()
                .map(segmentRequest -> {
                    FlightSegment segment = new FlightSegment();
                    segment.setBooking(booking);
                    // Set segment order from request to maintain sequence
                    segment.setSegmentOrder(segmentRequest.getSegmentOrder());
                    Flight flight = flightRepository.findById(segmentRequest.getFlightId())
                            .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));

                    segment.setFlight(flight);
//                    segment.setTravelClass(travelClassRepository.findById(segmentRequest.getClassId())
//                            .orElseThrow(() -> new ResourceNotFoundException("Travel class not found")));

                    FlightTravelClass flightTravelClass = flightTravelClassRepository.findById(segmentRequest.getClassId())
                            .orElseThrow(() -> new ResourceNotFoundException("FlightTravelClass not found"));
                    segment.setTravelClass(flightTravelClass.getTravelClass());

                    // Set airport information from the flight
                    segment.setDepartureAirport(flight.getDepartureAirport());
                    segment.setArrivalAirport(flight.getArrivalAirport());

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

    @Deprecated
    private void accumulateLoyaltyPoints(User user, BigDecimal amount) {
        // DEPRECATED: This method should not be used for booking creation
        // Loyalty points should only be awarded when booking is completed (CONFIRMED status)
        // Use awardLoyaltyPointsForCompletedBooking() instead
        log.warn("DEPRECATED METHOD CALLED: accumulateLoyaltyPoints() - should use awardLoyaltyPointsForCompletedBooking()");
    }


    /**
     * Tạo bản ghi CheckIn cho một segment cụ thể của hành khách
     * Mỗi segment sẽ có check-in record riêng biệt
     */
    private void createSegmentCheckIn(Booking booking, Passenger passenger, FlightSegment segment, BaggagePackage baggagePackage) {
        Baggage baggage = null;

        // Chỉ tạo baggage cho segment đầu tiên để tránh duplicate
        if (segment.getSegmentOrder() == 1 && baggagePackage != null) {
            baggage = Baggage.builder()
                    .type(BaggageType.CHECK_IN)
                    .purchasedPackage(baggagePackage)
                    .packagePrice(baggagePackage.getPrice())
                    .build();
            baggage = baggageRepository.save(baggage);
        }

        // Get seat number for this passenger and segment
        String seatNumber = getSeatNumberForPassengerAndSegment(passenger, segment);

        CheckIn checkIn = CheckIn.builder()
                .booking(booking)
                .passenger(passenger)
                .flightSegment(segment)
                .baggage(baggage)
                .seatNumber(seatNumber)
                .checkedAt(segment.getFlight().getDepartureTime())
                .status(CheckinStatus.PENDING)
                .build();

        checkIn = checkinRepository.save(checkIn);
        log.info("CheckIn created for segment {} with ID: {} for passenger {}",
                segment.getSegmentOrder(), checkIn.getCheckInId(), passenger.getFirstName());

        if (baggage != null) {
            baggage.setCheckIn(checkIn);
            baggageRepository.save(baggage);
        }
    }

    /**
     * Tạo bản ghi CheckIn ban đầu cho một hành khách.
     * Nếu có gói hành lý được chọn, tạo luôn bản ghi Baggage tương ứng.
     * @deprecated Use createSegmentCheckIn for roundtrip support
     */
    @Deprecated
    private void createCheckIn(Booking booking, Passenger passenger, BaggagePackage baggagePackage) {
        Baggage baggage = null;

        if (baggagePackage != null) {
            log.info("Creating baggage for passenger {} with package {}",
                    passenger.getFirstName(), baggagePackage);
            baggage = Baggage.builder()
                    .type(BaggageType.CHECK_IN)
                    .purchasedPackage(baggagePackage)
                    .packagePrice(baggagePackage.getPrice())
                    .build();
            baggage = baggageRepository.save(baggage);
            log.info("Baggage created with ID: {}", baggage.getBaggageId());
        } else {
            log.info("No baggage package selected for passenger {}", passenger.getFirstName());
        }

        String seatNumber = null;
        // Lấy seat từ segment đầu tiên (segmentOrder = 1) cho check-in
        Seat primarySeat = passenger.getPrimarySeat();
        if (primarySeat != null) {
            seatNumber = primarySeat.getSeatNumber();
        }

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

    /**
     * Tạo các bản ghi check-in ban đầu cho tất cả hành khách trong booking.
     * For roundtrip: creates separate check-in records for each segment
     */
    private void createInitialCheckinRecords(Booking booking, List<Passenger> passengers, BookingRequest request) {
        log.info("Creating initial check-in records for {} passengers and {} segments",
                passengers.size(), booking.getFlightSegments().size());

        // For each passenger, create check-in records for each segment
        for (int i = 0; i < passengers.size(); i++) {
            Passenger passenger = passengers.get(i);
            PassengerSeatRequest passengerRequest = request.getPassengers().get(i);
            BaggagePackage baggagePackage = passengerRequest.getBaggagePackage();

            // Create check-in records for each flight segment
            for (FlightSegment segment : booking.getFlightSegments()) {
                createSegmentCheckIn(booking, passenger, segment, baggagePackage);
            }
        }
    }
    private void awardLoyaltyPointsForCompletedBooking(Booking booking) {
        User user = booking.getUserId();
        if (user == null) {
            log.debug("Skipping loyalty points for guest booking: {}", booking.getBookingId());
            return;
        }

        BigDecimal bookingAmount = booking.getTotalAmount();

        // Base points: 1 point per 1000 VND (cải thiện từ rate quá thấp)
        int basePoints = bookingAmount.divide(BigDecimal.valueOf(1000)).intValue();

        // Tier multiplier bonus
        LoyaltyTier userTier = user.getLoyaltyTier() != null ? user.getLoyaltyTier() : LoyaltyTier.STANDARD;
        double tierMultiplier = getTierPointsMultiplier(userTier);

        // Booking type bonus (domestic vs international)
        int bookingBonus = calculateBookingTypeBonus(booking);

        // Total points calculation
        int totalPoints = (int) Math.round(basePoints * tierMultiplier) + bookingBonus;

        if (totalPoints > 0) {
            Integer currentPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
            user.setLoyaltyPoints(currentPoints + totalPoints);
            userRepository.save(user);

            log.info("Awarded {} loyalty points ({} base * {}x tier + {} bonus) for completed booking {} to {} ({})",
                    totalPoints, basePoints, tierMultiplier, bookingBonus, booking.getBookingId(),
                    user.getEmail(), userTier);

            // Check for tier upgrade after awarding points
            try {
                loyaltyService.checkAndUpgradeTier(user.getId());
            } catch (Exception e) {
                log.error("Failed to check tier upgrade for user {} after completing booking {}: {}",
                        user.getId(), booking.getBookingId(), e.getMessage());
            }
        }
    }

    /**
     * Tính multiplier cho tier
     */
    private double getTierPointsMultiplier(LoyaltyTier tier) {
        switch (tier) {
            case STANDARD: return 1.0;
            case SILVER: return 1.2;    // +20% bonus
            case GOLD: return 1.5;      // +50% bonus
            case PLATINUM: return 2.0;  // +100% bonus
            default: return 1.0;
        }
    }

    /**
     * Tính bonus theo loại booking
     */
    private int calculateBookingTypeBonus(Booking booking) {
        // Bonus cho international flights
        boolean isInternational = !booking.getFlight().getDepartureAirport().getCountry().getCountryCode()
                .equals(booking.getFlight().getArrivalAirport().getCountry().getCountryCode());

        if (isInternational) {
            return 50; // Bonus 50 points cho international
        }

        return 10; // Bonus 10 points cho domestic
    }

    @Override
    public Optional<BookingResponse> findByBookingCodeAndPassengerName(String bookingCode, String fullName) {
        log.info("Finding booking by booking code: {} and passenger name: {}", bookingCode, fullName);

        Optional<Booking> bookingOpt;

        // Nếu không có fullName, tìm theo bookingCode và trả về tất cả thông tin booking
        if (fullName == null || fullName.trim().isEmpty()) {
            log.info("No passenger name provided, finding booking by booking code only: {}", bookingCode);
            bookingOpt = bookingRepository.findByBookingCode(bookingCode);
        } else {
            // Nếu có fullName, tìm theo cả bookingCode và fullName để verify passenger
            bookingOpt = bookingRepository.findByBookingCodeAndPassengerFullName(bookingCode, fullName);
        }

        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            try {
                BookingResponse response = bookingMapper.toResponseDTO(booking);

                // Populate basic booking information that should always be available
                if (booking.getFlight() != null) {
                    response.setFlightNumber(booking.getFlight().getFlightNumber());
                }
                if (booking.getTravelClass() != null) {
                    response.setTravelClass(booking.getTravelClass().getClassName());
                }

                // Populate flight segments information
                if (booking.getFlightSegments() != null && !booking.getFlightSegments().isEmpty()) {
                    List<FlightSegmentResponse> segmentResponses = booking.getFlightSegments().stream()
                        .map(segment -> {
                            FlightSegmentResponse segResp = new FlightSegmentResponse();
                            segResp.setSegmentId(segment.getSegmentId());
                            segResp.setSegmentOrder(segment.getSegmentOrder());
                            segResp.setPrice(segment.getPrice());

                            if (segment.getFlight() != null) {
                                segResp.setFlightId(segment.getFlight().getFlightId());
                                segResp.setFlightNumber(segment.getFlight().getFlightNumber());
                                segResp.setDepartureTime(segment.getFlight().getDepartureTime());
                                segResp.setArrivalTime(segment.getFlight().getArrivalTime());
                                segResp.setDuration(segment.getFlight().getDuration() != null ?
                                    segment.getFlight().getDuration().toString() : null);

                                // Aircraft information
                                if (segment.getFlight().getAircraft() != null) {
                                    segResp.setAircraft(segment.getFlight().getAircraft().getAircraftName());
                                }

                                // Airport information
                                if (segment.getFlight().getDepartureAirport() != null) {
                                    segResp.setDepartureAirport(airportMapper.toResponseDTOWithoutGates(segment.getFlight().getDepartureAirport()));
                                }
                                if (segment.getFlight().getArrivalAirport() != null) {
                                    segResp.setArrivalAirport(airportMapper.toResponseDTOWithoutGates(segment.getFlight().getArrivalAirport()));
                                }
                            }

                            // Travel class information
                            if (segment.getTravelClass() != null) {
                                segResp.setClassId(segment.getTravelClass().getId());
                                segResp.setClassName(segment.getTravelClass().getClassName());
                            }
                            return segResp;
                        })
                        .collect(Collectors.toList());
                    response.setFlightSegments(segmentResponses);
                }

                // Populate thông tin passengers
                // Nếu không có fullName: lấy tất cả passengers trong booking
                // Nếu có fullName: chỉ lấy passengers khớp tên
                List<Passenger> passengersToProcess;
                if (fullName == null || fullName.trim().isEmpty()) {
                    passengersToProcess = booking.getPassengers();
                    log.info("No passenger name filter, processing all {} passengers in booking", passengersToProcess.size());
                } else {
                    passengersToProcess = booking.getPassengers().stream()
                        .filter(p -> (p.getFirstName() + " " + p.getLastName()).equalsIgnoreCase(fullName))
                        .collect(Collectors.toList());
                    log.info("Filtered to {} passengers matching name: {}", passengersToProcess.size(), fullName);
                }

                if (!passengersToProcess.isEmpty()) {
                    // Populate thông tin cho tất cả passengers được chọn
                    List<PassengerSeatResponse> filteredPassengers = new ArrayList<>();
                    for (Passenger passenger : passengersToProcess) {
                        PassengerSeatResponse psr = new PassengerSeatResponse();
                        psr.setPassengerId(passenger.getPassengerId());
                        psr.setFirstName(passenger.getFirstName());
                        psr.setLastName(passenger.getLastName());
                        psr.setType(passenger.getType());
                        psr.setPassportNumber(passenger.getPassportNumber());
                        psr.setEmail(passenger.getEmail());
                        psr.setPhone(passenger.getPhone());
                        psr.setGender(passenger.getGender());
                        psr.setDateOfBirth(passenger.getDateOfBirth());

                        // Populate travel class name
                        if (booking.getTravelClass() != null) {
                            psr.setClassName(booking.getTravelClass().getClassName());
                        }

                        // Populate seat assignments
                        if (passenger.getSeatAssignments() != null && !passenger.getSeatAssignments().isEmpty()) {
                            List<PassengerSeatAssignmentResponse> seatAssignmentResponses = passenger.getSeatAssignments().stream()
                                .map(assignment -> {
                                    PassengerSeatAssignmentResponse psaResp = new PassengerSeatAssignmentResponse();
                                    psaResp.setAssignmentId(assignment.getAssignmentId());
                                    if (assignment.getFlightSegment() != null) {
                                        psaResp.setSegmentOrder(assignment.getFlightSegment().getSegmentOrder());
                                        if (assignment.getFlightSegment().getFlight() != null) {
                                            psaResp.setFlightNumber(assignment.getFlightSegment().getFlight().getFlightNumber());
                                            if (assignment.getFlightSegment().getFlight().getDepartureAirport() != null) {
                                                psaResp.setDepartureAirport(assignment.getFlightSegment().getFlight().getDepartureAirport().getAirportCode());
                                            }
                                            if (assignment.getFlightSegment().getFlight().getArrivalAirport() != null) {
                                                psaResp.setArrivalAirport(assignment.getFlightSegment().getFlight().getArrivalAirport().getAirportCode());
                                            }
                                        }
                                    }
                                    if (assignment.getSeat() != null) {
                                        psaResp.setSeatNumber(assignment.getSeat().getSeatNumber());
                                        psaResp.setSeatType(assignment.getSeat().getType().toString());
                                    }
                                    psaResp.setStatus(assignment.getStatus());
                                    return psaResp;
                                })
                                .collect(Collectors.toList());
                            psr.setSeatAssignments(seatAssignmentResponses);
                        }
                        filteredPassengers.add(psr);
                    }
                    response.setPassengers(filteredPassengers);

                    // Populate baggage và ancillary services cho toàn bộ booking
                    try {
                        populateBaggageInformationForBooking(response, booking);
                    } catch (Exception e) {
                        log.warn("Could not populate baggage information for booking {}: {}", booking.getBookingId(), e.getMessage());
                    }
                    populateAncillaryServicesInformationForBooking(response, booking);

                    populateSeatTypeInformation(response, booking);
                    // Use new roundtrip-aware check-in logic
                    // Nếu tìm theo bookingCode only: trả về tất cả checkinEligiblePassengers
                    // Nếu tìm theo fullName: filter theo tên
                    if (fullName == null || fullName.trim().isEmpty()) {
                        response.setCheckinEligiblePassengers(getAllCheckinEligiblePassengers(bookingCode));
                    } else {
                        response.setCheckinEligiblePassengers(getCheckinEligiblePassengers(bookingCode, fullName));
                    }

                    // Populate available seats using new segment-aware logic
                    populateAvailableSeatsInformation(response, booking);
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

                // Populate tất cả passengers của booking
                List<Passenger> allPassengers = booking.getPassengers();
                if (allPassengers != null && !allPassengers.isEmpty()) {
                    List<PassengerSeatResponse> allPassengerResponses = new ArrayList<>();
                    for (Passenger passenger : allPassengers) {
                        PassengerSeatResponse psr = new PassengerSeatResponse();
                        psr.setPassengerId(passenger.getPassengerId());
                        psr.setFirstName(passenger.getFirstName());
                        psr.setLastName(passenger.getLastName());
                        psr.setType(passenger.getType());
                        allPassengerResponses.add(psr);
                    }
                    response.setPassengers(allPassengerResponses);

                    // Populate thông tin tổng thể của booking
                    populateDealInformation(response, booking);
                    try {
                        populateBaggageInformationForBooking(response, booking);
                    } catch (Exception ex) {
                        log.warn("Could not populate baggage information for booking lookup: {}", ex.getMessage());
                    }
                    populateAncillaryServicesInformationForBooking(response, booking);
                    populateSeatTypeInformation(response, booking);
                    response.setCheckinEligiblePassengers(getCheckinEligiblePassengers(bookingCode, fullName));
                    populateAvailableSeatsInformation(response, booking);
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
                // Cập nhật ghế chính của passenger (nếu có)
                if (passenger.getSeat() != null) {
                    passenger.getSeat().setStatus(SeatStatus.OCCUPIED);
                    seatRepository.save(passenger.getSeat());
                    log.debug("Updated passenger's main seat {} status to OCCUPIED for passenger {}",
                            passenger.getSeat().getSeatNumber(), passenger.getFirstName());
                }

                // Cập nhật tất cả ghế trong seat assignments
                for (PassengerSeatAssignment assignment : passenger.getSeatAssignments()) {
                    Seat seat = assignment.getSeat();
                    if (seat.getStatus() == SeatStatus.PENDING_PAYMENT) {
                        seat.setStatus(SeatStatus.OCCUPIED);
                        seatRepository.save(seat);
                        log.debug("Updated seat {} status to OCCUPIED for passenger {} via assignment",
                                seat.getSeatNumber(), passenger.getFirstName());
                    }

                    // Cũng cập nhật status trong assignment
                    if (assignment.getStatus() == SeatStatus.PENDING_PAYMENT) {
                        assignment.setStatus(SeatStatus.OCCUPIED);
                        passengerSeatAssignmentRepository.save(assignment);
                        log.debug("Updated seat assignment status to OCCUPIED for passenger {} seat {}",
                                passenger.getFirstName(), seat.getSeatNumber());
                    }
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

            // Tính seat type amount cho từng seat assignment
            for (PassengerSeatAssignment assignment : passenger.getSeatAssignments()) {
                Seat seat = assignment.getSeat();
                if (seat != null && seat.getType() != null) {
                    try {
                        // Lấy giá seat type từ enum
                        SeatTypePrice seatTypePrice = SeatTypePrice.fromSeatType(seat.getType());
                        BigDecimal additionalPrice = seatTypePrice.getAdditionalPrice();

                        totalSeatTypeAmount = totalSeatTypeAmount.add(additionalPrice);

                        log.debug("Applied seat type {} with additional price {} for passenger {} (segment {})",
                                seat.getType(), additionalPrice, passenger.getFirstName(), assignment.getFlightSegment().getSegmentOrder());
                    } catch (Exception e) {
                        log.error("Error calculating seat type amount for passenger {}: {}",
                                passenger.getFirstName(), e.getMessage());
                    }
                }
            }
        }

        return totalSeatTypeAmount;
    }

    private void populateSeatTypeInformation(BookingResponse response, Booking booking) {
        BigDecimal totalSeatTypeAmount = BigDecimal.ZERO;
        List<SeatTypePricingDetail> seatTypeDetails = new ArrayList<>();

        for (Passenger passenger : booking.getPassengers()) {
            // Iterate qua tất cả seat assignments của passenger
            for (PassengerSeatAssignment assignment : passenger.getSeatAssignments()) {
                Seat seat = assignment.getSeat();
                FlightSegment segment = assignment.getFlightSegment();
                
                if (seat != null && seat.getType() != null && segment != null) {
                    SeatTypePrice seatTypePrice = SeatTypePrice.fromSeatType(seat.getType());
                    BigDecimal additionalPrice = seatTypePrice.getAdditionalPrice();

                    totalSeatTypeAmount = totalSeatTypeAmount.add(additionalPrice);

                    seatTypeDetails.add(new SeatTypePricingDetail(
                        passenger.getFirstName() + " " + passenger.getLastName(),
                        seat.getSeatNumber(),
                        seat.getType(),
                        additionalPrice,
                        segment.getSegmentId(),
                        segment.getSegmentOrder(),
                        segment.getFlight().getFlightNumber()
                    ));
                }
            }
        }

        response.setSeatTypeAmount(totalSeatTypeAmount);
        response.setSeatTypeDetails(seatTypeDetails);

        log.debug("Populated seat type information: {} details, total amount: {}",
                seatTypeDetails.size(), totalSeatTypeAmount);
    }

    /**
     * Scheduled job để xử lý các booking đã hết thời hạn thanh toán
     * Chạy mỗi phút để check và cancel expired bookings
     */
    @Scheduled(fixedRate = 60000) // Chạy mỗi phút
    @Transactional
    public void processExpiredPayments() {
        log.debug("Checking for expired payments...");

        LocalDateTime now = LocalDateTime.now();
        // Chỉ kiểm tra các booking được tạo trong vòng 24 giờ qua để tránh xử lý lại dữ liệu cũ
        LocalDateTime lookbackTime = now.minusHours(24);

        List<Booking> expiredBookings = bookingRepository.findRecentExpiredBookings(
                now, BookingStatus.PENDING, lookbackTime);

        if (expiredBookings.isEmpty()) {
            log.debug("No expired bookings found");
            return;
        }

        log.info("Found {} expired bookings to process", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                // Kiểm tra lại trạng thái thanh toán trước khi hủy
                if (booking.getPayment() != null && booking.getPayment().getStatus() == PaymentStatus.COMPLETED) {
                    log.warn("Booking {} has a completed payment but was marked as expired. Skipping cancellation.", booking.getBookingId());
                    continue;
                }
                log.info("Cancelling expired booking: {} (timeout: {})",
                        booking.getBookingCode(), booking.getPaymentTimeout());
                cancelBookingAndReleaseSeats(booking, "Hết hạn thanh toán");
                log.info("Successfully cancelled expired booking: {}", booking.getBookingCode());
            } catch (Exception e) {
                log.error("Failed to cancel expired booking {}: {}", booking.getBookingId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Scheduled job để gửi thông báo check-in
     * Chạy mỗi 30 phút để kiểm tra và gửi thông báo check-in
     */
    @Scheduled(fixedRate = 1800000) // 30 phút
    @Transactional
    public void sendCheckinNotifications() {
        log.debug("Checking for check-in notifications to send...");

        LocalDateTime now = LocalDateTime.now();

        try {
            // 1. Gửi thông báo check-in đã mở (24 giờ trước bay)
            sendCheckinOpenNotifications(now);

            // 2. Gửi thông báo nhắc nhở check-in (3 giờ trước bay)
            sendCheckinReminderNotifications(now);

            log.debug("Check-in notifications sent successfully");
        } catch (Exception e) {
            log.error("Error sending check-in notifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Gửi thông báo khi check-in đã mở (24 giờ trước bay)
     */
    private void sendCheckinOpenNotifications(LocalDateTime now) {
        try {
            // Tìm các chuyến bay có check-in mở trong 24 giờ tới
            LocalDateTime checkinOpensAt = now.plusHours(24);
            LocalDateTime checkinOpensWindow = now.plusHours(24).plusMinutes(30); // Cửa sổ 30 phút

            List<Booking> bookingsToNotify = bookingRepository.findBookingsForCheckinNotifications(
                checkinOpensAt, checkinOpensWindow, BookingStatus.CONFIRMED, PaymentStatus.COMPLETED);

            log.info("Found {} bookings for check-in open notifications", bookingsToNotify.size());

            for (Booking booking : bookingsToNotify) {
                try {
                    if (booking.getUserId() != null) {
                        String flightNumber = booking.getFlight().getFlightNumber();
                        String departureTime = booking.getFlight().getDepartureTime().toString();
                        String message = String.format(
                            "Check-in cho chuyến bay %s đã mở. Thời gian bay: %s. Bạn có thể check-in online ngay bây giờ.",
                            flightNumber, departureTime);
                        String title = "Check-in đã mở";

                        notificationService.createAndSendNotification(
                            booking.getUserId().getId(),
                            NotificationType.CHECKIN_OPEN.toString(),
                            message,
                            booking.getBookingId(),
                            title
                        );

                        log.debug("Sent check-in open notification for booking {}", booking.getBookingCode());
                    }
                } catch (Exception e) {
                    log.error("Failed to send check-in open notification for booking {}: {}",
                             booking.getBookingId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in sendCheckinOpenNotifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Gửi thông báo nhắc nhở check-in (3 giờ trước bay)
     */
    private void sendCheckinReminderNotifications(LocalDateTime now) {
        try {
            // Tìm các chuyến bay cần nhắc nhở check-in (3 giờ trước bay)
            LocalDateTime reminderTime = now.plusHours(3);
            LocalDateTime reminderWindow = now.plusHours(3).plusMinutes(30); // Cửa sổ 30 phút

            List<Booking> bookingsToRemind = bookingRepository.findBookingsForCheckinNotifications(
                reminderTime, reminderWindow, BookingStatus.CONFIRMED, PaymentStatus.COMPLETED);

            log.info("Found {} bookings for check-in reminder notifications", bookingsToRemind.size());

            for (Booking booking : bookingsToRemind) {
                try {
                    if (booking.getUserId() != null) {
                        String flightNumber = booking.getFlight().getFlightNumber();
                        String departureTime = booking.getFlight().getDepartureTime().toString();
                        String message = String.format(
                            "Nhắc nhở: Còn 3 giờ nữa là đến giờ bay %s. Vui lòng check-in nếu chưa thực hiện.",
                            flightNumber);
                        String title = "Nhắc nhở check-in";

                        notificationService.createAndSendNotification(
                            booking.getUserId().getId(),
                            NotificationType.CHECKIN_REMINDER.toString(),
                            message,
                            booking.getBookingId(),
                            title
                        );

                        log.debug("Sent check-in reminder notification for booking {}", booking.getBookingCode());
                    }
                } catch (Exception e) {
                    log.error("Failed to send check-in reminder notification for booking {}: {}",
                             booking.getBookingId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in sendCheckinReminderNotifications: {}", e.getMessage(), e);
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

        // Check payment status (for display purposes, not for blocking lookup)
        boolean isPaid = booking.getPayment() != null &&
                        booking.getPayment().getStatus() == PaymentStatus.COMPLETED;

        // For roundtrip flights, handle each segment separately
        if (booking.getFlightSegments() != null && booking.getFlightSegments().size() > 1) {
            return getCheckinEligiblePassengersForRoundtrip(booking, fullName, isPaid);
        } else {
            // Single flight - use original logic
            return getCheckinEligiblePassengersForSingleFlight(booking, isPaid);
        }
    }

    /**
     * Get all check-in eligible passengers for a booking (không filter theo tên)
     */
    public List<CheckinEligiblePassengerResponse> getAllCheckinEligiblePassengers(String bookingCode) {
        log.info("Getting all check-in eligible passengers for booking: {}", bookingCode);

        // First verify booking exists
        Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(bookingCode);
        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException("Booking not found: " + bookingCode);
        }

        Booking booking = bookingOpt.get();

        // Check payment status (for display purposes, not for blocking lookup)
        boolean isPaid = booking.getPayment() != null &&
                        booking.getPayment().getStatus() == PaymentStatus.COMPLETED;

        // For roundtrip flights, handle each segment separately
        if (booking.getFlightSegments() != null && booking.getFlightSegments().size() > 1) {
            return getAllCheckinEligiblePassengersForRoundtrip(booking, isPaid);
        } else {
            // Single flight - use original logic
            return getCheckinEligiblePassengersForSingleFlight(booking, isPaid);
        }
    }

    /**
     * Handle check-in eligibility for roundtrip flights (multiple segments)
     * Priority: segment 1 (outbound) must be checked in before segment 2 (return)
     */
    private List<CheckinEligiblePassengerResponse> getCheckinEligiblePassengersForRoundtrip(Booking booking, String fullName, boolean isPaid) {
        List<CheckinEligiblePassengerResponse> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Sort segments by departure time (segment order should already be correct)
        List<FlightSegment> sortedSegments = booking.getFlightSegments().stream()
                .sorted((s1, s2) -> s1.getFlight().getDepartureTime().compareTo(s2.getFlight().getDepartureTime()))
                .collect(Collectors.toList());

        // Check each segment eligibility
        for (int i = 0; i < sortedSegments.size(); i++) {
            FlightSegment segment = sortedSegments.get(i);
            Flight flight = segment.getFlight();
            boolean isFirstSegment = (i == 0);
            
            // Check if this segment is within check-in window
            boolean segmentInCheckinWindow = flight.getDepartureTime().isAfter(now.plusHours(1)) &&
                                           flight.getDepartureTime().isBefore(now.plusDays(1));

            for (Passenger passenger : booking.getPassengers()) {
                log.debug("Processing segment {} for passenger {}", segment.getSegmentOrder(), passenger.getPassengerId());
                
                CheckinEligiblePassengerResponse response = new CheckinEligiblePassengerResponse();
                response.setPassengerId(passenger.getPassengerId());
                response.setFirstName(passenger.getFirstName());
                response.setLastName(passenger.getLastName());
                response.setFullName(passenger.getFirstName() + " " + passenger.getLastName());
                response.setPassportNumber(passenger.getPassportNumber());
                
                // Set flight-specific information
                response.setFlightNumber(flight.getFlightNumber());
                response.setSegmentId(segment.getSegmentId());
                response.setSegmentOrder(segment.getSegmentOrder());
                response.setDepartureTime(flight.getDepartureTime());
                response.setArrivalTime(flight.getArrivalTime());
                response.setDepartureAirport(flight.getDepartureAirport().getAirportCode());
                response.setArrivalAirport(flight.getArrivalAirport().getAirportCode());

                // Get seat for this specific segment
                String seatNumber = getSeatNumberForPassengerAndSegment(passenger, segment);
                response.setSeatNumber(seatNumber);

                // Calculate ticket price per passenger based on passenger type
                BigDecimal ticketPrice = calculatePassengerTicketPriceForSegment(passenger, segment);
                response.setTicketPrice(ticketPrice);

                // Check if already checked in for this specific segment
                boolean alreadyCheckedInForSegment = isPassengerCheckedInForSegment(passenger, segment);
                response.setCheckedIn(alreadyCheckedInForSegment);

                // Get boarding pass URL for this segment
                String boardingPassUrl = getBoardingPassUrlForSegment(passenger, segment);
                response.setBoardingpassurl(boardingPassUrl);

                // Determine check-in status based on segment priority and conditions
                CheckinStatus status = determineCheckinStatusForSegment(booking, passenger, segment, 
                                                                       isFirstSegment, segmentInCheckinWindow, 
                                                                       alreadyCheckedInForSegment, sortedSegments, isPaid);
                response.setCheckinStatus(status);

                result.add(response);
            }
        }
        
        return result;
    }

    /**
     * Handle check-in eligibility for single flights
     */
    private List<CheckinEligiblePassengerResponse> getCheckinEligiblePassengersForSingleFlight(Booking booking, boolean isPaid) {
        LocalDateTime now = LocalDateTime.now();
        Flight flight = booking.getFlight();
        
        boolean canCheckIn = flight.getDepartureTime().isAfter(now.plusHours(1)) &&
                           flight.getDepartureTime().isBefore(now.plusDays(1));

        if (!canCheckIn) {
            throw new IllegalStateException("Check-in not available for this flight at this time");
        }

        return booking.getPassengers().stream()
                .map(passenger -> {
                    CheckinEligiblePassengerResponse response = new CheckinEligiblePassengerResponse();
                    response.setPassengerId(passenger.getPassengerId());
                    response.setFirstName(passenger.getFirstName());
                    response.setLastName(passenger.getLastName());
                    response.setFullName(passenger.getFirstName() + " " + passenger.getLastName());
                    response.setPassportNumber(passenger.getPassportNumber());
                    response.setSeatNumber(passenger.getSeat() != null ? passenger.getSeat().getSeatNumber() : null);
                    response.setFlightNumber(flight.getFlightNumber());
                    response.setDepartureTime(flight.getDepartureTime());
                    response.setArrivalTime(flight.getArrivalTime());

                    // Calculate ticket price per passenger based on passenger type
                    BigDecimal ticketPrice = calculatePassengerTicketPrice(passenger, booking);
                    response.setTicketPrice(ticketPrice);

                    // Check if already checked in (COMPLETED status)
                    boolean alreadyCheckedIn = checkinRepository.existsByPassengerAndCompleted(passenger);
                    response.setCheckedIn(alreadyCheckedIn);

                    // Get boarding pass URL from CheckIn entity
                    List<CheckIn> checkIns = checkinRepository.findByBookingIdWithBaggage(booking.getBookingId());
                    Optional<CheckIn> passengerCheckIn = checkIns.stream()
                            .filter(ci -> ci.getPassenger().getPassengerId().equals(passenger.getPassengerId()))
                            .findFirst();
                    if (passengerCheckIn.isPresent()) {
                        response.setBoardingpassurl(passengerCheckIn.get().getBoardingPassUrl());
                    }

                    // Set checkin status based on booking status first
                    if (booking.getStatus() == BookingStatus.CANCELLED) {
                        response.setCheckinStatus(CheckinStatus.BOOKING_CANCELLED);
                    } else if (alreadyCheckedIn) {
                        response.setCheckinStatus(CheckinStatus.ALREADY_CHECKED_IN);
                    } else if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
                        response.setCheckinStatus(CheckinStatus.BOOKING_NOT_CONFIRMED);
                    } else if (!isPaid) {
                        response.setCheckinStatus(CheckinStatus.BOOKING_NOT_CONFIRMED); // Not paid yet
                    } else {
                        response.setCheckinStatus(CheckinStatus.ELIGIBLE);
                    }

                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Handle check-in eligibility for roundtrip flights (multiple segments) - không filter theo tên
     * Priority: segment 1 (outbound) must be checked in before segment 2 (return)
     */
    private List<CheckinEligiblePassengerResponse> getAllCheckinEligiblePassengersForRoundtrip(Booking booking, boolean isPaid) {
        List<CheckinEligiblePassengerResponse> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Sort segments by departure time (segment order should already be correct)
        List<FlightSegment> sortedSegments = booking.getFlightSegments().stream()
                .sorted((s1, s2) -> s1.getFlight().getDepartureTime().compareTo(s2.getFlight().getDepartureTime()))
                .collect(Collectors.toList());

        // Check each segment eligibility
        for (int i = 0; i < sortedSegments.size(); i++) {
            FlightSegment segment = sortedSegments.get(i);
            Flight flight = segment.getFlight();
            boolean isFirstSegment = (i == 0);

            // Check if this segment is within check-in window
            boolean segmentInCheckinWindow = flight.getDepartureTime().isAfter(now.plusHours(1)) &&
                                           flight.getDepartureTime().isBefore(now.plusDays(1));

            for (Passenger passenger : booking.getPassengers()) {
                log.debug("Processing segment {} for passenger {}", segment.getSegmentOrder(), passenger.getPassengerId());

                CheckinEligiblePassengerResponse response = new CheckinEligiblePassengerResponse();
                response.setPassengerId(passenger.getPassengerId());
                response.setFirstName(passenger.getFirstName());
                response.setLastName(passenger.getLastName());
                response.setFullName(passenger.getFirstName() + " " + passenger.getLastName());
                response.setPassportNumber(passenger.getPassportNumber());

                // Set flight-specific information
                response.setFlightNumber(flight.getFlightNumber());
                response.setSegmentId(segment.getSegmentId());
                response.setSegmentOrder(segment.getSegmentOrder());
                response.setDepartureTime(flight.getDepartureTime());
                response.setArrivalTime(flight.getArrivalTime());
                response.setDepartureAirport(flight.getDepartureAirport().getAirportCode());
                response.setArrivalAirport(flight.getArrivalAirport().getAirportCode());

                // Get seat for this specific segment
                String seatNumber = getSeatNumberForPassengerAndSegment(passenger, segment);
                response.setSeatNumber(seatNumber);

                // Calculate ticket price per passenger based on passenger type
                BigDecimal ticketPrice = calculatePassengerTicketPriceForSegment(passenger, segment);
                response.setTicketPrice(ticketPrice);

                // Check if already checked in for this specific segment
                boolean alreadyCheckedInForSegment = isPassengerCheckedInForSegment(passenger, segment);
                response.setCheckedIn(alreadyCheckedInForSegment);

                // Determine check-in eligibility based on segment priority and status
                if (booking.getStatus() == BookingStatus.CANCELLED) {
                    response.setCheckinStatus(CheckinStatus.BOOKING_CANCELLED);
                } else if (alreadyCheckedInForSegment) {
                    response.setCheckinStatus(CheckinStatus.ALREADY_CHECKED_IN);
                } else if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
                    response.setCheckinStatus(CheckinStatus.BOOKING_NOT_CONFIRMED);
                } else if (!isPaid) {
                    response.setCheckinStatus(CheckinStatus.BOOKING_NOT_CONFIRMED);
                } else if (!segmentInCheckinWindow) {
                    response.setCheckinStatus(CheckinStatus.CHECKIN_NOT_OPEN);
                } else if (!isFirstSegment) {
                    // For return segments, check if outbound segment is checked in
                    boolean outboundCheckedIn = isPassengerCheckedInForSegment(passenger, sortedSegments.get(0));
                    if (!outboundCheckedIn) {
                        response.setCheckinStatus(CheckinStatus.PREVIOUS_SEGMENT_NOT_CHECKED_IN);
                    } else {
                        response.setCheckinStatus(CheckinStatus.ELIGIBLE);
                    }
                } else {
                    response.setCheckinStatus(CheckinStatus.ELIGIBLE);
                }

                result.add(response);
            }
        }

        return result;
    }

    /**
     * Get seat number for a specific passenger and flight segment
     */
    private String getSeatNumberForPassengerAndSegment(Passenger passenger, FlightSegment segment) {
        // Find seat assignment for this passenger and segment
        List<PassengerSeatAssignment> seatAssignments = passengerSeatAssignmentRepository.findByPassengerAndSegment(
                passenger.getPassengerId(), segment.getSegmentId());
        
        if (!seatAssignments.isEmpty()) {
            return seatAssignments.get(0).getSeat().getSeatNumber();
        }
        return null;
    }

    /**
     * Calculate ticket price for a specific passenger and segment
     */
    private BigDecimal calculatePassengerTicketPriceForSegment(Passenger passenger, FlightSegment segment) {
        BigDecimal segmentPrice = segment.getPrice();
        return calculatePassengerBasePrice(segmentPrice, passenger.getType());
    }

    /**
     * Check if passenger is already checked in for a specific segment
     * Uses segment-specific check-in records
     */
    private boolean isPassengerCheckedInForSegment(Passenger passenger, FlightSegment segment) {
        // Check if there's a completed check-in record for this passenger and segment
        boolean result = checkinRepository.existsByPassengerAndSegmentAndCompleted(passenger, segment);
        
        // Debug logging
        log.debug("Checking check-in status for passenger {} and segment {}: result = {}", 
                passenger.getPassengerId(), segment.getSegmentId(), result);
        
        // Additional debug: find all check-in records for this passenger to see what's in database
        List<CheckIn> allCheckIns = checkinRepository.findByPassengerWithSegments(passenger);
        log.debug("All check-in records for passenger {}: {}", passenger.getPassengerId(), 
                allCheckIns.stream().map(ci -> String.format("CheckIn[id=%d, segmentId=%d, status=%s]", 
                        ci.getCheckInId(), 
                        ci.getFlightSegment() != null ? ci.getFlightSegment().getSegmentId() : null,
                        ci.getStatus())).collect(Collectors.toList()));
        
        return result;
    }

    /**
     * Check if passenger can check-in for a specific segment
     * considering roundtrip rules and flight timing
     */
    private boolean canCheckInForSegment(Booking booking, Passenger passenger, FlightSegment segment) {
        LocalDateTime now = LocalDateTime.now();
        Flight segmentFlight = segment.getFlight();
        
        // Basic timing check: within 24 hours, but not within 1 hour of departure
        if (segmentFlight.getDepartureTime().isBefore(now.plusHours(1)) ||
            segmentFlight.getDepartureTime().isAfter(now.plusDays(1))) {
            return false;
        }

        // Already checked in for this segment
        if (isPassengerCheckedInForSegment(passenger, segment)) {
            return false;
        }

        // For roundtrip: check sequential requirements
        if (booking.getFlightSegments().size() > 1) {
            return canCheckInForRoundtripSegment(booking, passenger, segment);
        }

        return true;
    }

    /**
     * Check roundtrip-specific rules for segment check-in
     */
    private boolean canCheckInForRoundtripSegment(Booking booking, Passenger passenger, FlightSegment targetSegment) {
        // Sort segments by departure time
        List<FlightSegment> sortedSegments = booking.getFlightSegments().stream()
                .sorted((s1, s2) -> s1.getFlight().getDepartureTime().compareTo(s2.getFlight().getDepartureTime()))
                .collect(Collectors.toList());

        int targetIndex = sortedSegments.indexOf(targetSegment);
        
        // First segment can always be checked in (if timing allows)
        if (targetIndex == 0) {
            return true;
        }

        // For later segments, check if previous segments have been checked in
        for (int i = 0; i < targetIndex; i++) {
            FlightSegment previousSegment = sortedSegments.get(i);

            // Previous segment must be checked in
            if (!isPassengerCheckedInForSegment(passenger, previousSegment)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get boarding pass URL for a specific passenger and segment
     * Returns segment-specific boarding pass URL
     */
    private String getBoardingPassUrlForSegment(Passenger passenger, FlightSegment segment) {
        // Get the segment-specific check-in record
        Optional<CheckIn> segmentCheckIn = checkinRepository.findByPassengerAndSegment(passenger, segment);
        return segmentCheckIn.map(CheckIn::getBoardingPassUrl).orElse(null);
    }

    /**
     * Determine check-in status for a specific segment considering roundtrip rules
     */
    private CheckinStatus determineCheckinStatusForSegment(Booking booking, Passenger passenger,
                                                         FlightSegment currentSegment, boolean isFirstSegment,
                                                         boolean segmentInCheckinWindow, boolean alreadyCheckedInForSegment,
                                                         List<FlightSegment> allSegments, boolean isPaid) {

        // Basic booking status checks
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return CheckinStatus.BOOKING_CANCELLED;
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
            return CheckinStatus.BOOKING_NOT_CONFIRMED;
        }

        if (alreadyCheckedInForSegment) {
            return CheckinStatus.ALREADY_CHECKED_IN;
        }

        // Check payment status (for check-in eligibility, not for info display)
        if (!isPaid) {
            return CheckinStatus.BOOKING_NOT_CONFIRMED; // Show as not confirmed if not paid
        }

        // Check if segment is within check-in window
        if (!segmentInCheckinWindow) {
            LocalDateTime now = LocalDateTime.now();
            if (currentSegment.getFlight().getDepartureTime().isBefore(now)) {
                return CheckinStatus.FLIGHT_DEPARTED;
            } else {
                return CheckinStatus.CHECKIN_NOT_OPEN;
            }
        }

        // For roundtrip: check sequential check-in requirement
        if (!isFirstSegment) {
            // Check if previous segments have been checked in by this passenger
            for (int i = 0; i < allSegments.indexOf(currentSegment); i++) {
                FlightSegment previousSegment = allSegments.get(i);
                boolean previousCheckedIn = isPassengerCheckedInForSegment(passenger, previousSegment);

                log.debug("Checking previous segment {} for passenger {}: checkedIn = {}",
                        previousSegment.getSegmentId(), passenger.getPassengerId(), previousCheckedIn);

                if (!previousCheckedIn) {
                    // Check if it's timing issue (previous flight hasn't departed yet)
                    LocalDateTime now = LocalDateTime.now();
                    if (previousSegment.getFlight().getDepartureTime().isAfter(now.minusHours(1))) {
                        return CheckinStatus.PREVIOUS_SEGMENT_NOT_CHECKED_IN;
                    }
                }
            }
        }

        return CheckinStatus.ELIGIBLE;
    }

    @Override
    public List<CheckinEligiblePassengerResponse> getPassengersWithCheckinStatus(String bookingCode, String fullName) {
        log.info("Getting passengers with check-in status for booking: {} and passenger: {}", bookingCode, fullName);

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
                .filter(passenger -> {
                    // Chỉ trả về hành khách có tên khớp với fullName truyền vào
                    String passengerFullName = passenger.getFirstName() + " " + passenger.getLastName();
                    return passengerFullName.equalsIgnoreCase(fullName);
                })
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

                    // Get boarding pass URL from CheckIn entity - chỉ của hành khách này
                    List<CheckIn> checkIns = checkinRepository.findByBookingIdWithBaggage(booking.getBookingId());
                    Optional<CheckIn> passengerCheckIn = checkIns.stream()
                            .filter(ci -> ci.getPassenger().getPassengerId().equals(passenger.getPassengerId()))
                            .findFirst();
                    if (passengerCheckIn.isPresent()) {
                        response.setBoardingpassurl(passengerCheckIn.get().getBoardingPassUrl());
                    }

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
     * Sử dụng logic tính giá theo tỷ lệ phần trăm
     */
    private BigDecimal calculatePassengerTicketPrice(Passenger passenger, Booking booking) {
        if (booking.getTotalAmount() == null || booking.getPassengers().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Tính tổng giá cơ bản của tất cả segments
        BigDecimal totalBasePrice = booking.getFlightSegments().stream()
                .map(FlightSegment::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính giá cho hành khách này dựa trên loại
        BigDecimal passengerBasePrice = calculatePassengerBasePrice(totalBasePrice, passenger.getType());

        // Tính tỷ lệ phần trăm của hành khách này trong tổng booking
        BigDecimal passengerRatio = passengerBasePrice.divide(
            booking.getTotalAmount().subtract(calculateNonTicketAmounts(booking)),
            4, java.math.RoundingMode.HALF_UP
        );

        // Áp dụng tỷ lệ để tính giá vé thực tế
        return booking.getTotalAmount().multiply(passengerRatio);
    }

    /**
     * Tính các khoản phí không phải là giá vé (hành lý, ghế, dịch vụ)
     */
    private BigDecimal calculateNonTicketAmounts(Booking booking) {
        BigDecimal nonTicketAmount = BigDecimal.ZERO;

        // Baggage amount - lấy từ CheckIn entities
        List<CheckIn> checkIns = checkinRepository.findByBookingIdWithBaggage(booking.getBookingId());
        for (CheckIn checkIn : checkIns) {
            if (checkIn.getBaggage() != null && checkIn.getBaggage().getPackagePrice() != null) {
                nonTicketAmount = nonTicketAmount.add(checkIn.getBaggage().getPackagePrice());
            }
        }

        // Seat type amount
        if (booking.getPassengers() != null) {
            for (Passenger passenger : booking.getPassengers()) {
                for (PassengerSeatAssignment assignment : passenger.getSeatAssignments()) {
                    Seat seat = assignment.getSeat();
                    if (seat != null && seat.getType() != null) {
                        try {
                            SeatTypePrice seatTypePrice = SeatTypePrice.fromSeatType(seat.getType());
                            nonTicketAmount = nonTicketAmount.add(seatTypePrice.getAdditionalPrice());
                        } catch (Exception e) {
                            // Ignore invalid seat types
                        }
                    }
                }
            }
        }

        // Ancillary services amount
        List<BookingAncillaryService> bookingAncillaryServices =
                bookingAncillaryServiceRepository.findByBookingId(booking.getBookingId());
        for (BookingAncillaryService service : bookingAncillaryServices) {
            if (service.getTotalPrice() != null) {
                nonTicketAmount = nonTicketAmount.add(service.getTotalPrice());
            }
        }

        return nonTicketAmount;
    }

    @Override
    @Transactional
    public CheckinResponse processCheckin(CheckinRequest request) {
        log.info("Processing check-in for booking: {}, passengerId: {}, passengerName: {}",
                request.getBookingCode(), request.getPassengerId(), request.getPassengerFullName());

        // Validate booking and passenger - Ưu tiên dùng passengerId
        Optional<Booking> bookingOpt;
        if (request.getPassengerId() != null) {
            // Tìm booking theo passengerId (chính xác hơn)
            bookingOpt = bookingRepository.findByBookingCodeAndPassengerId(
                    request.getBookingCode(), request.getPassengerId());
        } else if (request.getPassengerFullName() != null) {
            // Fallback: dùng tên (backward compatibility)
            bookingOpt = bookingRepository.findByBookingCodeAndPassengerFullName(
                    request.getBookingCode(), request.getPassengerFullName());
        } else {
            throw new IllegalArgumentException("Either passengerId or passengerFullName must be provided");
        }

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException("Booking not found or access denied");
        }

        Booking booking = bookingOpt.get();

        // Fetch all relationships using findById
        booking = bookingRepository.findById(booking.getBookingId()).orElse(booking);

        // Fetch passengers if not loaded
        if (booking.getPassengers() == null || booking.getPassengers().isEmpty()) {
            booking = bookingRepository.findByIdWithPassengers(booking.getBookingId()).orElse(booking);
        }

        // **CẬP NHẬT: Validate booking status - phải là CONFIRMED hoặc COMPLETED**
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking must be confirmed before check-in. Current status: " + booking.getStatus());
        }

        // Check if booking is paid (required for check-in) - kiểm tra có ít nhất 1 payment COMPLETED
        boolean isPaid = booking.getPayment() != null && booking.getPayment().getStatus() == PaymentStatus.COMPLETED;
        if (!isPaid) {
            throw new IllegalStateException("Booking must be paid before check-in");
        }

        // Check if flight is eligible for check-in (not departed, within check-in window)
        // For roundtrip bookings, check the specific segment's flight time
        LocalDateTime now = LocalDateTime.now();
        boolean canCheckIn;

        if (request.getSegmentId() != null) {
            // Find the specific segment for check-in
            FlightSegment targetSegment = booking.getFlightSegments().stream()
                    .filter(s -> s.getSegmentId().equals(request.getSegmentId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + request.getSegmentId()));

            // Check if this segment's flight is eligible for check-in
            canCheckIn = targetSegment.getFlight().getDepartureTime().isAfter(now.plusHours(1)) &&
                        targetSegment.getFlight().getDepartureTime().isBefore(now.plusDays(1));

            log.debug("Checking check-in eligibility for segment {} (flight {}): departure={}, canCheckIn={}",
                    request.getSegmentId(), targetSegment.getFlight().getFlightNumber(),
                    targetSegment.getFlight().getDepartureTime(), canCheckIn);
        } else {
            // For backward compatibility: use main flight for single flight bookings
            canCheckIn = booking.getFlight().getDepartureTime().isAfter(now.plusHours(1)) &&
                        booking.getFlight().getDepartureTime().isBefore(now.plusDays(1));

            log.debug("Checking check-in eligibility for main flight: departure={}, canCheckIn={}",
                    booking.getFlight().getDepartureTime(), canCheckIn);
        }

        if (!canCheckIn) {
            throw new IllegalStateException("Check-in not available for this flight at this time");
        }

        Passenger passenger = booking.getPassengers().stream()
                .filter(p -> p.getPassengerId().equals(request.getPassengerId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));

        // For roundtrip bookings: Check if passenger is already checked in for THIS SPECIFIC SEGMENT
        // Not for all segments (passenger can check-in multiple segments in roundtrip)
        if (request.getSegmentId() != null) {
            FlightSegment targetSegment = booking.getFlightSegments().stream()
                    .filter(s -> s.getSegmentId().equals(request.getSegmentId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + request.getSegmentId()));

            // Check if passenger is already checked in for this specific segment
            boolean alreadyCheckedInForSegment = checkinRepository.existsByPassengerAndSegmentAndCompleted(passenger, targetSegment);
            if (alreadyCheckedInForSegment) {
                throw new IllegalStateException("Passenger is already checked in for this segment");
            }
        } else {
            // For backward compatibility: check if already checked in for any segment (single flight)
            boolean alreadyCheckedIn = checkinRepository.existsByPassengerAndCompleted(passenger);
            if (alreadyCheckedIn) {
                throw new IllegalStateException("Passenger is already checked in");
            }
        }

        CheckinResponse response = new CheckinResponse();
        response.setPassengerId(passenger.getPassengerId());
        response.setPassengerName(passenger.getFirstName() + " " + passenger.getLastName());
        response.setBookingId(booking.getBookingId());

        // For roundtrip bookings, get seat info for the specific segment being checked in
        String currentSeatNumber;
        String currentSeatType;
        if (request.getSegmentId() != null) {
            FlightSegment targetSegment = booking.getFlightSegments().stream()
                    .filter(s -> s.getSegmentId().equals(request.getSegmentId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + request.getSegmentId()));

            currentSeatNumber = getSeatNumberForPassengerAndSegment(passenger, targetSegment);
            // Get seat type from assignment if available
            List<PassengerSeatAssignment> assignments = passengerSeatAssignmentRepository.findByPassengerAndSegment(
                    passenger.getPassengerId(), targetSegment.getSegmentId());
            currentSeatType = !assignments.isEmpty() ? assignments.get(0).getSeat().getType().toString() : null;
        } else {
            // For backward compatibility: use passenger's main seat
            currentSeatNumber = passenger.getSeat() != null ? passenger.getSeat().getSeatNumber() : null;
            currentSeatType = passenger.getSeat() != null ? passenger.getSeat().getType().toString() : null;
        }

        response.setOldSeatNumber(currentSeatNumber);
        response.setSeatNumber(currentSeatNumber);
        response.setSeatType(currentSeatType);
        response.setTicketPrice(calculatePassengerTicketPrice(passenger, booking));

        BigDecimal totalCharge = BigDecimal.ZERO;
        BigDecimal seatCharge = BigDecimal.ZERO;
        BigDecimal servicesCharge = BigDecimal.ZERO;

        // Handle seat change if requested - Seat changes are always free/completed
        // Only process seat change if user explicitly requests it (newSeatId or newSeatNumber provided)
        Long newSeatId = null;
        if (request.getNewSeatId() != null) {
            newSeatId = request.getNewSeatId();
        } else if (request.getNewSeatNumber() != null) {
            // For seat changes, find the correct flight based on segment
            Long flightIdForSeat;
            if (request.getSegmentId() != null) {
                FlightSegment targetSegment = booking.getFlightSegments().stream()
                        .filter(s -> s.getSegmentId().equals(request.getSegmentId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + request.getSegmentId()));
                flightIdForSeat = targetSegment.getFlight().getFlightId();
            } else {
                flightIdForSeat = booking.getFlight().getFlightId();
            }

            List<Seat> seats = seatRepository.findByFlightIdAndSeatNumber(flightIdForSeat, request.getNewSeatNumber());
            if (!seats.isEmpty()) {
                newSeatId = seats.get(0).getSeatId(); // Lấy seat đầu tiên
            }
        }
        // Note: For check-in without seat change, newSeatId remains null and no seat change occurs

        if (newSeatId != null) {
            // Kiểm tra xem có phải là seat change thực sự không
            // For roundtrip bookings, check if the new seat is already assigned to this passenger for this segment
            boolean isAlreadyAssigned = false;
            if (request.getSegmentId() != null) {
                List<PassengerSeatAssignment> assignments = passengerSeatAssignmentRepository
                        .findByPassengerAndSegment(passenger.getPassengerId(), request.getSegmentId());
                Long finalNewSeatId = newSeatId; // Create final variable for lambda
                isAlreadyAssigned = assignments.stream()
                        .anyMatch(assignment -> assignment.getSeat().getSeatId().equals(finalNewSeatId));
            }

            // Only process seat change if it's not already assigned to this passenger for this segment
            if (!isAlreadyAssigned) {
                Seat currentSeat = passenger.getSeat();
                if (currentSeat == null || !currentSeat.getSeatId().equals(newSeatId)) {
                    // For roundtrip bookings, pass the segmentId to handleSeatChange
                    Long segmentIdForSeatChange = request.getSegmentId();
                    seatCharge = handleSeatChange(booking, passenger, newSeatId, segmentIdForSeatChange);
                    response.setSeatChangeCharge(seatCharge);
                    response.setNewSeatNumber(passenger.getSeat() != null ? passenger.getSeat().getSeatNumber() : null);
                    totalCharge = totalCharge.add(seatCharge);

                    // Update response with new seat info after seat change
                    response.setSeatNumber(passenger.getSeat() != null ? passenger.getSeat().getSeatNumber() : null);
                    response.setSeatType(passenger.getSeat() != null ? passenger.getSeat().getType().toString() : null);
                }
            } else {
                // Seat is already assigned to this passenger for this segment, no change needed
                log.info("Seat {} is already assigned to passenger {} for segment {}, skipping seat change",
                        newSeatId, passenger.getPassengerId(), request.getSegmentId());
            }
        }

        // Handle ancillary services updates - Only add new services, no removal
        if (request.getServicesToAdd() != null && !request.getServicesToAdd().isEmpty()) {
            BigDecimal addCharge = handleAddAncillaryServices(booking, passenger, request.getServicesToAdd());
            response.setServicesAddedCharge(addCharge);
            servicesCharge = servicesCharge.add(addCharge);
            totalCharge = totalCharge.add(addCharge);
        }

        response.setTotalCharge(totalCharge);

        // At checkin stage, payment should already be completed via update-total + payment APIs
        // Checkin only validates payment and creates checkin record, doesn't calculate additional charges
        if (booking.getPayment() != null) {
            Payment existingPayment = booking.getPayment();
            if (existingPayment.getStatus() == PaymentStatus.COMPLETED &&
                existingPayment.getAmount().compareTo(booking.getTotalAmount()) >= 0) {
                // Payment is sufficient - proceed with checkin
                log.info("Payment {} validated for checkin. Amount: {}, Booking total: {}",
                        existingPayment.getPaymentId(), existingPayment.getAmount(), booking.getTotalAmount());

                response.setStatus("SUCCESS");
                response.setMessage("Check-in processed successfully");
                response.setPaymentRequired(false);
            } else {
                // Payment insufficient or not completed
                log.error("Payment validation failed for checkin. Payment status: {}, Amount: {}, Required: {}",
                        existingPayment.getStatus(), existingPayment.getAmount(), booking.getTotalAmount());
                throw new IllegalStateException("Payment not completed or insufficient. Please complete payment before check-in.");
            }
        } else {
            throw new IllegalStateException("No payment found for booking. Payment must be completed before check-in.");
        }

        // Get the specific segment for check-in
        FlightSegment segment;
        if (request.getSegmentId() != null) {
            segment = booking.getFlightSegments().stream()
                    .filter(s -> s.getSegmentId().equals(request.getSegmentId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + request.getSegmentId()));
        } else {
            // For backward compatibility: use first available segment for single flight
            segment = booking.getFlightSegments().get(0);
        }

        // Validate segment eligibility for check-in
        if (!canCheckInForSegment(booking, passenger, segment)) {
            throw new IllegalStateException("Check-in not available for this segment at this time");
        }

        // Update check-in status - find segment-specific check-in record
        Optional<CheckIn> existingCheckIn = checkinRepository.findByPassengerAndSegment(passenger, segment);

        CheckIn checkIn;
        if (existingCheckIn.isPresent()) {
            checkIn = existingCheckIn.get();
            checkIn.setStatus(CheckinStatus.COMPLETED); // Always COMPLETED since payment is validated
            checkIn.setCheckedAt(LocalDateTime.now());
            // Update seat number from passenger's current seat
            if (passenger.getSeat() != null) {
                checkIn.setSeatNumber(passenger.getSeat().getSeatNumber());
            }
            // Set ticket price if not already set
            if (checkIn.getTicketPrice() == null) {
                checkIn.setTicketPrice(booking.getTotalAmount());
            }
            checkinRepository.save(checkIn);
        } else {
            // Create new check-in record
            checkIn = new CheckIn();
            checkIn.setBooking(booking);
            checkIn.setPassenger(passenger);
            checkIn.setFlightSegment(segment); // Associate with specific segment
            checkIn.setStatus(CheckinStatus.COMPLETED); // Always COMPLETED since payment is validated
            checkIn.setCheckedAt(LocalDateTime.now());
            checkIn.setActive(true);
            // Set seat number for this specific segment
            String seatNumberForSegment = getSeatNumberForPassengerAndSegment(passenger, segment);
            checkIn.setSeatNumber(seatNumberForSegment);
            // Set ticket price for this segment
            BigDecimal segmentPrice = calculatePassengerTicketPriceForSegment(passenger, segment);
            checkIn.setTicketPrice(segmentPrice);
            checkinRepository.save(checkIn);
        }

        // Populate response with check-in details
        response.setCheckinId(checkIn.getCheckInId());
        response.setIssueDate(checkIn.getCheckedAt());
        response.setCreatedAt(checkIn.getCreatedAt());
        response.setUpdatedAt(checkIn.getUpdatedAt());
        response.setActive(checkIn.isActive());
        response.setDeleted(checkIn.isDeleted());
        response.setDeletedAt(checkIn.getDeletedAt());
        response.setTicketPrice(checkIn.getTicketPrice());

        // Set boarding pass URL if check-in is completed
        if (checkIn.getStatus() == CheckinStatus.COMPLETED) {
            try {
                // Generate boarding pass using BoardingPassService
                String boardingPassUrl = boardingPassService.generateAndSendBoardingPass(checkIn);
                log.info("Generated boarding pass URL for check-in {}: {}", checkIn.getCheckInId(), boardingPassUrl);

                if (boardingPassUrl != null && !boardingPassUrl.isEmpty()) {
                    checkIn.setBoardingPassUrl(boardingPassUrl);
                    checkinRepository.save(checkIn);
                    response.setBoardingPassUrl(boardingPassUrl);

                    log.info("Boarding pass URL set in response for check-in {}: {}", checkIn.getCheckInId(), boardingPassUrl);

                    // GỬI THÔNG BÁO SOCKET
                    String message = String.format("Check-in cho hành khách %s %s thành công. Boarding pass đã sẵn sàng.", passenger.getFirstName(), passenger.getLastName());
                    notificationService.createAndSendNotification(
                        booking.getUserId().getId(),
                        NotificationType.CHECKIN_SUCCESSFUL.toString(),
                        message,
                        checkIn.getCheckInId(),
                        "Check-in thành công"
                    );
                } else {
                    log.warn("Boarding pass URL is null or empty for check-in {}", checkIn.getCheckInId());
                }
            } catch (Exception e) {
                log.error("Failed to generate boarding pass for check-in {}: {}", checkIn.getCheckInId(), e.getMessage());
                // Continue without boarding pass - checkin is still successful
            }
        }

        return response;
    }


    private BigDecimal handleSeatChange(Booking booking, Passenger passenger, Long newSeatId, Long segmentId) {
        // Find the new seat by ID (chính xác hơn)
        Seat newSeat = seatRepository.findById(newSeatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with ID: " + newSeatId));

        // Verify seat belongs to the correct flight
        Long expectedFlightId;
        if (segmentId != null) {
            // For roundtrip bookings, find the flight for the specific segment
            FlightSegment targetSegment = booking.getFlightSegments().stream()
                    .filter(s -> s.getSegmentId().equals(segmentId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + segmentId));
            expectedFlightId = targetSegment.getFlight().getFlightId();
        } else {
            // For single flight bookings, use the main flight
            expectedFlightId = booking.getFlight().getFlightId();
        }

        if (!newSeat.getFlight().getFlightId().equals(expectedFlightId)) {
            throw new IllegalArgumentException("Seat " + newSeatId + " does not belong to flight " + expectedFlightId);
        }

        log.info("Selected seat for change: id={}, number={}, status={}, type={}",
                newSeat.getSeatId(), newSeat.getSeatNumber(), newSeat.getStatus(), newSeat.getType());

        // Check if seat is available
        if (!SeatStatus.AVAILABLE.equals(newSeat.getStatus())) {
            log.error("Seat {} is not available. Status: {}, bookedBy: {}",
                    newSeat.getSeatId(), newSeat.getStatus(),
                    newSeat.getBookedByPassenger() != null ? newSeat.getBookedByPassenger().getPassengerId() : "null");
            throw new SeatNotAvailableException("Ghế " + newSeat.getSeatId()+"đã được đặt trước.");
        }

        // Calculate price difference (same logic for both single flight and roundtrip)
        BigDecimal oldSeatPrice = BigDecimal.ZERO;
        BigDecimal newSeatPrice = calculateSeatPrice(newSeat);

        // Get old seat price based on booking type
        if (segmentId != null) {
            // For roundtrip, get old seat from assignment
            List<PassengerSeatAssignment> existingAssignments = passengerSeatAssignmentRepository
                    .findByPassengerAndSegment(passenger.getPassengerId(), segmentId);
            if (!existingAssignments.isEmpty()) {
                Seat oldAssignedSeat = existingAssignments.get(0).getSeat();
                oldSeatPrice = calculateSeatPrice(oldAssignedSeat);
            }
        } else {
            // For single flight, get old seat from passenger
            Seat oldSeat = passenger.getSeat();
            oldSeatPrice = calculateSeatPrice(oldSeat);
        }

        BigDecimal priceDifference = newSeatPrice.subtract(oldSeatPrice);

        log.info("Seat change calculation: oldSeatPrice={}, newSeatPrice={}, difference={}",
                oldSeatPrice, newSeatPrice, priceDifference);

        // Handle seat assignment based on booking type
        if (segmentId != null) {
            // For roundtrip bookings, update PassengerSeatAssignment for the specific segment
            FlightSegment targetSegment = booking.getFlightSegments().stream()
                    .filter(s -> s.getSegmentId().equals(segmentId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + segmentId));

            // Find existing assignment for this passenger and segment
            List<PassengerSeatAssignment> existingAssignments = passengerSeatAssignmentRepository
                    .findByPassengerAndSegment(passenger.getPassengerId(), segmentId);

            Seat oldAssignedSeat = null;
            if (!existingAssignments.isEmpty()) {
                // Update existing assignment
                PassengerSeatAssignment existingAssignment = existingAssignments.get(0);
                oldAssignedSeat = existingAssignment.getSeat();
                existingAssignment.setSeat(newSeat);
                existingAssignment.setStatus(SeatStatus.OCCUPIED);
                passengerSeatAssignmentRepository.save(existingAssignment);
            } else {
                // Create new assignment
                PassengerSeatAssignment newAssignment = PassengerSeatAssignment.builder()
                        .passenger(passenger)
                        .flightSegment(targetSegment)
                        .seat(newSeat)
                        .status(SeatStatus.OCCUPIED)
                        .build();
                passengerSeatAssignmentRepository.save(newAssignment);
            }

            // Free up old seat if it exists and is different from new seat
            if (oldAssignedSeat != null && !oldAssignedSeat.getSeatId().equals(newSeat.getSeatId())) {
                oldAssignedSeat.setStatus(SeatStatus.AVAILABLE);
                oldAssignedSeat.setBookedByPassenger(null);
                seatRepository.save(oldAssignedSeat);
            }
        } else {
            // For single flight bookings, update passenger's main seat
            // Lưu ghế cũ để xử lý
            Seat oldSeat = passenger.getSeat();

            // Xóa ghế cũ: set AVAILABLE và remove passenger
            if (oldSeat != null) {
                oldSeat.setStatus(SeatStatus.AVAILABLE);
                oldSeat.setBookedByPassenger(null);
                seatRepository.save(oldSeat);
            }

            // Cập nhật ghế mới cho passenger
            passenger.setSeat(newSeat);
            passengerRepository.save(passenger);
        }

        // Set ghế mới thành OCCUPIED và assign passenger
        newSeat.setStatus(SeatStatus.OCCUPIED);
        newSeat.setBookedByPassenger(passenger);
        seatRepository.save(newSeat);

        log.info("Seat changed for passenger {}: price difference: {}",
                passenger.getPassengerId(), priceDifference);

        return priceDifference;
    }

    private BigDecimal handleAddAncillaryServices(Booking booking, Passenger passenger,
                                                 List<BookingAncillaryServiceRequest> servicesToAdd) {
        BigDecimal totalCharge = BigDecimal.ZERO;

        for (BookingAncillaryServiceRequest serviceRequest : servicesToAdd) {
            AncillaryService service = ancillaryServiceRepository.findById(serviceRequest.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ancillary service not found: " + serviceRequest.getServiceId()));

            // Check if service already exists for this passenger (don't add duplicates)
            List<BookingAncillaryService> existingServices = bookingAncillaryServiceRepository.findByBookingId(booking.getBookingId());
            boolean alreadyExists = existingServices.stream()
                    .anyMatch(bs -> bs.getAncillaryService().getServiceId().equals(serviceRequest.getServiceId()) &&
                                   (bs.getPassenger() == null || bs.getPassenger().equals(passenger)));

            if (!alreadyExists) {
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

        return totalRefund.negate();
    }

    private BigDecimal calculateSeatPrice(Seat seat) {
        if (seat == null) return BigDecimal.ZERO;

        // Use the predefined seat type pricing from enum
        SeatTypePrice seatTypePrice = SeatTypePrice.fromSeatType(seat.getType());
        return seatTypePrice.getAdditionalPrice();
    }

    @Override
    @Transactional
    public SeatChangeCalculationResponse calculateSeatChange(SeatChangeCalculationRequest request) {
        log.info("Calculating seat change and services for booking {}, passenger {}, newSeatId {}, servicesToAdd: {}",
                request.getBookingCode(), request.getPassengerId(), request.getNewSeatId(),
                request.getServicesToAdd() != null ? request.getServicesToAdd().size() : 0);

        // Find booking
        Optional<Booking> bookingOpt = bookingRepository.findByBookingCodeAndPassengerId(
                request.getBookingCode(), request.getPassengerId());
        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException("Booking not found with bookingCode: " + request.getBookingCode() +
                    " and passengerId: " + request.getPassengerId());
        }
        Booking booking = bookingOpt.get();

        // Find passenger
        Passenger passenger = booking.getPassengers().stream()
                .filter(p -> p.getPassengerId().equals(request.getPassengerId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));

        BigDecimal totalCharge = BigDecimal.ZERO;
        BigDecimal seatCharge = BigDecimal.ZERO;
        BigDecimal servicesCharge = BigDecimal.ZERO;
        List<String> servicesAdded = new ArrayList<>();

        // Calculate seat change if provided
        String newSeatNumber = null;
        Seat newSeat = null;
        if (request.getNewSeatId() != null || request.getNewSeatNumber() != null) {
            // Find new seat
            if (request.getNewSeatId() != null) {
                newSeat = seatRepository.findById(request.getNewSeatId())
                        .orElseThrow(() -> new ResourceNotFoundException("Seat not found with ID: " + request.getNewSeatId()));
            } else {
                List<Seat> seats = seatRepository.findByFlightIdAndSeatNumberForUpdate(booking.getFlight().getFlightId(), request.getNewSeatNumber());
                if (seats.isEmpty()) {
                    throw new ResourceNotFoundException("Seat not found: " + request.getNewSeatNumber());
                }
                // Prioritize AVAILABLE seats
                newSeat = seats.stream()
                        .filter(s -> SeatStatus.AVAILABLE.equals(s.getStatus()))
                        .findFirst()
                        .orElse(seats.get(0));
            }

            // Check if seat is available
            if (!SeatStatus.AVAILABLE.equals(newSeat.getStatus())) {
                throw new IllegalStateException("Seat is not available: " + newSeat.getSeatNumber());
            }

            newSeatNumber = newSeat.getSeatNumber();

            // Get current seat
            Seat oldSeat = passenger.getSeat();

            // Calculate seat price difference
            BigDecimal oldSeatPrice = calculateSeatPrice(oldSeat);
            BigDecimal newSeatPrice = calculateSeatPrice(newSeat);
            seatCharge = newSeatPrice.subtract(oldSeatPrice);
            totalCharge = totalCharge.add(seatCharge);

            log.info("Seat change calculation: oldSeat={}, type={}, price={}, newSeat={}, type={}, price={}, difference={}",
                    oldSeat != null ? oldSeat.getSeatNumber() : "none",
                    oldSeat != null ? oldSeat.getType() : "none",
                    oldSeatPrice,
                    newSeat.getSeatNumber(),
                    newSeat.getType(),
                    newSeatPrice,
                    seatCharge);
        }

        // Calculate ancillary services if provided
        if (request.getServicesToAdd() != null && !request.getServicesToAdd().isEmpty()) {
            for (BookingAncillaryServiceRequest serviceRequest : request.getServicesToAdd()) {
                AncillaryService service = ancillaryServiceRepository.findById(serviceRequest.getServiceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ancillary service not found: " + serviceRequest.getServiceId()));

                // Check if service already exists for this passenger (don't add duplicates)
                List<BookingAncillaryService> existingServices = bookingAncillaryServiceRepository.findByBookingId(booking.getBookingId());
                boolean alreadyExists = existingServices.stream()
                        .anyMatch(bs -> bs.getAncillaryService().getServiceId().equals(serviceRequest.getServiceId()) &&
                                   (bs.getPassenger() == null || bs.getPassenger().equals(passenger)));

                if (!alreadyExists) {
                    servicesCharge = servicesCharge.add(service.getPrice());
                    servicesAdded.add(service.getServiceName());
                    log.info("Adding ancillary service: {} - {} VND", service.getServiceName(), service.getPrice());
                } else {
                    log.info("Ancillary service already exists: {}", service.getServiceName());
                }
            }
            totalCharge = totalCharge.add(servicesCharge);
        }

        boolean requiresPayment = totalCharge.compareTo(BigDecimal.ZERO) > 0;

        String message;
        if (seatCharge.compareTo(BigDecimal.ZERO) > 0 && servicesCharge.compareTo(BigDecimal.ZERO) > 0) {
            message = String.format("Seat change (%s VND) and %d services (%s VND) require total payment of %s VND",
                    seatCharge, servicesAdded.size(), servicesCharge, totalCharge);
        } else if (seatCharge.compareTo(BigDecimal.ZERO) > 0) {
            message = String.format("Seat change requires additional payment of %s VND", seatCharge);
        } else if (servicesCharge.compareTo(BigDecimal.ZERO) > 0) {
            message = String.format("%d ancillary services require payment of %s VND", servicesAdded.size(), servicesCharge);
        } else {
            message = "No additional payment required";
        }

        return SeatChangeCalculationResponse.builder()
                .priceDifference(seatCharge)
                .oldSeatPrice(calculateSeatPrice(passenger.getSeat()))
                .newSeatPrice(seatCharge.add(calculateSeatPrice(passenger.getSeat())))
                .oldSeatType(passenger.getSeat() != null ? passenger.getSeat().getType().toString() : null)
                .newSeatType(newSeat != null ? newSeat.getType().toString() : null)
                .oldSeatNumber(passenger.getSeat() != null ? passenger.getSeat().getSeatNumber() : null)
                .newSeatNumber(newSeatNumber)
                .servicesCharge(servicesCharge)
                .servicesAdded(servicesAdded)
                .totalCharge(totalCharge)
                .requiresPayment(requiresPayment)
                .message(message)
                .build();
    }

    private String getSeatTypeFromNumber(Booking booking, String seatNumber) {
        List<Seat> seats = seatRepository.findByFlightIdAndSeatNumberForUpdate(booking.getFlight().getFlightId(), seatNumber);
        if (!seats.isEmpty()) {
            Seat seat = seats.stream()
                    .filter(s -> SeatStatus.AVAILABLE.equals(s.getStatus()))
                    .findFirst()
                    .orElse(seats.get(0));
            return seat.getType().toString();
        }
        return null;
    }

    @Override
    @Transactional
    public UpdateBookingTotalResponse updateBookingTotal(Long bookingId, UpdateBookingTotalRequest request) {
        log.info("Starting updateBookingTotal for booking {}: additionalAmount={}, reason={}",
                bookingId, request.getAdditionalAmount(), request.getReason());

        // Find booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        log.info("Found booking {} with current total: {}", bookingId, booking.getTotalAmount());

        // Validate additional amount
        if (request.getAdditionalAmount() == null || request.getAdditionalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Additional amount must be positive");
        }

        // Check if there's already pending additional charges
        if (booking.getPayment() != null && booking.getPayment().getStatus() == PaymentStatus.COMPLETED) {
            BigDecimal paymentAmount = booking.getPayment().getAmount();
            if (booking.getTotalAmount().compareTo(paymentAmount) > 0) {
                log.warn("Booking {} already has pending additional charges. Current total: {}, Payment amount: {}. Cannot add more charges until payment is completed.",
                        bookingId, booking.getTotalAmount(), paymentAmount);
                throw new IllegalStateException("Cannot add additional charges while payment is pending. Please complete the current payment first.");
            }
        }

        // Store old total for response
        BigDecimal oldTotal = booking.getTotalAmount();

        // Update booking total
        BigDecimal newTotal = oldTotal.add(request.getAdditionalAmount());
        booking.setTotalAmount(newTotal);
        booking = bookingRepository.save(booking);

        log.info("Saved booking {} with new total: {} (added {})", bookingId, booking.getTotalAmount(), request.getAdditionalAmount());

        // Note: Do NOT update existing payment amount here. Let the payment service handle it
        // when the user attempts to pay for the additional charges.
        if (booking.getPayment() != null) {
            log.info("Existing payment {} found for booking {} - amount will be updated during payment processing",
                    booking.getPayment().getPaymentId(), bookingId);
        } else {
            log.info("No existing payment found for booking {}", bookingId);
        }

        log.info("Completed updateBookingTotal for booking {}: {} -> {} (reason: {})",
                bookingId, oldTotal, newTotal, request.getReason());

        return UpdateBookingTotalResponse.builder()
                .bookingId(bookingId)
                .bookingCode(booking.getBookingCode())
                .oldTotalAmount(oldTotal)
                .additionalAmount(request.getAdditionalAmount())
                .newTotalAmount(newTotal)
                .reason(request.getReason())
                .message(String.format("Booking total updated successfully. New total: %s VND. Please proceed with payment.", newTotal))
                .build();
    }

    private void populateContactInformation(BookingResponse response, Booking booking) {
        // Ưu tiên thông tin liên hệ từ booking entity
        if (booking.getContactName() != null && !booking.getContactName().isBlank()) {
            response.setContactName(booking.getContactName());
            response.setContactEmail(booking.getContactEmail());
        }
        // Fallback về thông tin người dùng nếu có
        else if (booking.getUserId() != null) {
            response.setContactName(booking.getUserId().getFirstName() + " " + booking.getUserId().getLastName());
            response.setContactEmail(booking.getUserId().getEmail());
        }
    }

    /**
     * Populate available seats information based on segment and passenger check-in status
     * For roundtrip: show seats for appropriate segment based on check-in progression
     */
    private void populateAvailableSeatsInformation(BookingResponse response, Booking booking) {
        List<AvailableSeatsResponse> availableSeatsPerSegment = new ArrayList<>();

        if (booking.getFlightSegments() == null || booking.getFlightSegments().isEmpty()) {
            response.setAvailableSeats(availableSeatsPerSegment);
            return;
        }

        // Sort segments by departure time to ensure correct order
        List<FlightSegment> sortedSegments = booking.getFlightSegments().stream()
                .sorted((s1, s2) -> s1.getFlight().getDepartureTime().compareTo(s2.getFlight().getDepartureTime()))
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();

        for (FlightSegment segment : sortedSegments) {
            Flight flight = segment.getFlight();

            // Check if this segment is eligible for seat selection
            boolean canSelectSeat = canSelectSeatForSegment(booking, segment, now);
            String message = getSegmentSeatSelectionMessage(booking, segment, now);

            // Get available seats for this segment
            List<String> availableSeats = getAvailableSeatsForSegment(segment, booking.getTravelClass());

            availableSeatsPerSegment.add(new AvailableSeatsResponse(
                segment.getSegmentId(),
                segment.getSegmentOrder(),
                flight.getFlightNumber(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getDepartureAirport().getAirportCode(),
                flight.getArrivalAirport().getAirportCode(),
                availableSeats,
                canSelectSeat,
                message
            ));
        }

        response.setAvailableSeats(availableSeatsPerSegment);
        log.info("Populated available seats for {} segments", availableSeatsPerSegment.size());
    }

    /**
     * Determine if seat selection is allowed for a specific segment
     * based on check-in status and business rules
     */
    private boolean canSelectSeatForSegment(Booking booking, FlightSegment segment, LocalDateTime now) {
        // Basic checks
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING) {
            return false;
        }

        // Check if payment is completed
        if (booking.getPayment() == null || booking.getPayment().getStatus() != PaymentStatus.COMPLETED) {
            return false;
        }

        // Check if flight is still in the future
        if (segment.getFlight().getDepartureTime().isBefore(now.plusHours(1))) {
            return false;
        }

        // For roundtrip: check sequential rules
        if (booking.getFlightSegments().size() > 1) {
            return canSelectSeatForRoundtripSegment(booking, segment, now);
        }

        return true;
    }

    /**
     * Check seat selection eligibility for roundtrip segments
     */
    private boolean canSelectSeatForRoundtripSegment(Booking booking, FlightSegment targetSegment, LocalDateTime now) {
        // Sort segments by departure time
        List<FlightSegment> sortedSegments = booking.getFlightSegments().stream()
                .sorted((s1, s2) -> s1.getFlight().getDepartureTime().compareTo(s2.getFlight().getDepartureTime()))
                .collect(Collectors.toList());

        int targetIndex = sortedSegments.indexOf(targetSegment);

        // First segment can always be selected (if not departed)
        if (targetIndex == 0) {
            return !targetSegment.getFlight().getDepartureTime().isBefore(now.plusHours(1));
        }

        // For later segments, check if previous segments have been processed
        for (int i = 0; i < targetIndex; i++) {
            FlightSegment previousSegment = sortedSegments.get(i);

            // If previous segment hasn't departed yet, can't select future segments
            if (previousSegment.getFlight().getDepartureTime().isAfter(now)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get explanation message for seat selection availability
     */
    private String getSegmentSeatSelectionMessage(Booking booking, FlightSegment segment, LocalDateTime now) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return "Booking đã bị hủy";
        }

        if (booking.getPayment() == null || booking.getPayment().getStatus() != PaymentStatus.COMPLETED) {
            return "Cần hoàn tất thanh toán trước khi chọn ghế";
        }

        if (segment.getFlight().getDepartureTime().isBefore(now.plusHours(1))) {
            return "Chuyến bay đã cất cánh hoặc trong thời gian chờ lên máy bay";
        }

        if (booking.getFlightSegments().size() > 1) {
            return getRoundtripSeatSelectionMessage(booking, segment, now);
        }

        return "Có thể chọn ghế";
    }

    /**
     * Get specific message for roundtrip seat selection
     */
    private String getRoundtripSeatSelectionMessage(Booking booking, FlightSegment targetSegment, LocalDateTime now) {
        List<FlightSegment> sortedSegments = booking.getFlightSegments().stream()
                .sorted((s1, s2) -> s1.getFlight().getDepartureTime().compareTo(s2.getFlight().getDepartureTime()))
                .collect(Collectors.toList());

        int targetIndex = sortedSegments.indexOf(targetSegment);
        
        if (targetIndex == 0) {
            return "Có thể chọn ghế cho chuyến đi";
        }

        // Check previous segments
        for (int i = 0; i < targetIndex; i++) {
            FlightSegment previousSegment = sortedSegments.get(i);
            if (previousSegment.getFlight().getDepartureTime().isAfter(now)) {
                return "Chỉ có thể chọn ghế sau khi chuyến bay trước cất cánh";
            }
        }

        return "Có thể chọn ghế cho chuyến về";
    }

    /**
     * Get available seats for a specific segment and travel class
     */
    private List<String> getAvailableSeatsForSegment(FlightSegment segment, TravelClass travelClass) {
        Flight flight = segment.getFlight();
        
        // Get available seats for this flight and travel class
        List<Seat> availableSeats = seatRepository.findAvailableSeatsByFlightIdAndTravelClassId(
                flight.getFlightId(), travelClass.getId());
        
        // Map to seat numbers and sort
        List<String> availableSeatNumbers = availableSeats.stream()
                .map(Seat::getSeatNumber)
                .sorted()
                .collect(Collectors.toList());
        
        log.debug("Found {} available seats for segment {} flight {}", 
                availableSeatNumbers.size(), segment.getSegmentOrder(), flight.getFlightNumber());
        
        return availableSeatNumbers;
    }

    private void refundPointsForCancelledBooking(Booking booking) {
        // Tìm các dealUsage liên quan đến booking này
        List<DealUsage> dealUsages = dealUsageRepository.findAllByBooking(booking);
        if (dealUsages == null || dealUsages.isEmpty()) return;
        for (DealUsage dealUsage : dealUsages) {
            Deal deal = dealUsage.getDeal();
            if (deal != null && Boolean.TRUE.equals(deal.getIsPointsRedemption()) && deal.getPointsRequired() != null && booking.getUserId() != null) {
                User user = booking.getUserId();
                Integer currentPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
                user.setLoyaltyPoints(currentPoints + deal.getPointsRequired());
                userRepository.save(user);
                log.info("Refunded {} points to user {} for cancelled booking {} (deal: {})", deal.getPointsRequired(), user.getId(), booking.getBookingId(), deal.getDealCode());
            }
        }
    }
}
