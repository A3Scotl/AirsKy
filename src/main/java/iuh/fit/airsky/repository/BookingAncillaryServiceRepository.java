package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.BookingAncillaryService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingAncillaryServiceRepository extends JpaRepository<BookingAncillaryService, Long> {
    
    @Query("SELECT bas FROM BookingAncillaryService bas " +
           "JOIN FETCH bas.ancillaryService " +
           "LEFT JOIN FETCH bas.passenger " +
           "WHERE bas.booking.bookingId = :bookingId")
    List<BookingAncillaryService> findByBookingId(@Param("bookingId") Long bookingId);
    
    @Query("SELECT bas FROM BookingAncillaryService bas " +
           "JOIN FETCH bas.ancillaryService " +
           "WHERE bas.booking.bookingId = :bookingId AND bas.passenger.passengerId = :passengerId")
    List<BookingAncillaryService> findByBookingIdAndPassengerId(@Param("bookingId") Long bookingId, 
                                                               @Param("passengerId") Long passengerId);
    
    void deleteByBookingBookingId(Long bookingId);
}