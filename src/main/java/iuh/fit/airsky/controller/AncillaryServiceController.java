package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.AncillaryServiceRequest;
import iuh.fit.airsky.dto.response.AncillaryServiceResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.AncillaryServiceType;
import iuh.fit.airsky.service.AncillaryServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ancillary-services")
@RequiredArgsConstructor
@Slf4j
public class AncillaryServiceController {
    
    private final AncillaryServiceService ancillaryServiceService;
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AncillaryServiceResponse> createService(@Valid @RequestBody AncillaryServiceRequest request) {
        log.info("Request to create ancillary service: {}", request.getServiceName());
        AncillaryServiceResponse response = ancillaryServiceService.createService(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AncillaryServiceResponse> updateService(@PathVariable Long id, 
                                                                @Valid @RequestBody AncillaryServiceRequest request) {
        log.info("Request to update ancillary service with ID: {}", id);
        AncillaryServiceResponse response = ancillaryServiceService.updateService(id, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AncillaryServiceResponse> getServiceById(@PathVariable Long id) {
        log.info("Request to get ancillary service by ID: {}", id);
        return ancillaryServiceService.findById(id)
                .map(service -> ResponseEntity.ok(service))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<PageResponse<AncillaryServiceResponse>> getAllServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "serviceName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) AncillaryServiceType serviceType,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        
        log.info("Request to get all ancillary services - page: {}, size: {}, activeOnly: {}", page, size, activeOnly);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PageResponse<AncillaryServiceResponse> response;
        
        if (serviceType != null) {
            response = ancillaryServiceService.findByServiceType(serviceType, pageable);
        } else if (activeOnly) {
            response = ancillaryServiceService.findAllActive(pageable);
        } else {
            response = ancillaryServiceService.findAll(pageable);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<AncillaryServiceResponse>> getAllActiveServices() {
        log.info("Request to get all active ancillary services");
        List<AncillaryServiceResponse> services = ancillaryServiceService.findAllActiveList();
        return ResponseEntity.ok(services);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<AncillaryServiceResponse>> searchServicesByName(@RequestParam String name) {
        log.info("Request to search ancillary services by name: {}", name);
        List<AncillaryServiceResponse> services = ancillaryServiceService.findByServiceNameContaining(name);
        return ResponseEntity.ok(services);
    }
    
    @GetMapping("/types")
    public ResponseEntity<AncillaryServiceType[]> getAllServiceTypes() {
        log.info("Request to get all ancillary service types");
        return ResponseEntity.ok(AncillaryServiceType.values());
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        log.info("Request to delete ancillary service with ID: {}", id);
        ancillaryServiceService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleActiveStatus(@PathVariable Long id) {
        log.info("Request to toggle active status for ancillary service with ID: {}", id);
        ancillaryServiceService.toggleActiveStatus(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Sync isPerSegment flags in database based on service type logic
     */
    @PostMapping("/sync-per-segment-flags")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> syncPerSegmentFlags() {
        log.info("Request to sync isPerSegment flags");
        ancillaryServiceService.syncPerSegmentFlags();
        return ResponseEntity.ok("isPerSegment flags synced successfully");
    }
}