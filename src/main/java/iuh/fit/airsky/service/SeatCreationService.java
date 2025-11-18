package iuh.fit.airsky.service;

import iuh.fit.airsky.model.Flight;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeatCreationService {

    private final SeatService seatService;

    @Async("taskExecutor")
    public void createSeatsAsync(Flight flight) {
        log.info("Async task STARTED for flight {} on thread: {}",
                flight.getFlightId(), Thread.currentThread().getName());
        long startTime = System.currentTimeMillis();
        try {
            seatService.createSeatsForFlight(flight);
            long endTime = System.currentTimeMillis();
            log.info("Async seat creation COMPLETED for flight {} in {}ms on thread: {}",
                    flight.getFlightId(), (endTime - startTime), Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("Failed to create seats asynchronously for flight {}: {}",
                    flight.getFlightId(), e.getMessage(), e);
        }
    }
}