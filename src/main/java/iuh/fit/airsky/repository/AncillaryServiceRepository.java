package iuh.fit.airsky.repository;

import iuh.fit.airsky.enums.AncillaryServiceType;
import iuh.fit.airsky.model.AncillaryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AncillaryServiceRepository extends JpaRepository<AncillaryService, Long> {
    
    @Query("SELECT a FROM AncillaryService a WHERE a.deleted = false")
    Page<AncillaryService> findAllActive(Pageable pageable);
    
    @Query("SELECT a FROM AncillaryService a WHERE a.deleted = false AND a.active = true")
    Page<AncillaryService> findAllActiveAndEnabled(Pageable pageable);
    
    @Query("SELECT a FROM AncillaryService a WHERE a.deleted = false AND a.active = true")
    List<AncillaryService> findAllActiveAndEnabledList();
    
    @Query("SELECT a FROM AncillaryService a WHERE a.deleted = false AND a.serviceType = :serviceType")
    List<AncillaryService> findByServiceType(@Param("serviceType") AncillaryServiceType serviceType);
    
    @Query("SELECT a FROM AncillaryService a WHERE a.deleted = false AND a.active = true " +
           "AND (:serviceType IS NULL OR a.serviceType = :serviceType)")
    Page<AncillaryService> findByServiceTypeAndActive(@Param("serviceType") AncillaryServiceType serviceType, Pageable pageable);
    
    @Query("SELECT a FROM AncillaryService a WHERE a.deleted = false AND a.serviceName LIKE %:name%")
    List<AncillaryService> findByServiceNameContaining(@Param("name") String name);
}