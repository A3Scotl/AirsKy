package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.UserRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.UserResponse;
import iuh.fit.airsky.enums.Role;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.UserMapper;
import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.NotificationService;
import iuh.fit.airsky.service.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.mapper.BookingMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final NotificationService notificationService;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, BCryptPasswordEncoder passwordEncoder, BookingRepository bookingRepository, BookingMapper bookingMapper, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.notificationService = notificationService;
    }


    @Override
    public UserResponse updateUser(Long id, UserRequest request) {
        log.info("Updating user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getPassportNumber() != null) {
            user.setPassportNumber(request.getPassportNumber());
        }
        if (request.getPassportExpiry() != null) {
            user.setPassportExpiry(request.getPassportExpiry());
        }
        if (request.getLoyaltyPoints() != null) {
            user.setLoyaltyPoints(request.getLoyaltyPoints());
        }
        if (request.getLoyaltyTier() != null) {
            user.setLoyaltyTier(request.getLoyaltyTier());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getIsVerified() != null) {
            user.setVerified(request.getIsVerified());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
        User updated = userRepository.save(user);
        log.info("User updated with ID: {}", updated.getId());
        return userMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<UserResponse> findById(Long id) {
        log.info("Finding user by ID: {}", id);
        return userRepository.findById(id).map(userMapper::toResponseDTO);
    }

    @Override
    public PageResponse<UserResponse> findAll(Pageable pageable) {
        log.info("Finding all users with pagination: {}", pageable);
        Page<User> page = userRepository.findAll(pageable);
        return new PageResponse<>(page.map(userMapper::toResponseDTO));
    }
@Override
@Transactional
public void updateUserRole(Long userId, String role) {
    log.info("Admin is attempting to change role for user {} to {}", userId, role);

    // 1. Tìm người dùng trong database
    User userToUpdate = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    // 2. Kiểm tra vai trò mới có hợp lệ không
    Role newRole;
    try {
        newRole = Role.valueOf(role.toUpperCase());
    } catch (IllegalArgumentException e) {
        throw new ValidationException("Invalid role: " + role);
    }

    // 3. Kiểm tra logic nghiệp vụ: không cho admin tự hạ vai trò của mình
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentAdminUsername = authentication.getName();
    if (userToUpdate.getEmail().equals(currentAdminUsername) && newRole != Role.ADMIN) {
        throw new AccessDeniedException("Admin cannot demote themselves.");
    }

    // 4. Cập nhật và lưu lại
    userToUpdate.setRole(newRole);
    userRepository.save(userToUpdate);

    log.info("Successfully updated role for user {} to {}", userId, newRole);

    // Gửi thông báo WebSocket cho người dùng bị thay đổi vai trò
    String message = String.format("Vai trò của bạn đã được thay đổi thành %s.", newRole.name());
    notificationService.sendNotificationToUserWithRelatedId(userId, "SYSTEM_ANNOUNCEMENT", message, userId);

}

    @Override
    public void softDelete(Long id) {
        log.info("Soft deleting user with ID: {}", id);
        if (userRepository.findById(id).isEmpty()) {
            log.warn("User not found for soft delete: {}", id);
            throw new ResourceNotFoundException("User not found with id " + id);
        }
        userRepository.softDeleteById(id, LocalDateTime.now());
        log.info("User soft deleted: {}", id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.info("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        log.info("Saving user: {}", user.getEmail());
        return userRepository.save(user);
    }

    @Override
    public void toggleActive(Long id) {
        log.info("Toggling active status for user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
        boolean newActive = !user.isActive();
        userRepository.updateActiveById(id, newActive);
        log.info("User active status toggled to {} for ID: {}", newActive, id);
    }

    @Override
    public List<BookingResponse> getBookingsByUserId(Long userId) {
        log.info("Getting bookings for user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        return bookingMapper.toResponseDTOList(bookingRepository.findByUserId(user));
    }
}