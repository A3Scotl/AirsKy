package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.DealRequest;
import iuh.fit.airsky.dto.response.DealResponse;
import iuh.fit.airsky.dto.response.DealUsageResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.LoyaltyTier;
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
import org.springframework.transaction.annotation.Propagation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    @PersistenceContext
    private EntityManager entityManager;

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
    public DealUsageResponse applyDeal(String dealCode, Long userId, Booking booking, BigDecimal orderAmount) {
        log.info("Starting applyDeal: dealCode='{}' (isNull={}, length={}), userId={}, bookingId={}, orderAmount={}",
                dealCode, dealCode == null, dealCode != null ? dealCode.length() : 0, userId, booking.getBookingId(), orderAmount);
        
        LocalDateTime now = LocalDateTime.now();
        log.info("Looking for deal with code: {} at time: {}", dealCode, now);
        Deal deal = dealRepository.findValidDealByCode(dealCode, now)
                .orElseThrow(() -> new IllegalArgumentException("Deal không hợp lệ hoặc đã hết hạn: " + dealCode));
        log.info("Found valid deal: id={}, code={}, title={}", deal.getDealId(), deal.getDealCode(), deal.getTitle());
        
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));
        } else {
            log.info("Applying deal for guest user (no user account)");
        }
        
        // Booking object is passed directly to avoid transaction isolation issues
        
        // Check if this deal has already been applied to this booking
        if (dealUsageRepository.existsByDealAndBooking(deal, booking)) {
            throw new IllegalArgumentException("Deal này đã được áp dụng cho booking này");
        }
        
        // Check minimum order amount
        if (orderAmount.compareTo(deal.getMinimumOrderAmount()) < 0) {
            throw new IllegalArgumentException("Giá trị đơn hàng tối thiểu: " + deal.getMinimumOrderAmount());
        }
        
        // Check total usage limit (for authenticated users only, guests don't count toward total limit)
        if (user != null && deal.getTotalUsageLimit() != null && deal.getTotalUsageLimit() > 0) {
            Integer currentUsedCount = deal.getUsedCount() != null ? deal.getUsedCount() : 0;
            log.info("Checking total usage limit: current={}, limit={}", currentUsedCount, deal.getTotalUsageLimit());
            if (currentUsedCount >= deal.getTotalUsageLimit()) {
                throw new IllegalArgumentException("Deal đã hết lượt sử dụng");
            }
        }
        
        // Check user usage limit (only for authenticated users)
        if (user != null && deal.getUsagePerUser() != null) {
            long userUsageCount = dealUsageRepository.countByDealAndUser(deal, user);
            if (userUsageCount >= deal.getUsagePerUser()) {
                throw new IllegalArgumentException("Bạn đã sử dụng hết số lần cho phép với deal này");
            }
        }
        
        // Check route-specific deal (if deal has specific departure/arrival airports)
        if (deal.getDepartureAirport() != null && deal.getArrivalAirport() != null) {
            boolean routeMatches = booking.getFlightSegments().stream()
                    .anyMatch(segment -> 
                        segment.getFlight().getDepartureAirport().getAirportId().equals(deal.getDepartureAirport().getAirportId()) &&
                        segment.getFlight().getArrivalAirport().getAirportId().equals(deal.getArrivalAirport().getAirportId())
                    );
            
            if (!routeMatches) {
                throw new IllegalArgumentException("Deal này chỉ áp dụng cho tuyến bay từ " + 
                    deal.getDepartureAirport().getAirportCode() + " đến " + 
                    deal.getArrivalAirport().getAirportCode());
            }
            log.info("Route-specific deal validated for booking {}", booking.getBookingId());
        } else {
            log.info("Deal applies to all routes (no route restrictions)");
        }
        
        // Calculate discount
        log.info("Calculating discount - Order Amount: {}, Discount Percentage: {}%", orderAmount, deal.getDiscountPercentage());
        BigDecimal discountAmount = orderAmount.multiply(deal.getDiscountPercentage().divide(BigDecimal.valueOf(100)));
        log.info("Calculated discount amount: {}", discountAmount);
        
        // Apply max discount limit
        if (deal.getMaxDiscountAmount() != null && discountAmount.compareTo(deal.getMaxDiscountAmount()) > 0) {
            log.info("Applying max discount limit: {} -> {}", discountAmount, deal.getMaxDiscountAmount());
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
        log.info("DealUsage saved with ID: {}, user: {}, booking: {}", 
                savedUsage.getUsageId(), savedUsage.getUser() != null ? savedUsage.getUser().getId() : "null", savedUsage.getBooking().getBookingId());
        
        // Note: Removed manual flush - let @Transactional handle it
        
        // Update deal usage count (for both authenticated and guest users)
        Integer currentUsedCount = deal.getUsedCount() != null ? deal.getUsedCount() : 0;
        deal.setUsedCount(currentUsedCount + 1);
        dealRepository.save(deal);
        log.info("Deal usage count updated to: {} for deal: {} (user: {})", 
                deal.getUsedCount(), deal.getDealCode(), 
                user != null ? "authenticated" : "guest");
        
        // Note: Removed manual flush - let @Transactional handle it
        
        log.info("Deal applied successfully. DealUsage ID: {}, discount: {}", savedUsage.getUsageId(), discountAmount);
        
        // Create response
        DealUsageResponse.DealUsageResponseBuilder responseBuilder = DealUsageResponse.builder()
                .usageId(savedUsage.getUsageId())
                .discountAmount(discountAmount)
                .originalAmount(orderAmount)
                .finalAmount(finalAmount)
                .createdAt(savedUsage.getCreatedAt())
                .dealId(deal.getDealId())
                .dealCode(deal.getDealCode())
                .dealTitle(deal.getTitle())
                .bookingId(booking.getBookingId())
                .bookingCode(booking.getBookingCode());
        
        // Set user info only if user is not null (authenticated user)
        if (user != null) {
            responseBuilder
                .userId(user.getId())
                .userName(user.getFirstName() + " " + user.getLastName())
                .userEmail(user.getEmail());
        } else {
            responseBuilder
                .userId(null)
                .userName("Guest User")
                .userEmail(null);
        }
        
        DealUsageResponse response = responseBuilder.build();
        
        return response;
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
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));
        }

        // Check eligibility first
        try {
            validateDealEligibility(deal, user);
        } catch (IllegalArgumentException e) {
            log.debug("User {} is not eligible for deal {}: {}", userId, dealCode, e.getMessage());
            return false;
        }

        // For guest users, no usage limit check needed
        if (user == null) {
            return true;
        }

        long userUsageCount = dealUsageRepository.countByDealAndUser(deal, user);
        // Handle null usagePerUser (unlimited usage per user)
        return deal.getUsagePerUser() == null || userUsageCount < deal.getUsagePerUser();
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

    private void validateDealEligibility(Deal deal, User user) {
        log.info("Validating deal eligibility: deal={}, userId={}", deal.getDealCode(), user != null ? user.getId() : "guest");

        // Case 1: Deal chỉ dành cho guest users
        if (Boolean.TRUE.equals(deal.getIsGuestOnly())) {
            if (user != null) {
                throw new IllegalArgumentException("Deal này chỉ dành cho khách hàng chưa đăng ký tài khoản");
            }
            log.info("Deal {} is guest-only and user is guest - eligible", deal.getDealCode());
            return;
        }

        // Case 2: Deal chỉ dành cho loyalty members (không cho guest)
        if (Boolean.TRUE.equals(deal.getIsLoyaltyExclusive())) {
            if (user == null) {
                throw new IllegalArgumentException("Deal này chỉ dành cho thành viên loyalty. Vui lòng đăng nhập để sử dụng");
            }
            log.info("Deal {} is loyalty-exclusive and user is authenticated - eligible", deal.getDealCode());
            return;
        }

        // Case 3: Deal yêu cầu hạng thành viên tối thiểu
        if (deal.getRequiredLoyaltyTier() != null) {
            if (user == null) {
                throw new IllegalArgumentException("Deal này yêu cầu hạng thành viên " + deal.getRequiredLoyaltyTier() +
                    ". Vui lòng đăng nhập để sử dụng");
            }

            LoyaltyTier userTier = user.getLoyaltyTier() != null ? user.getLoyaltyTier() : LoyaltyTier.STANDARD;
            if (userTier.ordinal() < deal.getRequiredLoyaltyTier().ordinal()) {
                throw new IllegalArgumentException("Deal này yêu cầu hạng thành viên " + deal.getRequiredLoyaltyTier() +
                    " trở lên. Hạng hiện tại của bạn: " + userTier);
            }
            log.info("Deal {} requires tier {} and user has tier {} - eligible", deal.getDealCode(),
                deal.getRequiredLoyaltyTier(), userTier);
            return;
        }

        // Case 4: Deal dành cho tất cả (authenticated users và guest)
        log.info("Deal {} is available to all users - eligible", deal.getDealCode());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealResponse> getUserEligibleDeals(Long userId, Pageable pageable) {
        log.info("Getting eligible deals for authenticated user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));

        LocalDateTime now = LocalDateTime.now();
        List<Deal> allActiveDeals = dealRepository.findActiveDealsList(now);

        // Filter deals that user is eligible for
        List<Deal> eligibleDeals = allActiveDeals.stream()
                .filter(deal -> {
                    try {
                        validateDealEligibility(deal, user);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        // Apply pagination manually since we're filtering
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), eligibleDeals.size());
        List<Deal> pageContent = eligibleDeals.subList(start, end);

        return new PageResponse<>(
                pageContent.stream()
                        .map(dealMapper::toResponseDTO)
                        .collect(Collectors.toList()),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                eligibleDeals.size(),
                (int) Math.ceil((double) eligibleDeals.size() / pageable.getPageSize()),
                end >= eligibleDeals.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealResponse> getGuestEligibleDeals(Pageable pageable) {
        log.info("Getting eligible deals for guest users");

        LocalDateTime now = LocalDateTime.now();
        List<Deal> allActiveDeals = dealRepository.findActiveDealsList(now);

        // Filter deals that guest users are eligible for
        List<Deal> eligibleDeals = allActiveDeals.stream()
                .filter(deal -> {
                    try {
                        validateDealEligibility(deal, null); // null user = guest
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        // Apply pagination manually since we're filtering
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), eligibleDeals.size());
        List<Deal> pageContent = eligibleDeals.subList(start, end);

        return new PageResponse<>(
                pageContent.stream()
                        .map(dealMapper::toResponseDTO)
                        .collect(Collectors.toList()),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                eligibleDeals.size(),
                (int) Math.ceil((double) eligibleDeals.size() / pageable.getPageSize()),
                end >= eligibleDeals.size()
        );
    }
}
