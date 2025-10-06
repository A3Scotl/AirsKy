package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.AncillaryServiceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AncillaryServiceResponse {
    
    private Long serviceId;
    private String serviceName;
    private AncillaryServiceType serviceType;
    private String serviceTypeDisplayName;
    private String description;
    private BigDecimal price;
    private Boolean isActive;
    private String thumbnail;
    private Integer maxQuantity;
    private Boolean isPerPassenger;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public String getServiceTypeDisplayName() {
        return serviceType != null ? serviceType.getVietnameseName() : null;
    }
}