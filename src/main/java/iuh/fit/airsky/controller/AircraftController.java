/*
 * @ (#) AircraftController.java 1.0 8/17/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.AircraftRequest;
import iuh.fit.airsky.dto.response.AircraftResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.service.AircraftService;
import iuh.fit.airsky.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/17/2025
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/aircrafts")
@RequiredArgsConstructor
public class AircraftController {

    private final AircraftService aircraftService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER')")
    public ResponseEntity<ApiResponse<AircraftResponse>> createAircraft(@RequestBody AircraftRequest request) {
        AircraftResponse response = aircraftService.createAircraft(request);
        return ApiResponseUtil.buildResponse(true, "Aircraft created successfully", response, "/api/v1/aircrafts");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER', 'STAFF', 'BUSINESS')")
    public ResponseEntity<ApiResponse<List<AircraftResponse>>> getAllAircrafts() {
        List<AircraftResponse> response = aircraftService.getAllAircrafts();
        return ApiResponseUtil.buildResponse(true, "Aircrafts retrieved successfully", response, "/api/v1/aircrafts");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<AircraftResponse>> getAircraftById(@PathVariable Long id) {
        AircraftResponse response = aircraftService.getAircraftById(id);
        return ApiResponseUtil.buildResponse(true, "Aircraft retrieved successfully", response, "/api/v1/aircrafts/" + id);
    }
}
