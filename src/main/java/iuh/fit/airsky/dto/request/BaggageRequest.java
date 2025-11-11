package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.BaggageType;
import iuh.fit.airsky.enums.BaggagePackage;
import lombok.Data;

@Data
public class BaggageRequest {
    private Long checkinId;
    private BaggageType type;
    private BaggagePackage purchasedPackage;
}