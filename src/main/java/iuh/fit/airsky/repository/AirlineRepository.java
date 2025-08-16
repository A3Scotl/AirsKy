package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Airline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, Long> {

    @Query("SELECT a FROM Airline a WHERE a.deleted = false")
    Page<Airline> findAll(Pageable pageable);

    @Query("SELECT a FROM Airline a WHERE a.airlineId = :id AND a.deleted = false")
    Optional<Airline> findById(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Airline a SET a.deleted = true, a.deletedAt = :now, a.active = false WHERE a.airlineId = :id")
    void softDeleteById(Long id, LocalDateTime now);
}