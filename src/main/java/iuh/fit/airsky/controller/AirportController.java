package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.AirportRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.service.AirportService;
import iuh.fit.airsky.service.CloudinaryService;
import iuh.fit.airsky.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;
    private final CloudinaryService cloudinaryService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AirportResponse>> getAirport(@PathVariable Long id) {
        try {
            return airportService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(true, "Airport retrieved successfully", response, "/api/v1/airports/" + id))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Airport not found", "RESOURCE_NOT_FOUND", "/api/v1/airports/" + id));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/airports/" + id);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AirportResponse>>> getAllAirports(Pageable pageable) {
        try {
            PageResponse<AirportResponse> response = airportService.findAll(pageable);
            return ApiResponseUtil.buildResponse(true, "Airports retrieved successfully", response, "/api/v1/airports");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/airports");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAirport(@PathVariable Long id) {
        try {
            airportService.softDelete(id);
            return ApiResponseUtil.buildResponse(true, "Airport soft deleted successfully", null, "/api/v1/airports/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/airports/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", ex.getMessage(), "/api/v1/airports/" + id);
        }
    }

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AirportResponse>> createAirportWithImage(
            @RequestParam("airportCode") String airportCode,
            @RequestParam("airportName") String airportName,
            @RequestParam(value = "cityNames", required = false) List<String> cityNames,
            @RequestParam(value = "countryId", required = false) Long countryId,
            @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "thumbnailUrl", required = false) String thumbnailUrl) {
        try {
            AirportRequest request = new AirportRequest();
            request.setAirportCode(airportCode);
            request.setAirportName(airportName);
            request.setCityNames(cityNames);
            request.setCountryId(countryId);
            request.setActive(active);

            // Handle image upload or URL
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String imageUrl = cloudinaryService.uploadFile(thumbnail);
                request.setThumbnail(imageUrl);
                log.info("Thumbnail uploaded successfully: {}", imageUrl);
            } else if (thumbnailUrl != null && !thumbnailUrl.trim().isEmpty()) {
                request.setThumbnail(thumbnailUrl);
            }

            if (airportCode == null || airportCode.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Mã sân bay không được để trống", null, "/api/v1/airports/upload");
            }
            if (airportName == null || airportName.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Tên sân bay không được để trống", null, "/api/v1/airports/upload");
            }

            AirportResponse response = airportService.createAirport(request);
            return ApiResponseUtil.buildResponse(true, "Tạo sân bay thành công", response, "/api/v1/airports/upload");
        } catch (Exception e) {
            log.error("Error creating airport with image", e);
            return ApiResponseUtil.buildResponse(false, "Có lỗi xảy ra khi tạo sân bay: " + e.getMessage(), null, "/api/v1/airports/upload");
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AirportResponse>> updateAirportWithImage(
            @PathVariable Long id,
            @RequestParam("airportCode") String airportCode,
            @RequestParam("airportName") String airportName,
            @RequestParam(value = "cityNames", required = false) List<String> cityNames,
            @RequestParam(value = "countryId", required = false) Long countryId,
            @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
            @RequestParam(value = "existingThumbnail", required = false) String existingThumbnail,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "thumbnailUrl", required = false) String thumbnailUrl) {
        try {
            AirportRequest request = new AirportRequest();
            request.setAirportCode(airportCode);
            request.setAirportName(airportName);
            request.setCityNames(cityNames);
            request.setCountryId(countryId);
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

            if (airportCode == null || airportCode.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Mã sân bay không được để trống", null, "/api/v1/airports/" + id + "/upload");
            }
            if (airportName == null || airportName.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Tên sân bay không được để trống", null, "/api/v1/airports/" + id + "/upload");
            }

            AirportResponse response = airportService.updateAirport(id, request);
            return ApiResponseUtil.buildResponse(true, "Cập nhật sân bay thành công", response, "/api/v1/airports/" + id + "/upload");
        } catch (ResourceNotFoundException e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/airports/" + id + "/upload");
        } catch (Exception e) {
            log.error("Error updating airport with image", e);
            return ApiResponseUtil.buildResponse(false, "Có lỗi xảy ra khi cập nhật sân bay: " + e.getMessage(), null, "/api/v1/airports/" + id + "/upload");
        }
    }
}