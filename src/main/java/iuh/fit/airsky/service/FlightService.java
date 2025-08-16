package iuh.fit.airsky.service;

import iuh.fit.airsky.model.Flight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface FlightService {
    Page<Flight> searchFlights(Long departureAirportId, Long arrivalAirportId, LocalDate departureDate, Pageable pageable);
    Page<Flight> getAllFlights(Pageable pageable);
    Flight getFlightById(Long id);
    Flight saveFlight(Flight flight);
    Flight updateFlight(Long id, Flight flight);
    void deleteFlight(Long id);
}
