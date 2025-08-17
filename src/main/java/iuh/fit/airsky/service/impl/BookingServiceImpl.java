package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.BookingMapper;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Passenger;
import iuh.fit.airsky.model.Seat;
import iuh.fit.airsky.repository.*;
import iuh.fit.airsky.service.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public BookingServiceImpl(BookingRepository bookingRepository, BookingMapper bookingMapper, UserRepository userRepository, FlightRepository flightRepository, TravelClassRepository travelClassRepository, SeatRepository seatRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.userRepository = userRepository;
        this.flightRepository = flightRepository;
        this.travelClassRepository = travelClassRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Booking booking = bookingMapper.toEntity(request);

        // Set user, flight, class
        booking.setUserId(userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
        booking.setFlight(flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found")));
        booking.setTravelClass(travelClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("TravelClass not found")));

        // Map passengers
        if (request.getPassengers() != null && !request.getPassengers().isEmpty()) {
            List<Passenger> passengers = request.getPassengers().stream().map(p -> {
                Passenger passenger = new Passenger();
                passenger.setFirstName(p.getFirstName());
                passenger.setLastName(p.getLastName());
                passenger.setDateOfBirth(p.getDateOfBirth());
                passenger.setPassportNumber(p.getPassportNumber());
                passenger.setType(p.getType());

                if (p.getSeatId() != null) {
                    Seat seat = seatRepository.findById(p.getSeatId())
                            .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
                    seat.setStatus(SeatStatus.BOOKED);
                    seat.setBookedBy(passenger); // seat now references passenger
                    passenger.setSeat(seat);
                }

                passenger.setBooking(booking);
                return passenger;
            }).toList();
            booking.setPassengers(passengers);
        }

        Booking savedBooking = bookingRepository.saveAndFlush(booking); // flush ngay
        return bookingMapper.toResponseDTO(savedBooking);
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