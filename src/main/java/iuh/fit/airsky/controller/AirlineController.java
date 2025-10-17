package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.AirlineRequest;
import iuh.fit.airsky.dto.response.AirlineResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.service.AirlineService;
import iuh.fit.airsky.service.CloudinaryService;
import iuh.fit.airsky.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;
    private final CloudinaryService cloudinaryService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AirlineResponse>> getAirline(@PathVariable Long id) {
        try {
            return airlineService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(true, "Airline retrieved successfully", response, "/api/v1/airlines/" + id))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Airline not found", "RESOURCE_NOT_FOUND", "/api/v1/airlines/" + id));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/airlines/" + id);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AirlineResponse>>> getAllAirlines(Pageable pageable) {
        try {
            PageResponse<AirlineResponse> response = airlineService.findAll(pageable);
            return ApiResponseUtil.buildResponse(true, "Airlines retrieved successfully", response, "/api/v1/airlines");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/airlines");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteAirline(@PathVariable Long id) {
        try {
            airlineService.softDelete(id);
            return ApiResponseUtil.buildResponse(true, "Airline soft deleted successfully", null, "/api/v1/airlines/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/airlines/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", ex.getMessage(), "/api/v1/airlines/" + id);
        }
    }

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER')")
    public ResponseEntity<ApiResponse<AirlineResponse>> createAirlineWithImage(
            @RequestParam("airlineCode") String airlineCode,
            @RequestParam("airlineName") String airlineName,
            @RequestParam(value = "contact", required = false) String contact,
            @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "thumbnailUrl", required = false) String thumbnailUrl) {

        try {
            AirlineRequest request = new AirlineRequest();
            request.setAirlineCode(airlineCode);
            request.setAirlineName(airlineName);
            request.setContact(contact);
            request.setActive(active);

            // Handle image upload or URL
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String imageUrl = cloudinaryService.uploadFile(thumbnail);
                request.setThumbnail(imageUrl);
                log.info("Thumbnail uploaded successfully: {}", imageUrl);
            } else if (thumbnailUrl != null && !thumbnailUrl.trim().isEmpty()) {
                request.setThumbnail(thumbnailUrl);
            }

            // Validate required fields
            if (airlineCode == null || airlineCode.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Mã hãng hàng không không được để trống", null, "/api/v1/airlines/upload");
            }
            if (airlineName == null || airlineName.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Tên hãng hàng không không được để trống", null, "/api/v1/airlines/upload");
            }

            AirlineResponse response = airlineService.createAirline(request);
            return ApiResponseUtil.buildResponse(true, "Tạo hãng hàng không thành công", response, "/api/v1/airlines/upload");
        } catch (Exception e) {
            log.error("Error creating airline with image", e);
            return ApiResponseUtil.buildResponse(false, "Có lỗi xảy ra khi tạo hãng hàng không: " + e.getMessage(), null, "/api/v1/airlines/upload");
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER')")
    public ResponseEntity<ApiResponse<AirlineResponse>> updateAirlineWithImage(
            @PathVariable Long id,
            @RequestParam("airlineCode") String airlineCode,
            @RequestParam("airlineName") String airlineName,
            @RequestParam(value = "contact", required = false) String contact,
            @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
            @RequestParam(value = "existingThumbnail", required = false) String existingThumbnail,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "thumbnailUrl", required = false) String thumbnailUrl) {

        try {
            AirlineRequest request = new AirlineRequest();
            request.setAirlineCode(airlineCode);
            request.setAirlineName(airlineName);
            request.setContact(contact);
            request.setActive(active);

            // Handle image upload or URL
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String imageUrl = cloudinaryService.uploadFile(thumbnail);
                request.setThumbnail(imageUrl);
                log.info("Thumbnail uploaded successfully: {}", imageUrl);
            } else if (thumbnailUrl != null && !thumbnailUrl.trim().isEmpty()) {
                request.setThumbnail(thumbnailUrl);
            } else if (existingThumbnail != null && !existingThumbnail.trim().isEmpty()) {
                request.setThumbnail(existingThumbnail);
            }

            // Validate required fields
            if (airlineCode == null || airlineCode.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Mã hãng hàng không không được để trống", null, "/api/v1/airlines/" + id + "/upload");
            }
            if (airlineName == null || airlineName.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Tên hãng hàng không không được để trống", null, "/api/v1/airlines/" + id);
            }

            AirlineResponse response = airlineService.updateAirline(id, request);
            return ApiResponseUtil.buildResponse(true, "Cập nhật hãng hàng không thành công", response, "/api/v1/airlines/" + id);

        } catch (ResourceNotFoundException e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/airlines/" + id);
        } catch (Exception e) {
            log.error("Error updating airline with image", e);
            return ApiResponseUtil.buildResponse(false, "Có lỗi xảy ra khi cập nhật hãng hàng không: " + e.getMessage(), null, "/api/v1/airlines/" + id);
        }
    }
}