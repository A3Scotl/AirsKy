package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.UserRequest;
import iuh.fit.airsky.dto.response.UserResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.UserMapper;
import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder; // Inject trong config Spring Security

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public UserResponse updateUser(Long id, UserRequest request) {
        log.info("Updating user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setVerified(request.isVerified());
        user.setRole(request.getRole());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
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

}