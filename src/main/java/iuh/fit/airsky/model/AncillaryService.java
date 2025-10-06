package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import iuh.fit.airsky.enums.AncillaryServiceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "ancillary_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AncillaryService extends BaseFullSoftDeleteEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Long serviceId;
    
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private AncillaryServiceType serviceType;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "thumbnail")
    private String thumbnail;
    
    @Builder.Default
    @Column(name = "max_quantity")
    private Integer maxQuantity = 1;
    
    @Builder.Default
    @Column(name = "is_per_passenger", nullable = false)
    private Boolean isPerPassenger = true;
}