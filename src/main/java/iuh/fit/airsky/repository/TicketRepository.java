package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.CheckIn;
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
public interface TicketRepository extends JpaRepository<CheckIn, Long> {

    @Query("SELECT t FROM CheckIn t WHERE t.deleted = false")
    Page<CheckIn> findAll(Pageable pageable);

    @Query("SELECT t FROM CheckIn t WHERE t.ticketId = :id AND t.deleted = false")
    Optional<CheckIn> findById(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE CheckIn t SET t.deleted = true, t.deletedAt = :now, t.active = false WHERE t.ticketId = :id")
    void softDeleteById(Long id, LocalDateTime now);
}