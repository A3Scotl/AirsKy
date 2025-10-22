package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.DealRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.DealResponse;
import iuh.fit.airsky.dto.response.DealUsageResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.service.CloudinaryService;
import iuh.fit.airsky.service.DealService;
import iuh.fit.airsky.service.UserService;
import iuh.fit.airsky.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;
    private final BookingRepository bookingRepository;

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DealResponse>> createDeal(
            @RequestParam("dealCode") String dealCode,
            @RequestParam("title") String title,
            @RequestParam("discountPercentage") BigDecimal discountPercentage,
            @RequestParam("minimumOrderAmount") BigDecimal minimumOrderAmount,
            @RequestParam("validFrom") String validFromStr,
            @RequestParam("validTo") String validToStr,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "maxDiscountAmount", required = false) BigDecimal maxDiscountAmount,
            @RequestParam(value = "departureAirportId", required = false) Long departureAirportId,
            @RequestParam(value = "arrivalAirportId", required = false) Long arrivalAirportId,
            @RequestParam(value = "totalUsageLimit", required = false) Integer totalUsageLimit,
            @RequestParam(value = "usageLimit", required = false) Integer usageLimit,
            @RequestParam(value = "usagePerUser", required = false, defaultValue = "1") Integer usagePerUser,
            @RequestParam(value = "isActive", required = false, defaultValue = "true") Boolean isActive,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "isGuestOnly", required = false, defaultValue = "false") Boolean isGuestOnly,
            @RequestParam(value = "requiredLoyaltyTier", required = false) iuh.fit.airsky.enums.LoyaltyTier requiredLoyaltyTier,
            @RequestParam(value = "isLoyaltyExclusive", required = false, defaultValue = "false") Boolean isLoyaltyExclusive) {

        try {
            // Create DealRequest object manually
            DealRequest request = new DealRequest();
            request.setDealCode(dealCode);
            request.setTitle(title);
            request.setDiscountPercentage(discountPercentage);
            request.setMinimumOrderAmount(minimumOrderAmount);
            
            // Parse dates
            try {
                request.setValidFrom(LocalDateTime.parse(validFromStr));
                request.setValidTo(LocalDateTime.parse(validToStr));
            } catch (Exception e) {
                return ApiResponseUtil.buildResponse(false, "Định dạng ngày không hợp lệ. Sử dụng format: yyyy-MM-ddTHH:mm:ss", null, "/api/v1/deals");
            }
            
            request.setDescription(description);
            request.setMaxDiscountAmount(maxDiscountAmount);
            request.setDepartureAirportId(departureAirportId);
            request.setArrivalAirportId(arrivalAirportId);
            request.setTotalUsageLimit(totalUsageLimit != null ? totalUsageLimit : usageLimit);
            request.setUsagePerUser(usagePerUser);
            request.setIsActive(isActive);
            
            // Upload image if provided
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String imageUrl = cloudinaryService.uploadFile(thumbnail);
                request.setThumbnail(imageUrl);
                log.info("Thumbnail uploaded successfully: {}", imageUrl);
            }
            
            // Set new fields for deal type
            request.setIsGuestOnly(isGuestOnly);
            request.setRequiredLoyaltyTier(requiredLoyaltyTier);
            request.setIsLoyaltyExclusive(isLoyaltyExclusive);

            // Validate required fields
            if (dealCode == null || dealCode.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Mã giảm giá không được để trống", null, "/api/v1/deals");
            }
            if (title == null || title.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Tiêu đề không được để trống", null, "/api/v1/deals");
            }
            if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) <= 0 || discountPercentage.compareTo(new BigDecimal("100")) > 0) {
                return ApiResponseUtil.buildResponse(false, "Phần trăm giảm giá phải từ 0.01 đến 100", null, "/api/v1/deals");
            }
            if (minimumOrderAmount == null || minimumOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
                return ApiResponseUtil.buildResponse(false, "Giá trị đơn hàng tối thiểu phải lớn hơn hoặc bằng 0", null, "/api/v1/deals");
            }
            
            DealResponse response = dealService.createDeal(request);
            return ApiResponseUtil.buildResponse(true, "Tạo mã giảm giá thành công", response, "/api/v1/deals");
            
        } catch (IllegalArgumentException e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/deals");
        } catch (Exception e) {
            log.error("Error creating deal", e);
            return ApiResponseUtil.buildResponse(false, "Có lỗi xảy ra khi tạo mã giảm giá: " + e.getMessage(), null, "/api/v1/deals");
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DealResponse>> updateDeal(
            @PathVariable Long id,
            @RequestParam("dealCode") String dealCode,
            @RequestParam("title") String title,
            @RequestParam("discountPercentage") BigDecimal discountPercentage,
            @RequestParam("minimumOrderAmount") BigDecimal minimumOrderAmount,
            @RequestParam("validFrom") String validFromStr,
            @RequestParam("validTo") String validToStr,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "maxDiscountAmount", required = false) BigDecimal maxDiscountAmount,
            @RequestParam(value = "departureAirportId", required = false) Long departureAirportId,
            @RequestParam(value = "arrivalAirportId", required = false) Long arrivalAirportId,
            @RequestParam(value = "totalUsageLimit", required = false) Integer totalUsageLimit,
            @RequestParam(value = "usageLimit", required = false) Integer usageLimit,
            @RequestParam(value = "usagePerUser", required = false, defaultValue = "1") Integer usagePerUser,
            @RequestParam(value = "isActive", required = false, defaultValue = "true") Boolean isActive,
            @RequestParam(value = "existingThumbnail", required = false) String existingThumbnail,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {

        try {
            // Create DealRequest object manually
            DealRequest request = new DealRequest();
            request.setDealCode(dealCode);
            request.setTitle(title);
            request.setDiscountPercentage(discountPercentage);
            request.setMinimumOrderAmount(minimumOrderAmount);
            
            // Parse dates
            try {
                request.setValidFrom(LocalDateTime.parse(validFromStr));
                request.setValidTo(LocalDateTime.parse(validToStr));
            } catch (Exception e) {
                return ApiResponseUtil.buildResponse(false, "Định dạng ngày không hợp lệ. Sử dụng format: yyyy-MM-ddTHH:mm:ss", null, "/api/v1/deals/" + id);
            }
            
            request.setDescription(description);
            request.setMaxDiscountAmount(maxDiscountAmount);
            request.setDepartureAirportId(departureAirportId);
            request.setArrivalAirportId(arrivalAirportId);
            request.setTotalUsageLimit(totalUsageLimit != null ? totalUsageLimit : usageLimit);
            request.setUsagePerUser(usagePerUser);
            request.setIsActive(isActive);
            
            // Handle image upload
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String imageUrl = cloudinaryService.uploadFile(thumbnail);
                request.setThumbnail(imageUrl);
                log.info("Thumbnail uploaded successfully: {}", imageUrl);
            } else if (existingThumbnail != null && !existingThumbnail.trim().isEmpty()) {
                request.setThumbnail(existingThumbnail);
            }
            
            // Validate required fields
            if (dealCode == null || dealCode.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Mã giảm giá không được để trống", null, "/api/v1/deals/" + id);
            }
            if (title == null || title.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Tiêu đề không được để trống", null, "/api/v1/deals/" + id);
            }
            if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) <= 0 || discountPercentage.compareTo(new BigDecimal("100")) > 0) {
                return ApiResponseUtil.buildResponse(false, "Phần trăm giảm giá phải từ 0.01 đến 100", null, "/api/v1/deals/" + id);
            }
            if (minimumOrderAmount == null || minimumOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
                return ApiResponseUtil.buildResponse(false, "Giá trị đơn hàng tối thiểu phải lớn hơn hoặc bằng 0", null, "/api/v1/deals/" + id);
            }
            
            DealResponse response = dealService.updateDeal(id, request);
            return ApiResponseUtil.buildResponse(true, "Cập nhật mã giảm giá thành công", response, "/api/v1/deals/" + id);
            
        } catch (IllegalArgumentException e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/deals/" + id);
        } catch (Exception e) {
            log.error("Error updating deal", e);
            return ApiResponseUtil.buildResponse(false, "Có lỗi xảy ra khi cập nhật mã giảm giá: " + e.getMessage(), null, "/api/v1/deals/" + id);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DealResponse>> getDealById(@PathVariable Long id) {
        return dealService.findById(id)
                .map(deal -> ApiResponseUtil.buildResponse(true, "Lấy thông tin mã giảm giá thành công", deal, "/api/v1/deals/" + id))
                .orElse(ApiResponseUtil.buildResponse(false, "Không tìm thấy mã giảm giá", null, "/api/v1/deals/" + id));
    }

    @GetMapping("/code/{dealCode}")
    public ResponseEntity<ApiResponse<DealResponse>> getDealByCode(@PathVariable String dealCode) {
        return dealService.findByCode(dealCode)
                .map(deal -> ApiResponseUtil.buildResponse(true, "Lấy thông tin mã giảm giá thành công", deal, "/api/v1/deals/code/" + dealCode))
                .orElse(ApiResponseUtil.buildResponse(false, "Không tìm thấy mã giảm giá", null, "/api/v1/deals/code/" + dealCode));
    }

    @GetMapping
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<DealResponse>>> getAllDeals(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<DealResponse> response = dealService.findAll(pageable);
        return ApiResponseUtil.buildResponse(true, "Lấy danh sách mã giảm giá thành công", response, "/api/v1/deals");
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PageResponse<DealResponse>>> getActiveDeals(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<DealResponse> response = dealService.findActiveDeals(pageable);
        return ApiResponseUtil.buildResponse(true, "Lấy danh sách mã giảm giá đang hoạt động thành công", response, "/api/v1/deals/active");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDeal(@PathVariable Long id) {
        try {
            dealService.delete(id);
            return ApiResponseUtil.buildResponse(true, "Xóa mã giảm giá thành công", null, "/api/v1/deals/" + id);
        } catch (IllegalArgumentException e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/deals/" + id);
        }
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateDeal(@PathVariable Long id) {
        try {
            dealService.activateDeal(id);
            return ApiResponseUtil.buildResponse(true, "Kích hoạt mã giảm giá thành công", null, "/api/v1/deals/" + id + "/activate");
        } catch (Exception e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/deals/" + id + "/activate");
        }
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateDeal(@PathVariable Long id) {
        try {
            dealService.deactivateDeal(id);
            return ApiResponseUtil.buildResponse(true, "Vô hiệu hóa mã giảm giá thành công", null, "/api/v1/deals/" + id + "/deactivate");
        } catch (Exception e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/deals/" + id + "/deactivate");
        }
    }

    @GetMapping("/check-code/{dealCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkCodeExists(@PathVariable String dealCode) {
        boolean exists = dealService.existsByCode(dealCode);
        return ApiResponseUtil.buildResponse(true, "Kiểm tra mã giảm giá thành công", exists, "/api/v1/deals/check-code/" + dealCode);
    }

    @PostMapping("/apply")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<DealUsageResponse>> applyDeal(
            @RequestParam String dealCode,
            @RequestParam Long bookingId,
            @RequestParam BigDecimal orderAmount) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName(); // Lấy email từ JWT
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Find booking by ID
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
            
            DealUsageResponse response = dealService.applyDeal(dealCode, user.getId(), booking, orderAmount);
            return ApiResponseUtil.buildResponse(true, "Áp dụng mã giảm giá thành công", response, "/api/v1/deals/apply");
        } catch (IllegalArgumentException e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/deals/apply");
        }
    }

    @GetMapping("/can-use/{dealCode}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Boolean>> canUserUseDeal(@PathVariable String dealCode) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName(); // Lấy email từ JWT
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            boolean canUse = dealService.canUserUseDeal(dealCode, user.getId());
            return ApiResponseUtil.buildResponse(true, "Kiểm tra khả năng sử dụng mã giảm giá thành công", canUse, "/api/v1/deals/can-use/" + dealCode);
        } catch (Exception e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/deals/can-use/" + dealCode);
        }
    }



    @GetMapping("/my-usage-history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<DealUsageResponse>>> getMyDealUsageHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // Lấy email từ JWT
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        PageResponse<DealUsageResponse> response = dealService.getUserDealUsageHistory(user.getId(), pageable);
        return ApiResponseUtil.buildResponse(true, "Lấy lịch sử sử dụng mã giảm giá của tôi thành công", response, "/api/v1/deals/my-usage-history");
    }

    @GetMapping("/refresh-status")
    public ResponseEntity<ApiResponse<PageResponse<DealResponse>>> refreshDealStatuses(@PageableDefault(size = 20) Pageable pageable) {
        PageResponse<DealResponse> response = dealService.refreshDealStatuses(pageable);
        return ApiResponseUtil.buildResponse(true, "Lấy danh sách mã giảm giá với trạng thái mới nhất thành công", response, "/api/v1/deals/refresh-status");
    }
}
