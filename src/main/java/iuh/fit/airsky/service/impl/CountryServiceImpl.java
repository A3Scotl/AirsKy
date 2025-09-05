package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.CountryRequest;
import iuh.fit.airsky.dto.response.CountryResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.CountryMapper;
import iuh.fit.airsky.model.Country;
import iuh.fit.airsky.repository.CountryRepository;
import iuh.fit.airsky.service.CloudinaryService;
import iuh.fit.airsky.service.CountryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;
    private final CountryMapper countryMapper;
    private final CloudinaryService cloudinaryService;

    public CountryServiceImpl(CountryRepository countryRepository, CountryMapper countryMapper, CloudinaryService cloudinaryService) {
        this.countryRepository = countryRepository;
        this.countryMapper = countryMapper;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    public CountryResponse createCountry(CountryRequest request) {
        log.info("Creating new country with name: {}", request.getCountryName());
        Country country = countryMapper.toEntity(request);
        country.setCountryCode(request.getCountryCode());
        country.setThumbnail(request.getThumbnail());

        // Set active status if provided, otherwise default to true
        if (request.getActive() != null) {
            country.setActive(request.getActive());
        }

        Country saved = countryRepository.save(country);
        log.info("Country created with ID: {}", saved.getCountryId());
        return countryMapper.toResponseDTO(saved);
    }

    @Override
    public CountryResponse updateCountry(Long id, CountryRequest request) {
        log.info("Updating country with ID: {}", id);

        Country country = countryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id " + id));

        // Update all fields from request
        if (request.getCountryCode() != null && !request.getCountryCode().trim().isEmpty()) {
            country.setCountryCode(request.getCountryCode());
        }

        if (request.getCountryName() != null && !request.getCountryName().trim().isEmpty()) {
            country.setCountryName(request.getCountryName());
        }

        if (request.getThumbnail() != null) {
            country.setThumbnail(request.getThumbnail());
        }

        if (request.getActive() != null) {
            country.setActive(request.getActive());
        }

        Country updated = countryRepository.save(country);
        log.info("Country updated with ID: {}", updated.getCountryId());
        return countryMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<CountryResponse> findById(Long id) {
        log.info("Finding country by ID: {}", id);
        return countryRepository.findById(id).map(countryMapper::toResponseDTO);
    }

    @Override
    public PageResponse<CountryResponse> findAll(Pageable pageable) {
        log.info("Finding all countries with pagination: {}", pageable);
        Page<Country> page = countryRepository.findAll(pageable);
        return new PageResponse<>(page.map(countryMapper::toResponseDTO));
    }

    @Override
    public PageResponse<CountryResponse> searchByName(String countryName, Pageable pageable) {
        log.info("Searching countries by name: {} with pagination: {}", countryName, pageable);
        Page<Country> page = countryRepository.findByCountryNameContaining(countryName, pageable);
        return new PageResponse<>(page.map(countryMapper::toResponseDTO));
    }

    @Override
    public void softDelete(Long id) {
        log.info("Soft deleting country with ID: {}", id);
        if (countryRepository.findById(id).isEmpty()) {
            log.warn("Country not found for soft delete: {}", id);
            throw new ResourceNotFoundException("Country not found with id " + id);
        }
        countryRepository.softDeleteById(id, LocalDateTime.now());
        log.info("Country soft deleted: {}", id);
    }
}
