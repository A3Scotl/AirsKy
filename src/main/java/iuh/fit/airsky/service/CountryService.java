package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.CountryRequest;
import iuh.fit.airsky.dto.response.CountryResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CountryService {
    CountryResponse createCountry(CountryRequest request);
    CountryResponse updateCountry(Long id, CountryRequest request);
    Optional<CountryResponse> findById(Long id);
    PageResponse<CountryResponse> findAll(Pageable pageable);
    PageResponse<CountryResponse> searchByName(String countryName, Pageable pageable);
    void softDelete(Long id);
}
