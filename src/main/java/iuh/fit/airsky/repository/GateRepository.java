package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Gate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GateRepository extends JpaRepository<Gate, Long> {

    @Query("SELECT g FROM Gate g WHERE g.deleted = false")
    Page<Gate> findAll(Pageable pageable);

    @Query("SELECT g FROM Gate g WHERE g.gateId = :id AND g.deleted = false")
    Optional<Gate> findById(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Gate g SET g.deleted = true, g.deletedAt = :now, g.active = false WHERE g.gateId = :id")
    void softDeleteById(Long id, LocalDateTime now);

    List<Gate> findAllByAirport_AirportId(Long airportAirportId);
}