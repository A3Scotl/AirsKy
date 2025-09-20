package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.PassengerRequest;
import iuh.fit.airsky.dto.response.PassengerResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.PassengerSeatResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.PassengerMapper;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Passenger;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.repository.PassengerRepository;
import iuh.fit.airsky.service.PassengerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;
    private final PassengerMapper passengerMapper;
    private final BookingRepository bookingRepository;

    public PassengerServiceImpl(PassengerRepository passengerRepository, PassengerMapper passengerMapper, BookingRepository bookingRepository) {
        this.passengerRepository = passengerRepository;
        this.passengerMapper = passengerMapper;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public PassengerResponse createPassenger(PassengerRequest request) {
        log.info("Creating new passenger for booking ID: {}", request.getBookingId());
        Passenger passenger = passengerMapper.toEntity(request);
        passenger.setBooking(bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + request.getBookingId())));
        Passenger saved = passengerRepository.save(passenger);
        log.info("Passenger created with ID: {}", saved.getPassengerId());
        return passengerMapper.toResponseDTO(saved);
    }

    @Override
    public PassengerResponse updatePassenger(Long id, PassengerRequest request) {
        log.info("Updating passenger with ID: {}", id);
        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id " + id));
        passenger.setFirstName(request.getFirstName());
        passenger.setLastName(request.getLastName());
        passenger.setDateOfBirth(request.getDateOfBirth());
        passenger.setPassportNumber(request.getPassportNumber());
        passenger.setType(request.getType());
        Passenger updated = passengerRepository.save(passenger);
        log.info("Passenger updated with ID: {}", updated.getPassengerId());
        return passengerMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<PassengerResponse> findById(Long id) {
        log.info("Finding passenger by ID: {}", id);
        return passengerRepository.findById(id).map(passengerMapper::toResponseDTO);
    }

    @Override
    public PageResponse<PassengerResponse> findAll(Pageable pageable) {
        log.info("Finding all passengers with pagination: {}", pageable);
        Page<Passenger> page = passengerRepository.findAll(pageable);
        return new PageResponse<>(page.map(passengerMapper::toResponseDTO));
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting passenger with ID: {}", id);
        if (!passengerRepository.existsById(id)) {
            log.warn("Passenger not found for delete: {}", id);
            throw new ResourceNotFoundException("Passenger not found with id " + id);
        }
        passengerRepository.deleteById(id);
        log.info("Passenger deleted: {}", id);
    }
    public List<PassengerSeatResponse> getPassengersWithSeats(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        return booking.getPassengers()
                .stream()
                .map(passengerMapper::toPassengerSeatResponse)
                .toList();
    }

}