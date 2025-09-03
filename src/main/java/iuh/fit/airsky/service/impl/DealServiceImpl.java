package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.DealRequest;
import iuh.fit.airsky.dto.response.DealResponse;
import iuh.fit.airsky.dto.response.DealUsageResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.DealMapper;
import iuh.fit.airsky.mapper.DealUsageMapper;
import iuh.fit.airsky.model.*;
import iuh.fit.airsky.repository.*;
import iuh.fit.airsky.service.DealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DealServiceImpl implements DealService {

    private final DealRepository dealRepository;
    private final DealUsageRepository dealUsageRepository;
    private final DealMapper dealMapper;
    private final DealUsageMapper dealUsageMapper;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final AirportRepository airportRepository;

    @Override
    @Transactional
    public DealResponse createDeal(DealRequest request) {
        log.info("Creating new deal with code: {}", request.getDealCode());
        
        if (dealRepository.existsByDealCode(request.getDealCode())) {
            throw new IllegalArgumentException("Deal với mã này đã tồn tại: " + request.getDealCode());
        }
        
        // Validate dates
        if (request.getValidTo().isBefore(request.getValidFrom())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        
        Deal deal = dealMapper.toEntity(request);
        
        // Set airports if provided
        if (request.getDepartureAirportId() != null) {
            Airport departureAirport = airportRepository.findById(request.getDepartureAirportId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sân bay đi không tồn tại với ID: " + request.getDepartureAirportId()));
            deal.setDepartureAirport(departureAirport);
        }
        
        if (request.getArrivalAirportId() != null) {
            Airport arrivalAirport = airportRepository.findById(request.getArrivalAirportId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sân bay đến không tồn tại với ID: " + request.getArrivalAirportId()));
            deal.setArrivalAirport(arrivalAirport);
        }
        
        Deal savedDeal = dealRepository.save(deal);
        
        log.info("Deal created successfully with ID: {}", savedDeal.getDealId());
        return dealMapper.toResponseDTO(savedDeal);
    }

    @Override
    @Transactional
    public DealResponse updateDeal(Long id, DealRequest request) {
        log.info("Updating deal with ID: {}", id);
        
        Deal existingDeal = dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal không tồn tại với ID: " + id));
        
        // Check if deal code exists for other deals
        if (!existingDeal.getDealCode().equals(request.getDealCode()) && 
            dealRepository.existsByDealCode(request.getDealCode())) {
            throw new IllegalArgumentException("Deal với mã này đã tồn tại: " + request.getDealCode());
        }
        
        // Validate dates
        if (request.getValidTo().isBefore(request.getValidFrom())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        
        existingDeal.setDealCode(request.getDealCode());
        existingDeal.setTitle(request.getTitle());
        existingDeal.setDiscountPercentage(request.getDiscountPercentage());
        existingDeal.setMinimumOrderAmount(request.getMinimumOrderAmount());
        existingDeal.setValidFrom(request.getValidFrom());
        existingDeal.setValidTo(request.getValidTo());
        existingDeal.setDescription(request.getDescription());
        existingDeal.setMaxDiscountAmount(request.getMaxDiscountAmount());
        // Handle both totalUsageLimit and usageLimit fields
        Integer usageLimit = request.getTotalUsageLimit() != null ? request.getTotalUsageLimit() : request.getUsageLimit();
        existingDeal.setTotalUsageLimit(usageLimit);
        existingDeal.setUsagePerUser(request.getUsagePerUser());
        existingDeal.setIsActive(request.getIsActive());

        existingDeal.setThumbnail(request.getThumbnail());
        
        // Update airports
        if (request.getDepartureAirportId() != null) {
            Airport departureAirport = airportRepository.findById(request.getDepartureAirportId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sân bay đi không tồn tại với ID: " + request.getDepartureAirportId()));
            existingDeal.setDepartureAirport(departureAirport);
        } else {
            existingDeal.setDepartureAirport(null);
        }
        
        if (request.getArrivalAirportId() != null) {
            Airport arrivalAirport = airportRepository.findById(request.getArrivalAirportId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sân bay đến không tồn tại với ID: " + request.getArrivalAirportId()));
            existingDeal.setArrivalAirport(arrivalAirport);
        } else {
            existingDeal.setArrivalAirport(null);
        }
        
        Deal updatedDeal = dealRepository.save(existingDeal);
        
        log.info("Deal updated successfully with ID: {}", updatedDeal.getDealId());
        return dealMapper.toResponseDTO(updatedDeal);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DealResponse> findById(Long id) {
        log.debug("Finding deal by ID: {}", id);
        return dealRepository.findById(id)
                .map(dealMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DealResponse> findByCode(String dealCode) {
        log.debug("Finding deal by code: {}", dealCode);
        return dealRepository.findByDealCode(dealCode)
                .map(dealMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealResponse> findAll(Pageable pageable) {
        log.debug("Finding all deals with pagination");
        Page<Deal> dealPage = dealRepository.findAll(pageable);
        return createPageResponse(dealPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealResponse> findActiveDealsByRoute(Long departureAirportId, Long arrivalAirportId, Pageable pageable) {
        log.debug("Finding active deals for route: {} -> {}", departureAirportId, arrivalAirportId);
        
        Airport departureAirport = null;
        Airport arrivalAirport = null;
        
        if (departureAirportId != null) {
            departureAirport = airportRepository.findById(departureAirportId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sân bay đi không tồn tại với ID: " + departureAirportId));
        }
        
        if (arrivalAirportId != null) {
            arrivalAirport = airportRepository.findById(arrivalAirportId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sân bay đến không tồn tại với ID: " + arrivalAirportId));
        }
        
        LocalDateTime now = LocalDateTime.now();
        Page<Deal> dealPage = dealRepository.findAvailableDealsForRoute(now, departureAirport, arrivalAirport, pageable);
        return createPageResponse(dealPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealResponse> findActiveDeals(Pageable pageable) {
        log.debug("Finding all active deals");
        LocalDateTime now = LocalDateTime.now();
        Page<Deal> dealPage = dealRepository.findByIsActiveTrueAndValidFromLessThanEqualAndValidToGreaterThanEqual(
                now, now, pageable);
        return createPageResponse(dealPage);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting deal with ID: {}", id);
        
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal không tồn tại với ID: " + id));
        
        // Check if deal has been used
        long usageCount = dealUsageRepository.countByDeal(deal);
        if (usageCount > 0) {
            throw new IllegalArgumentException("Không thể xóa deal đã được sử dụng");
        }
        
        dealRepository.delete(deal);
        log.info("Deal deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional
    public void activateDeal(Long id) {
        log.info("Activating deal with ID: {}", id);
        
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal không tồn tại với ID: " + id));
        
        deal.setIsActive(true);
        dealRepository.save(deal);
        
        log.info("Deal activated successfully with ID: {}", id);
    }

    @Override
    @Transactional
    public void deactivateDeal(Long id) {
        log.info("Deactivating deal with ID: {}", id);
        
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal không tồn tại với ID: " + id));
        
        deal.setIsActive(false);
        dealRepository.save(deal);
        
        log.info("Deal deactivated successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String dealCode) {
        return dealRepository.existsByDealCode(dealCode);
    }

    @Override
    @Transactional
    public DealUsageResponse applyDeal(String dealCode, Long userId, Long bookingId, BigDecimal orderAmount) {
        log.info("Applying deal {} for user {} on booking {}", dealCode, userId, bookingId);
        
        LocalDateTime now = LocalDateTime.now();
        Deal deal = dealRepository.findValidDealByCode(dealCode, now)
                .orElseThrow(() -> new IllegalArgumentException("Deal không hợp lệ hoặc đã hết hạn: " + dealCode));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking không tồn tại với ID: " + bookingId));
        
        // Check minimum order amount
        if (orderAmount.compareTo(deal.getMinimumOrderAmount()) < 0) {
            throw new IllegalArgumentException("Giá trị đơn hàng tối thiểu: " + deal.getMinimumOrderAmount());
        }
        
        // Check user usage limit
        long userUsageCount = dealUsageRepository.countByDealAndUser(deal, user);
        if (userUsageCount >= deal.getUsagePerUser()) {
            throw new IllegalArgumentException("Bạn đã sử dụng hết số lần cho phép với deal này");
        }
        
        // Calculate discount
        BigDecimal discountAmount = orderAmount.multiply(deal.getDiscountPercentage().divide(BigDecimal.valueOf(100)));
        
        // Apply max discount limit
        if (deal.getMaxDiscountAmount() != null && discountAmount.compareTo(deal.getMaxDiscountAmount()) > 0) {
            discountAmount = deal.getMaxDiscountAmount();
        }
        
        BigDecimal finalAmount = orderAmount.subtract(discountAmount);
        
        // Create usage record
        DealUsage dealUsage = DealUsage.builder()
                .deal(deal)
                .user(user)
                .booking(booking)
                .discountAmount(discountAmount)
                .originalAmount(orderAmount)
                .finalAmount(finalAmount)
                .build();
        
        DealUsage savedUsage = dealUsageRepository.save(dealUsage);
        
        // Update deal usage count
        Integer currentUsedCount = deal.getUsedCount() != null ? deal.getUsedCount() : 0;
        deal.setUsedCount(currentUsedCount + 1);
        dealRepository.save(deal);
        
        log.info("Deal applied successfully. Discount: {}", discountAmount);
        
        return DealUsageResponse.builder()
                .usageId(savedUsage.getUsageId())
                .discountAmount(discountAmount)
                .originalAmount(orderAmount)
                .finalAmount(finalAmount)
                .createdAt(savedUsage.getCreatedAt())
                .dealId(deal.getDealId())
                .dealCode(deal.getDealCode())
                .dealTitle(deal.getTitle())
                .userId(user.getId())
                .userName(user.getFirstName() + " " + user.getLastName())
                .userEmail(user.getEmail())
                .bookingId(booking.getBookingId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserUseDeal(String dealCode, Long userId) {
        log.debug("Checking if user {} can use deal {}", userId, dealCode);
        
        LocalDateTime now = LocalDateTime.now();
        Optional<Deal> dealOpt = dealRepository.findValidDealByCode(dealCode, now);
        
        if (dealOpt.isEmpty()) {
            return false;
        }
        
        Deal deal = dealOpt.get();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));
        
        long userUsageCount = dealUsageRepository.countByDealAndUser(deal, user);
        return userUsageCount < deal.getUsagePerUser();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealUsageResponse> getDealUsageHistory(Long dealId, Pageable pageable) {
        log.debug("Getting usage history for deal ID: {}", dealId);
        
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal không tồn tại với ID: " + dealId));
        
        Page<DealUsage> usagePage = dealUsageRepository.findByDeal(deal, pageable);
        
        return new PageResponse<>(
                usagePage.getContent().stream()
                        .map(dealUsageMapper::toResponseDTO)
                        .collect(Collectors.toList()),
                usagePage.getNumber(),
                usagePage.getSize(),
                usagePage.getTotalElements(),
                usagePage.getTotalPages(),
                usagePage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealUsageResponse> getUserDealUsageHistory(Long userId, Pageable pageable) {
        log.debug("Getting deal usage history for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));
        
        Page<DealUsage> usagePage = dealUsageRepository.findByUser(user, pageable);
        
        return new PageResponse<>(
                usagePage.getContent().stream()
                        .map(dealUsageMapper::toResponseDTO)
                        .collect(Collectors.toList()),
                usagePage.getNumber(),
                usagePage.getSize(),
                usagePage.getTotalElements(),
                usagePage.getTotalPages(),
                usagePage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealResponse> refreshDealStatuses(Pageable pageable) {
        Page<Deal> dealPage = dealRepository.findAll(pageable);
        var dealResponses = dealPage.getContent().stream()
                .map(dealMapper::toResponseDTO)
                .collect(Collectors.toList());
        return new PageResponse<>(
                dealResponses,
                dealPage.getNumber(),
                dealPage.getSize(),
                dealPage.getTotalElements(),
                dealPage.getTotalPages(),
                dealPage.isLast()
        );
    }

    private PageResponse<DealResponse> createPageResponse(Page<Deal> dealPage) {
        return new PageResponse<>(
                dealPage.getContent().stream()
                        .map(dealMapper::toResponseDTO)
                        .collect(Collectors.toList()),
                dealPage.getNumber(),
                dealPage.getSize(),
                dealPage.getTotalElements(),
                dealPage.getTotalPages(),
                dealPage.isLast()
        );
    }
}
