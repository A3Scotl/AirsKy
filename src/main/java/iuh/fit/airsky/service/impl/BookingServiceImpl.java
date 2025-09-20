package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.PaymentStatus;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.BookingMapper;
import iuh.fit.airsky.model.*;
import iuh.fit.airsky.repository.*;
import iuh.fit.airsky.service.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            BookingMapper bookingMapper,
            UserRepository userRepository,
            FlightRepository flightRepository,
            TravelClassRepository travelClassRepository,
            SeatRepository seatRepository,
            PaymentRepository paymentRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.userRepository = userRepository;
        this.flightRepository = flightRepository;
        this.travelClassRepository = travelClassRepository;
        this.seatRepository = seatRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));

        // Check booking time: must be at least 2 hours before departure
        if (Duration.between(LocalDateTime.now(), flight.getDepartureTime()).toHours() < 2) {
            throw new IllegalArgumentException("You can only book at least 2 hours before departure");
        }

        if (flight.getAvailableSeats() < request.getPassengers().size()) {
            throw new RuntimeException("Not enough available seats");
        }

        Booking booking = bookingMapper.toEntity(request);
        booking.setUserId(userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
        booking.setFlight(flight);
        booking.setTravelClass(travelClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("TravelClass not found")));
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setHoldTime(LocalDateTime.now());

        if (request.getPassengers() != null && !request.getPassengers().isEmpty()) {
            List<Passenger> passengers = mapPassengers(request, booking);
            booking.setPassengers(passengers);
            updateAvailableSeats(flight, passengers.size());
        }

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        Payment payment = createPayment(request, savedBooking);
        savedBooking.setPayment(payment);

        bookingRepository.save(savedBooking);
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

            passenger.setBooking(booking);
            return passenger;
        }).collect(Collectors.toList());
    }


    private void updateAvailableSeats(Flight flight, int bookedSeats) {
        int newAvailableSeats = flight.getAvailableSeats() - bookedSeats;
        flight.setAvailableSeats(newAvailableSeats);
        flightRepository.save(flight);
    }

    private Payment createPayment(BookingRequest request, Booking booking) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(request.getTotalAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.saveAndFlush(payment);
    }

    // Scheduled task to cancel expired bookings
    @Scheduled(fixedRate = 60000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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