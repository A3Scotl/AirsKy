package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.CountryRequest;
import iuh.fit.airsky.dto.response.CountryResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.service.CloudinaryService;
import iuh.fit.airsky.service.CountryService;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;
    private final CloudinaryService cloudinaryService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CountryResponse>> getCountry(@PathVariable Long id) {
        try {
            return countryService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(true, "Country retrieved successfully", response, "/api/v1/countries/" + id))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Country not found", "RESOURCE_NOT_FOUND", "/api/v1/countries/" + id));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/countries/" + id);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CountryResponse>>> getAllCountries(Pageable pageable) {
        try {
            PageResponse<CountryResponse> response = countryService.findAll(pageable);
            return ApiResponseUtil.buildResponse(true, "Countries retrieved successfully", response, "/api/v1/countries");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/countries");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<CountryResponse>>> searchCountries(
            @RequestParam String countryName,
            Pageable pageable) {
        try {
            PageResponse<CountryResponse> response = countryService.searchByName(countryName, pageable);
            return ApiResponseUtil.buildResponse(true, "Countries searched successfully", response, "/api/v1/countries/search");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Search failed", ex.getMessage(), "/api/v1/countries/search");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCountry(@PathVariable Long id) {
        try {
            countryService.softDelete(id);
            return ApiResponseUtil.buildResponse(true, "Country soft deleted successfully", null, "/api/v1/countries/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/countries/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", ex.getMessage(), "/api/v1/countries/" + id);
        }
    }

    @PostMapping( consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CountryResponse>> createCountryWithImage(
            @RequestParam("countryCode") String countryCode,
            @RequestParam("countryName") String countryName,
            @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "thumbnailUrl", required = false) String thumbnailUrl) {

        try {
            CountryRequest request = new CountryRequest();
            request.setCountryCode(countryCode);
            request.setCountryName(countryName);
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
            if (countryCode == null || countryCode.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Mã quốc gia không được để trống", null, "/api/v1/countries/upload");
            }
            if (countryName == null || countryName.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Tên quốc gia không được để trống", null, "/api/v1/countries/upload");
            }

            CountryResponse response = countryService.createCountry(request);
            return ApiResponseUtil.buildResponse(true, "Tạo quốc gia thành công", response, "/api/v1/countries/upload");

        } catch (Exception e) {
            log.error("Error creating country with image", e);
            return ApiResponseUtil.buildResponse(false, "Có lỗi xảy ra khi tạo quốc gia: " + e.getMessage(), null, "/api/v1/countries/upload");
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CountryResponse>> updateCountryWithImage(
            @PathVariable Long id,
            @RequestParam("countryCode") String countryCode,
            @RequestParam("countryName") String countryName,
            @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
            @RequestParam(value = "existingThumbnail", required = false) String existingThumbnail,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "thumbnailUrl", required = false) String thumbnailUrl) {

        try {
            CountryRequest request = new CountryRequest();
            request.setCountryCode(countryCode);
            request.setCountryName(countryName);
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
            if (countryCode == null || countryCode.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Mã quốc gia không được để trống", null, "/api/v1/countries/" + id + "/upload");
            }
            if (countryName == null || countryName.trim().isEmpty()) {
                return ApiResponseUtil.buildResponse(false, "Tên quốc gia không được để trống", null, "/api/v1/countries/" + id + "/upload");
            }

            CountryResponse response = countryService.updateCountry(id, request);
            return ApiResponseUtil.buildResponse(true, "Cập nhật quốc gia thành công", response, "/api/v1/countries/" + id + "/upload");

        } catch (ResourceNotFoundException e) {
            return ApiResponseUtil.buildResponse(false, e.getMessage(), null, "/api/v1/countries/" + id + "/upload");
        } catch (Exception e) {
            log.error("Error updating country with image", e);
            return ApiResponseUtil.buildResponse(false, "Có lỗi xảy ra khi cập nhật quốc gia: " + e.getMessage(), null, "/api/v1/countries/" + id + "/upload");
        }
    }
}
