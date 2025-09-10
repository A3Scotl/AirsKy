package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.UserRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.UserResponse;
import iuh.fit.airsky.service.UserService;
import iuh.fit.airsky.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        Optional<UserResponse> user = userService.findById(id);
        if (user.isPresent()) {
            return ApiResponseUtil.buildResponse(true, "User retrieved successfully", user.get(), "/api/users/" + id);
        } else {
            return ApiResponseUtil.buildErrorResponse(org.springframework.http.HttpStatus.NOT_FOUND, "User not found", "User with ID " + id + " does not exist", "/api/users/" + id);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        UserResponse updatedUser = userService.updateUser(id, request);
        return ApiResponseUtil.buildResponse(true, "User updated successfully", updatedUser, "/api/users/" + id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> softDeleteUser(@PathVariable Long id) {
        userService.softDelete(id);
        return ApiResponseUtil.buildResponse(true, "User deleted successfully", null, "/api/users/" + id);
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleActive(@PathVariable Long id) {
        userService.toggleActive(id);
        return ApiResponseUtil.buildResponse(true, "User active status toggled successfully", null, "/api/users/" + id + "/toggle-active");
    }
}
