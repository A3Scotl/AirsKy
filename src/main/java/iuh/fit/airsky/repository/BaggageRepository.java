package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Baggage;
import iuh.fit.airsky.model.Passenger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@Repository
public interface BaggageRepository extends JpaRepository<Baggage, Long> {
    Page<Baggage> findAll(Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM Baggage b WHERE b.checkIn.passenger = :passenger")
    void deleteByPassenger(Passenger passenger);
}