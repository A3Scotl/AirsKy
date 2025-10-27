package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.LoyaltyTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipValidationResponse {
    private boolean valid;
    private String membershipCode;
    private String userName;
    private String userEmail;
    private LoyaltyTier tier;
    private Integer currentPoints;
    private BigDecimal discountRate;
    private String message;
}