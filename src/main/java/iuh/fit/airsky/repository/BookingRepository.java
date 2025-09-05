package iuh.fit.airsky.repository;

import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @EntityGraph(attributePaths = {"flight","travelClass","passengers","payment"})
    Page<Booking> findAll(Pageable pageable);

    List<Booking> findByStatus(BookingStatus bookingStatus);

    List<Booking> findByUserId(User userId);
}