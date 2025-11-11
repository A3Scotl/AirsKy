package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CountryResponse {
    private Long countryId;
    private String countryCode;
    private String countryName;
    private String thumbnail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;
    private LocalDateTime deletedAt;
    private boolean deleted;
}
