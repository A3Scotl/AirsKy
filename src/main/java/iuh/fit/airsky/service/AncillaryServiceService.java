package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.AncillaryServiceRequest;
import iuh.fit.airsky.dto.response.AncillaryServiceResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.AncillaryServiceType;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AncillaryServiceService {
    
    AncillaryServiceResponse createService(AncillaryServiceRequest request);
    
    AncillaryServiceResponse updateService(Long id, AncillaryServiceRequest request);
    
    Optional<AncillaryServiceResponse> findById(Long id);
    
    PageResponse<AncillaryServiceResponse> findAll(Pageable pageable);
    
    PageResponse<AncillaryServiceResponse> findAllActive(Pageable pageable);
    
    List<AncillaryServiceResponse> findAllActiveList();
    
    PageResponse<AncillaryServiceResponse> findByServiceType(AncillaryServiceType serviceType, Pageable pageable);
    
    List<AncillaryServiceResponse> findByServiceNameContaining(String name);
    
    void deleteService(Long id);
    
    void toggleActiveStatus(Long id);
    
    void syncPerSegmentFlags();
}