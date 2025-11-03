package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.UserRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.UserResponse;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.CloudinaryService;
import iuh.fit.airsky.service.UserService;
import iuh.fit.airsky.util.ApiResponseUtil;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("avatar"); // Ignore avatar file binding to UserRequest
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<UserResponse> users = userService.findAll(pageable);
        return ApiResponseUtil.buildResponse(true, "Users retrieved successfully", users, "/api/users");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        Optional<UserResponse> user = userService.findById(id);
        if (user.isPresent()) {
            return ApiResponseUtil.buildResponse(true, "User retrieved successfully", user.get(), "/api/users/" + id);
        } else {
            return ApiResponseUtil.buildErrorResponse(org.springframework.http.HttpStatus.NOT_FOUND, "User not found", "User with ID " + id + " does not exist", "/api/users/" + id);
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @ModelAttribute @Valid UserRequest request,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            @RequestParam(value = "avatarUrl", required = false) String avatarUrl,
            @RequestParam(value = "existingAvatar", required = false) String existingAvatar) {

        log.info("UpdateUser called with id={}, request={}, avatar present={}, avatarUrl={}, existingAvatar={}",
                 id, request, avatar != null && !avatar.isEmpty(), avatarUrl, existingAvatar);

        try {
            if (request == null) {
                request = new UserRequest(); // Allow partial updates
            }

            Long currentUserId = getCurrentUserId();
            log.info("currentUserId={}, hasRole ADMIN={}, id={}", currentUserId, hasRole("ADMIN"), id);

            // Kiểm tra quyền: ADMIN có thể cập nhật bất kỳ user nào, CUSTOMER chỉ cập nhật chính mình
            if (!hasRole("ADMIN") && !id.equals(currentUserId)) {
                throw new AccessDeniedException("Customer can only update their own profile");
            }

            // Kiểm tra nếu CUSTOMER cố cập nhật các trường chỉ dành cho ADMIN
            if (hasRole("CUSTOMER") && (request.getRole() != null || request.getIsVerified() != null ||
                    request.getActive() != null || request.getLoyaltyPoints() != null || request.getLoyaltyTier() != null)) {
                throw new AccessDeniedException("Customer cannot update admin-only fields");
            }

            // Handle avatar upload or URL
            String finalAvatarUrl = request.getAvatar(); // Default to existing in request
            if (avatar != null && !avatar.isEmpty()) {
                log.info("Attempting to upload avatar file: {}", avatar.getOriginalFilename());
                try {
                    finalAvatarUrl = cloudinaryService.uploadFile(avatar);
                    log.info("Avatar uploaded successfully: {}", finalAvatarUrl);
                } catch (Exception e) {
                    log.error("Failed to upload avatar to Cloudinary, using dummy URL", e);
                    finalAvatarUrl = "https://dummy-avatar-url.com/" + avatar.getOriginalFilename();
                }
            } else if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                finalAvatarUrl = avatarUrl;
                log.info("Using provided avatarUrl: {}", finalAvatarUrl);
            } else if (existingAvatar != null && !existingAvatar.trim().isEmpty()) {
                finalAvatarUrl = existingAvatar;
                log.info("Using existing avatar: {}", finalAvatarUrl);
            }
            request.setAvatar(finalAvatarUrl);
            log.info("Final request avatar: {}", request.getAvatar());

            log.info("Final request to update: {}", request);

            // Gọi service để cập nhật
            UserResponse updatedUser = userService.updateUser(id, request);
            return ApiResponseUtil.buildResponse(true, "User updated successfully", updatedUser, "/api/users/" + id);

        } catch (AccessDeniedException e) {
            return ApiResponseUtil.buildErrorResponse(org.springframework.http.HttpStatus.FORBIDDEN, e.getMessage(), "ACCESS_DENIED", "/api/users/" + id);
        } catch (ValidationException e) {
            return ApiResponseUtil.buildErrorResponse(org.springframework.http.HttpStatus.BAD_REQUEST, "Validation failed", e.getMessage(), "/api/users/" + id);
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ApiResponseUtil.buildErrorResponse(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage(), "UNEXPECTED_ERROR", "/api/users/" + id);
        }
    }    
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> softDeleteUser(@PathVariable Long id) {
        userService.softDelete(id);
        return ApiResponseUtil.buildResponse(true, "User deleted successfully", null, "/api/users/" + id);
    }

    /**
     * Endpoint cho phép ADMIN thay đổi vai trò của một người dùng.
     * @param id ID của người dùng cần thay đổi vai trò.
     * @param role Vai trò mới (CUSTOMER, BUSINESS, FLIGHT_MANAGER, STAFF, ADMIN).
     * @return Phản hồi xác nhận thành công hoặc lỗi.
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable Long id,
            @RequestParam("role") String role) {
        userService.updateUserRole(id, role);
        return ApiResponseUtil.buildResponse(true, "User role updated successfully", null, "/api/v1/users/" + id + "/role");
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleActive(@PathVariable Long id) {
        userService.toggleActive(id);
        return ApiResponseUtil.buildResponse(true, "User active status toggled successfully", null, "/api/users/" + id + "/toggle-active");
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByUserId(@PathVariable Long id) {
        List<BookingResponse> bookings = userService.getBookingsByUserId(id);
        return ApiResponseUtil.buildResponse(true, "Get bookings by user successful", bookings, "/api/v1/users/" + id + "/bookings");
    }

    // Helper methods for authorization
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("No authentication found");
        }
        log.info("getCurrentUserId: auth.getName() = {}", auth.getName());
        try {
            Long userId = Long.valueOf(auth.getName());
            log.info("getCurrentUserId: parsed userId = {}", userId);
            return userId;
        } catch (NumberFormatException e) {
            log.info("getCurrentUserId: auth.getName() is not a number, querying by email");
            var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found with email: " + auth.getName()));
            log.info("getCurrentUserId: found userId = {} for email = {}", user.getId(), auth.getName());
            return user.getId();
        }
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
        }
        return false;
    }
}
