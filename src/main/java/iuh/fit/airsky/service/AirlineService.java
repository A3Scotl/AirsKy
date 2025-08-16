package iuh.fit.airsky.service;

import iuh.fit.airsky.model.Airline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AirlineService {
    Page<Airline> searchAirlines(String keyword, Pageable pageable);
    Page<Airline> getAllAirlines(Pageable pageable);
    Airline getAirlineById(Long id);
    Airline saveAirline(Airline airline);
    Airline updateAirline(Long id, Airline airline);
    void deleteAirline(Long id);
}
