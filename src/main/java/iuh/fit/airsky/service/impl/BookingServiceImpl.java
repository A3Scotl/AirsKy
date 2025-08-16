package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.BookingMapper;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.repository.FlightRepository;
import iuh.fit.airsky.repository.TravelClassRepository;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final UserRepository userRepository;
    private final FlightRepository flightRepository;
    private final TravelClassRepository travelClassRepository;

    public BookingServiceImpl(BookingRepository bookingRepository, BookingMapper bookingMapper, UserRepository userRepository, FlightRepository flightRepository, TravelClassRepository travelClassRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.userRepository = userRepository;
        this.flightRepository = flightRepository;
        this.travelClassRepository = travelClassRepository;
    }

    @Override
    public BookingResponse createBooking(BookingRequest request) {
        log.info("Creating new booking for user ID: {}", request.getUserId());
        Booking booking = bookingMapper.toEntity(request);
        booking.setUserId(userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + request.getUserId())));
        booking.setFlight(flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id " + request.getFlightId())));
        booking.setTravelClass(travelClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("TravelClass not found with id " + request.getClassId())));
        Booking saved = bookingRepository.save(booking);
        log.info("Booking created with ID: {}", saved.getBookingId());
        return bookingMapper.toResponseDTO(saved);
    }

    @Override
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        log.info("Updating booking with ID: {}", id);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
        booking.setBookingDate(request.getBookingDate());
        booking.setTotalAmount(request.getTotalAmount());
        booking.setStatus(request.getStatus());
        booking.setAdultCount(request.getAdultCount());
        booking.setChildCount(request.getChildCount());
        booking.setInfantCount(request.getInfantCount());
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