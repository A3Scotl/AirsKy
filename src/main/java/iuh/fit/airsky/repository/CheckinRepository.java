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
public interface CheckinRepository extends JpaRepository<CheckIn, Long> {

    @Query("SELECT c FROM CheckIn c WHERE c.deleted = false")
    Page<CheckIn> findAll(Pageable pageable);

    @Query("SELECT c FROM CheckIn c WHERE c.checkInId = :id AND c.deleted = false")
    Optional<CheckIn> findById(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE CheckIn c SET c.deleted = true, c.deletedAt = :now, c.active = false WHERE c.checkInId = :id")
    void softDeleteById(Long id, LocalDateTime now);
}