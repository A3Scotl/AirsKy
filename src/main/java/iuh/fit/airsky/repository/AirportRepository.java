package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Airport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {

    @EntityGraph(attributePaths = "gates")
    @Query("SELECT a FROM Airport a WHERE a.deleted = false")
    Page<Airport> findAll(Pageable pageable);

    @Query("SELECT a FROM Airport a WHERE a.airportId = :id AND a.deleted = false")
    Optional<Airport> findById(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Airport a SET a.deleted = true, a.deletedAt = :now, a.active = false WHERE a.airportId = :id")
    void softDeleteById(Long id, LocalDateTime now);

    Optional<Airport> findByAirportCode(String airportCode);


}