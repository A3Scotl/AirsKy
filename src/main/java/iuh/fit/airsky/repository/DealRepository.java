package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Airport;
import iuh.fit.airsky.model.Deal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {
    Page<Deal> findAll(Pageable pageable);
    
    Optional<Deal> findByDealCode(String dealCode);
    
    Page<Deal> findByIsActiveTrueAndValidFromLessThanEqualAndValidToGreaterThanEqual(
            LocalDateTime currentTime1, LocalDateTime currentTime2, Pageable pageable);
    
    @Query("SELECT d FROM Deal d WHERE d.isActive = true AND d.validFrom <= :currentTime AND d.validTo >= :currentTime " +
           "AND (d.departureAirport IS NULL OR d.departureAirport = :departureAirport) " +
           "AND (d.arrivalAirport IS NULL OR d.arrivalAirport = :arrivalAirport)")
    Page<Deal> findAvailableDealsForRoute(@Param("currentTime") LocalDateTime currentTime,
                                         @Param("departureAirport") Airport departureAirport,
                                         @Param("arrivalAirport") Airport arrivalAirport,
                                         Pageable pageable);
    
    @Query("SELECT d FROM Deal d WHERE d.dealCode = :dealCode AND d.isActive = true " +
           "AND d.validFrom <= :currentTime AND d.validTo >= :currentTime " +
           "AND d.usedCount < d.totalUsageLimit")
    Optional<Deal> findValidDealByCode(@Param("dealCode") String dealCode, 
                                      @Param("currentTime") LocalDateTime currentTime);
    
    boolean existsByDealCode(String dealCode);
}
