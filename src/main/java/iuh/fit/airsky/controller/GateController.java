package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.GateRequest;
import iuh.fit.airsky.dto.response.GateResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.service.GateService;
import iuh.fit.airsky.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/gates")
@RequiredArgsConstructor
public class GateController {

    private final GateService gateService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GateResponse>> createGate(@Valid @RequestBody GateRequest request) {
        try {
            GateResponse response = gateService.createGate(request);
            return ApiResponseUtil.buildResponse(true, "Gate created successfully", response, "/api/v1/gates");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Creation failed", ex.getMessage(), "/api/v1/gates");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GateResponse>> updateGate(@PathVariable Long id, @Valid @RequestBody GateRequest request) {
        try {
            GateResponse response = gateService.updateGate(id, request);
            return ApiResponseUtil.buildResponse(true, "Gate updated successfully", response, "/api/v1/gates/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/gates/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Update failed", ex.getMessage(), "/api/v1/gates/" + id);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GateResponse>> getGate(@PathVariable Long id) {
        try {
            return gateService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(true, "Gate retrieved successfully", response, "/api/v1/gates/" + id))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Gate not found", "RESOURCE_NOT_FOUND", "/api/v1/gates/" + id));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/gates/" + id);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<GateResponse>>> getAllGates(Pageable pageable) {
        try {
            PageResponse<GateResponse> response = gateService.findAll(pageable);
            return ApiResponseUtil.buildResponse(true, "Gates retrieved successfully", response, "/api/v1/gates");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/gates");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGate(@PathVariable Long id) {
        try {
            gateService.softDelete(id);
            return ApiResponseUtil.buildResponse(true, "Gate soft deleted successfully", null, "/api/v1/gates/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/gates/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", ex.getMessage(), "/api/v1/gates/" + id);
        }
    }
}