package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.UserRequest;
import iuh.fit.airsky.dto.response.UserResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.model.User;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {
    UserResponse updateUser(Long id, UserRequest request);
    Optional<UserResponse> findById(Long id);
    PageResponse<UserResponse> findAll(Pageable pageable);
    void softDelete(Long id);
    
    // Methods for Google Auth
    Optional<User> findByEmail(String email);
    User save(User user);
}