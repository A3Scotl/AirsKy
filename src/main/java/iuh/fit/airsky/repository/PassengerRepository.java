package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Passenger;
import iuh.fit.airsky.model.Seat;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    Page<Passenger> findAll(Pageable pageable);
    // Removed findBySeat method as seat relationship is now through PassengerSeatAssignment
}