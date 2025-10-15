package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.PassengerSeatAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassengerSeatAssignmentRepository extends JpaRepository<PassengerSeatAssignment, Long> {

    @Query("SELECT psa FROM PassengerSeatAssignment psa WHERE psa.seat.id = :seatId AND psa.status = 'OCCUPIED'")
    List<PassengerSeatAssignment> findBySeatAndOccupied(@Param("seatId") Long seatId);

    @Query("SELECT psa FROM PassengerSeatAssignment psa WHERE psa.passenger.id = :passengerId")
    List<PassengerSeatAssignment> findByPassengerId(@Param("passengerId") Long passengerId);

    @Query("SELECT psa FROM PassengerSeatAssignment psa WHERE psa.flightSegment.id = :segmentId")
    List<PassengerSeatAssignment> findByFlightSegmentId(@Param("segmentId") Long segmentId);

    @Query("SELECT psa FROM PassengerSeatAssignment psa WHERE psa.passenger.booking.id = :bookingId")
    List<PassengerSeatAssignment> findByBookingId(@Param("bookingId") Long bookingId);
}