package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.CheckinRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.CheckinResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.service.CheckinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/checkins")
@RequiredArgsConstructor
@Slf4j
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping
    public ResponseEntity<ApiResponse<CheckinResponse>> createCheckin(@RequestBody CheckinRequest request) {
        log.info("Creating checkin: {}", request);
        CheckinResponse response = checkinService.createCheckin(request);
        ApiResponse<CheckinResponse> apiResponse = new ApiResponse<>(true, "Checkin created successfully", response, null, null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CheckinResponse>> updateCheckin(@PathVariable Long id, @RequestBody CheckinRequest request) {
        log.info("Updating checkin with ID: {}", id);
        CheckinResponse response = checkinService.updateCheckin(id, request);
        ApiResponse<CheckinResponse> apiResponse = new ApiResponse<>(true, "Checkin updated successfully", response, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CheckinResponse>> getCheckinById(@PathVariable Long id) {
        log.info("Getting checkin by ID: {}", id);
        Optional<CheckinResponse> response = checkinService.findById(id);
        if (response.isPresent()) {
            ApiResponse<CheckinResponse> apiResponse = new ApiResponse<>(true, "Checkin found", response.get(), null, null, null);
            return ResponseEntity.ok(apiResponse);
        } else {
            ApiResponse<CheckinResponse> apiResponse = new ApiResponse<>(false, "Checkin not found", null, null, null, null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CheckinResponse>>> getAllCheckins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting all checkins with page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<CheckinResponse> response = checkinService.findAll(pageable);
        ApiResponse<PageResponse<CheckinResponse>> apiResponse = new ApiResponse<>(true, "Checkins retrieved successfully", response, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCheckin(@PathVariable Long id) {
        log.info("Deleting checkin with ID: {}", id);
        checkinService.softDelete(id);
        ApiResponse<Void> apiResponse = new ApiResponse<>(true, "Checkin deleted successfully", null, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }
}