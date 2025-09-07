package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.response.SeatResponse;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.mapper.SeatMapper;
import iuh.fit.airsky.model.*;
import iuh.fit.airsky.repository.AircraftRepository;
import iuh.fit.airsky.repository.PassengerRepository;
import iuh.fit.airsky.repository.SeatRepository;
import iuh.fit.airsky.repository.TravelClassRepository;
import iuh.fit.airsky.service.SeatService;
import iuh.fit.airsky.util.SeatGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Service
@Slf4j
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;
    private final TravelClassRepository travelClassRepository;
    private final PassengerRepository passengerRepository;
    private final AircraftRepository aircraftRepository;
    // Lock map theo flightId
    private final ConcurrentHashMap<Long, Object> flightLocks = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public List<SeatResponse> getSeatsByFlight(Long flightId) {
        // First, check if seats already exist for this flight
        Flight flight = new Flight();
        flight.setFlightId(flightId);
        List<Seat> seats = seatRepository.findByFlight(flight);

        // Lazy seat generation
        if (seats.isEmpty()) {
            Object lock = flightLocks.computeIfAbsent(flightId, k -> new Object());
            synchronized (lock) {
                // Double-check
                seats = seatRepository.findByFlight(flight);
                if (seats.isEmpty()) {
                    log.info("No seats found for flight {}, generating now...", flightId);

                    // Get the full flight with aircraft relationship
                    Flight fullFlight = aircraftRepository.findFlightWithAircraftById(flightId)
                            .orElseThrow(() -> new RuntimeException("Flight not found with id: " + flightId));

                    // Verify aircraft exists
                    if (fullFlight.getAircraft() == null) {
                        throw new IllegalStateException("No aircraft assigned to flight " + flightId);
                    }

                    // Get travel classes
                    List<TravelClass> travelClasses = travelClassRepository.findAll();

                    // Generate seats
                    seats = SeatGeneratorUtil.generateSeats(fullFlight, fullFlight.getAircraft(), travelClasses);
                    seatRepository.saveAll(seats);
                    log.info("Generated {} seats for flight {}", seats.size(), flightId);
                }
            }
            flightLocks.remove(flightId); // Release lock
        }

        return seats.stream()
                .map(seatMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public void createSeatsForFlight(Flight flight) {
        // Optional: nếu muốn tạo upfront
        Object lock = flightLocks.computeIfAbsent(flight.getFlightId(), k -> new Object());
        synchronized (lock) {
            List<Seat> existingSeats = seatRepository.findByFlight(flight);
            if (!existingSeats.isEmpty()) return;

            List<TravelClass> travelClasses = travelClassRepository.findAll();
            List<Seat> seats = SeatGeneratorUtil.generateSeats(flight, flight.getAircraft(), travelClasses);
            seatRepository.saveAll(seats);
            log.info("Generated {} seats for flight {}", seats.size(), flight.getFlightNumber());
        }
        flightLocks.remove(flight.getFlightId());
    }

    // Ví dụ cho book seat an toàn (nhiều người cùng lúc)
    @Transactional
    public void bookSeats(Long flightId, List<String> seatNumbers, Long passengerId) {
        for (String seatNumber : seatNumbers) {
            Seat seat = seatRepository.findByFlightIdAndSeatNumberForUpdate(flightId, seatNumber)
                    .orElseThrow(() -> new RuntimeException("Seat not found: " + seatNumber));

            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new RuntimeException("Seat already booked: " + seatNumber);
            }

            Passenger passenger = passengerRepository.findById(passengerId)
                    .orElseThrow(() -> new RuntimeException("Passenger not found"));

            seat.setBookedBy(passenger);
            seat.setStatus(SeatStatus.BOOKED);


            seatRepository.save(seat);
        }
    }
}
