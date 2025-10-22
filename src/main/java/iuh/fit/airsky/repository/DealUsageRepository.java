package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Deal;
import iuh.fit.airsky.model.DealUsage;
import iuh.fit.airsky.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealUsageRepository extends JpaRepository<DealUsage, Long> {
    Page<DealUsage> findByUser(User user, Pageable pageable);
    
    Page<DealUsage> findByDeal(Deal deal, Pageable pageable);
    
    long countByDealAndUser(Deal deal, User user);
    
    long countByDeal(Deal deal);
    
    Optional<DealUsage> findByBooking(Booking booking);
    
    List<DealUsage> findAllByBooking(Booking booking);
    
    boolean existsByDealAndBooking(Deal deal, Booking booking);
}
