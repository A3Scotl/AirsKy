package iuh.fit.airsky.repository;

import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @EntityGraph(attributePaths = {"flight","travelClass","passengers","payment","flightSegments"})
    Page<Booking> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"checkIns","checkIns.baggage"})
    List<Booking> findByStatus(BookingStatus bookingStatus);

    @Override
    @EntityGraph(attributePaths = {"userId","flight","travelClass","passengers","passengers.seat","payment","flightSegments","checkIns","checkIns.baggage","checkIns.passenger"})
    Optional<Booking> findById(Long id);

    List<Booking> findByUserId(User user);

    @Query("SELECT b FROM Booking b WHERE b.flight.status = :flightStatus AND b.status = :bookingStatus")
    List<Booking> findBookingsByFlightStatusAndBookingStatus(FlightStatus flightStatus, BookingStatus bookingStatus);
}