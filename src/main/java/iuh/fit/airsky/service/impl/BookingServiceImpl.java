package iuh.fit.airsky.service.impl;
import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.BaggageType;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.PaymentStatus;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.BookingMapper;
import iuh.fit.airsky.model.*;
import iuh.fit.airsky.repository.*;
import iuh.fit.airsky.service.BookingService;
import iuh.fit.airsky.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
    private final EmailService emailService;

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
            EmailService emailService
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
        this.emailService = emailService;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));

        // Kiểm tra thời gian đặt vé: phải trước giờ khởi hành ít nhất 2 tiếng
        if (Duration.between(LocalDateTime.now(), flight.getDepartureTime()).toHours() < 2) {
            throw new IllegalArgumentException("Bạn chỉ có thể đặt vé trước giờ khởi hành ít nhất 2 tiếng");
        }

        // Kiểm tra số lượng ghế trống
        if (flight.getAvailableSeats() < request.getPassengers().size()) {
            throw new IllegalStateException("Không đủ ghế trống");
        }

        Booking booking = bookingMapper.toEntity(request);
        booking.setUserId(userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng")));
        booking.setFlight(flight);
        booking.setTravelClass(travelClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy TravelClass")));
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setHoldTime(LocalDateTime.now());

        List<Passenger> passengers = mapPassengers(request, booking);
        booking.getPassengers().addAll(passengers);

        // Cập nhật số lượng ghế trống và lưu chuyến bay
        updateAvailableSeats(flight, passengers.size());


        Booking savedBooking = bookingRepository.save(booking);

        // Tạo thanh toán
        Payment payment = createPayment(request, booking);
        booking.setPayment(payment);
        bookingRepository.save(savedBooking);
        // Lưu booking


        // Tạo CheckIn cho mỗi hành khách
        passengers.forEach(passenger -> createCheckIn(savedBooking, passenger, request));
        String email = savedBooking.getUserId().getEmail(); // lấy email từ user
        String subject = "Xác nhận đặt vé thành công";
        String body = "<h3>Xin chào " + savedBooking.getUserId().getLastName() + "</h3>"
                + "<p>Bạn đã đặt vé thành công cho chuyến bay: " + flight.getFlightNumber() + "</p>"
                + "<p>Mã đặt vé: " + savedBooking.getBookingCode() + "</p>"
                + "<p>Khởi hành: " + flight.getDepartureTime() + "</p>"
                + "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.</p>";

        emailService.sendEmail(email, subject, body);
        return bookingMapper.toResponseDTO(savedBooking);
    }

    private List<Passenger> mapPassengers(BookingRequest request, Booking booking) {
        return request.getPassengers().stream().map(p -> {
            Passenger passenger = new Passenger();
            passenger.setFirstName(p.getFirstName());
            passenger.setLastName(p.getLastName());
            passenger.setDateOfBirth(p.getDateOfBirth());
            passenger.setPassportNumber(p.getPassportNumber());
            passenger.setType(p.getType());

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


    private void updateAvailableSeats(Flight flight, int bookedSeats) {
        int newAvailableSeats = flight.getAvailableSeats() - bookedSeats;
        flight.setAvailableSeats(newAvailableSeats);
        flightRepository.save(flight);
    }

    private Payment createPayment(BookingRequest request, Booking booking) {
        BigDecimal total = request.getTotalAmount();

        for (Passenger passenger : booking.getPassengers()) {
            if (passenger.getTempBaggagePackage() != null) {
                total = total.add(passenger.getTempBaggagePackage().getPrice());
            }
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(total);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.saveAndFlush(payment);
    }


    private void createCheckIn(Booking booking, Passenger passenger, BookingRequest request) {
        Baggage baggage = null;

        if (passenger.getTempBaggagePackage() != null) {
            baggage = Baggage.builder()
                    .type(BaggageType.CHECK_IN)
                    .purchasedPackage(passenger.getTempBaggagePackage())
                    .packagePrice(passenger.getTempBaggagePackage().getPrice())
                    .build();
            baggage = baggageRepository.save(baggage);
        }

        CheckIn checkIn = CheckIn.builder()
                .booking(booking)
                .passenger(passenger)
                .baggage(baggage) // có thể null nếu không chọn
                .seatNumber(passenger.getSeat().getSeatNumber())
                .checkedAt(booking.getFlight().getDepartureTime())
                .build();
        checkIn = checkinRepository.save(checkIn);

        if (baggage != null) {
            baggage.setCheckIn(checkIn);
            baggageRepository.save(baggage);
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

    @Override
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        log.info("Updating booking with ID: {}", id);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
        booking.setBookingDate(request.getBookingDate());
        booking.setTotalAmount(request.getTotalAmount());
        booking.setStatus(request.getStatus());
        Booking updated = bookingRepository.save(booking);
        log.info("Booking updated with ID: {}", updated.getBookingId());
        return bookingMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<BookingResponse> findById(Long id) {
        log.info("Finding booking by ID: {}", id);
        return bookingRepository.findById(id).map(bookingMapper::toResponseDTO);
    }

    @Override
    public PageResponse<BookingResponse> findAll(Pageable pageable) {
        log.info("Finding all bookings with pagination: {}", pageable);
        Page<Booking> page = bookingRepository.findAll(pageable);
        return new PageResponse<>(page.map(bookingMapper::toResponseDTO));
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting booking with ID: {}", id);
        if (!bookingRepository.existsById(id)) {
            log.warn("Booking not found for delete: {}", id);
            throw new ResourceNotFoundException("Booking not found with id " + id);
        }
        bookingRepository.deleteById(id);
        log.info("Booking deleted: {}", id);
    }
}