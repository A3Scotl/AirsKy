package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Baggage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BaggageRepository extends JpaRepository<Baggage, Long> {
    Page<Baggage> findAll(Pageable pageable);
}