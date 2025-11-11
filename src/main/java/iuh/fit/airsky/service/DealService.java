package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.DealRequest;
import iuh.fit.airsky.dto.response.DealResponse;
import iuh.fit.airsky.dto.response.DealUsageResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.model.Booking;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

public interface DealService {
    DealResponse createDeal(DealRequest request);
    DealResponse updateDeal(Long id, DealRequest request);
    Optional<DealResponse> findById(Long id);
    Optional<DealResponse> findByCode(String dealCode);
    PageResponse<DealResponse> findAll(Pageable pageable);
    PageResponse<DealResponse> findActiveDealsByRoute(Long departureAirportId, Long arrivalAirportId, Pageable pageable);
    PageResponse<DealResponse> findActiveDeals(Pageable pageable);
    void delete(Long id);
    void activateDeal(Long id);
    void deactivateDeal(Long id);
    boolean existsByCode(String dealCode);
    
    // Deal usage
    DealUsageResponse applyDeal(String dealCode, Long userId, Booking booking, BigDecimal orderAmount);
    boolean canUserUseDeal(String dealCode, Long userId);
    PageResponse<DealUsageResponse> getDealUsageHistory(Long dealId, Pageable pageable);
    PageResponse<DealUsageResponse> getUserDealUsageHistory(Long userId, Pageable pageable);
    PageResponse<DealResponse> refreshDealStatuses(Pageable pageable);
    PageResponse<DealResponse> getUserEligibleDeals(Long userId, Pageable pageable);
    PageResponse<DealResponse> getGuestEligibleDeals(Pageable pageable);
}
