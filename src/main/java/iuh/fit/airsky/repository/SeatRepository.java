package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Seat;
import iuh.fit.airsky.model.Flight;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    @Query("SELECT s FROM Seat s WHERE s.flight.flightId = :flightId")
    List<Seat> findByFlightId(@Param("flightId") Long flightId);

    List<Seat> findByFlight(Flight flight);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.flight.flightId = :flightId AND s.seatNumber = :seatNumber")
    Optional<Seat> findByFlightIdAndSeatNumberForUpdate(@Param("flightId") Long flightId,
                                                        @Param("seatNumber") String seatNumber);
    @Query("SELECT s FROM Seat s WHERE s.flight.flightId = :flightId AND s.travelClass.id = :travelClassId")
    List<Seat> findByFlightIdAndTravelClassId(@Param("flightId") Long flightId,
                                              @Param("travelClassId") Long travelClassId);
    Optional<Object> findBySeatNumber(String seatNumber);
}
