package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.AncillaryServiceRequest;
import iuh.fit.airsky.dto.response.AncillaryServiceResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.AncillaryServiceType;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.AncillaryServiceMapper;
import iuh.fit.airsky.repository.AncillaryServiceRepository;
import iuh.fit.airsky.service.AncillaryServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AncillaryServiceImpl implements AncillaryServiceService {
    
    private final AncillaryServiceRepository ancillaryServiceRepository;
    private final AncillaryServiceMapper ancillaryServiceMapper;
    
    @Override
    @Transactional
    public AncillaryServiceResponse createService(AncillaryServiceRequest request) {
        log.info("Creating new ancillary service: {}", request.getServiceName());
        
        iuh.fit.airsky.model.AncillaryService service = ancillaryServiceMapper.toEntity(request);
        iuh.fit.airsky.model.AncillaryService savedService = ancillaryServiceRepository.save(service);
        
        log.info("Ancillary service created with ID: {}", savedService.getServiceId());
        return ancillaryServiceMapper.toResponse(savedService);
    }
    
    @Override
    @Transactional
    public AncillaryServiceResponse updateService(Long id, AncillaryServiceRequest request) {
        log.info("Updating ancillary service with ID: {}", id);
        
        iuh.fit.airsky.model.AncillaryService existingService = ancillaryServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại với ID: " + id));
        
        ancillaryServiceMapper.updateEntityFromRequest(request, existingService);
        existingService.setUpdatedAt(LocalDateTime.now());
        
        iuh.fit.airsky.model.AncillaryService updatedService = ancillaryServiceRepository.save(existingService);
        
        log.info("Ancillary service updated successfully with ID: {}", updatedService.getServiceId());
        return ancillaryServiceMapper.toResponse(updatedService);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<AncillaryServiceResponse> findById(Long id) {
        log.debug("Finding ancillary service by ID: {}", id);
        return ancillaryServiceRepository.findById(id)
                .map(ancillaryServiceMapper::toResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AncillaryServiceResponse> findAll(Pageable pageable) {
        log.debug("Finding all ancillary services with pagination");
        Page<iuh.fit.airsky.model.AncillaryService> servicePage = ancillaryServiceRepository.findAllActive(pageable);
        return createPageResponse(servicePage);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AncillaryServiceResponse> findAllActive(Pageable pageable) {
        log.debug("Finding all active ancillary services with pagination");
        Page<iuh.fit.airsky.model.AncillaryService> servicePage = ancillaryServiceRepository.findAllActiveAndEnabled(pageable);
        return createPageResponse(servicePage);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AncillaryServiceResponse> findAllActiveList() {
        log.debug("Finding all active ancillary services as list");
        return ancillaryServiceRepository.findAllActiveAndEnabledList().stream()
                .map(ancillaryServiceMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AncillaryServiceResponse> findByServiceType(AncillaryServiceType serviceType, Pageable pageable) {
        log.debug("Finding ancillary services by type: {}", serviceType);
        Page<iuh.fit.airsky.model.AncillaryService> servicePage = ancillaryServiceRepository.findByServiceTypeAndActive(serviceType, pageable);
        return createPageResponse(servicePage);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AncillaryServiceResponse> findByServiceNameContaining(String name) {
        log.debug("Finding ancillary services by name containing: {}", name);
        return ancillaryServiceRepository.findByServiceNameContaining(name).stream()
                .map(ancillaryServiceMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deleteService(Long id) {
        log.info("Deleting ancillary service with ID: {}", id);
        
        iuh.fit.airsky.model.AncillaryService service = ancillaryServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại với ID: " + id));
        
        service.setDeleted(true);
        service.setDeletedAt(LocalDateTime.now());
        ancillaryServiceRepository.save(service);
        
        log.info("Ancillary service deleted successfully with ID: {}", id);
    }
    
    @Override
    @Transactional
    public void toggleActiveStatus(Long id) {
        log.info("Toggling active status for ancillary service with ID: {}", id);
        
        iuh.fit.airsky.model.AncillaryService service = ancillaryServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại với ID: " + id));
        
        service.setActive(!service.isActive());
        service.setUpdatedAt(LocalDateTime.now());
        ancillaryServiceRepository.save(service);
        
        log.info("Ancillary service active status toggled to: {} for ID: {}", service.isActive(), id);
    }
    
    private PageResponse<AncillaryServiceResponse> createPageResponse(Page<iuh.fit.airsky.model.AncillaryService> servicePage) {
        List<AncillaryServiceResponse> responses = servicePage.getContent().stream()
                .map(ancillaryServiceMapper::toResponse)
                .collect(Collectors.toList());
        
        PageResponse<AncillaryServiceResponse> pageResponse = new PageResponse<>();
        pageResponse.setContent(responses);
        pageResponse.setPageNumber(servicePage.getNumber());
        pageResponse.setPageSize(servicePage.getSize());
        pageResponse.setTotalElements(servicePage.getTotalElements());
        pageResponse.setTotalPages(servicePage.getTotalPages());
        pageResponse.setLast(servicePage.isLast());
        return pageResponse;
    }
}