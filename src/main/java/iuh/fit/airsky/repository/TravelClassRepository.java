package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.TravelClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TravelClassRepository extends JpaRepository<TravelClass, Long> {
    Page<TravelClass> findAll(Pageable pageable);
}