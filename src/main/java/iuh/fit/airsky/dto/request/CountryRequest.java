package iuh.fit.airsky.dto.request;

import lombok.Data;

@Data
public class CountryRequest {
    private String countryCode;
    private String countryName;
    private String thumbnail;
    private Boolean active;
}
