package iuh.fit.airsky.service;

import iuh.fit.airsky.model.Airport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AirportService {
    Page<Airport> searchAirports(String keyword, Pageable pageable);
    Page<Airport> getAllAirports(Pageable pageable);
    Airport getAirportById(Long id);
    Airport saveAirport(Airport airport);
    Airport updateAirport(Long id, Airport airport);
    void deleteAirport(Long id);
}