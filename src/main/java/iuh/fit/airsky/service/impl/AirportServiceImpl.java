package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.AirportRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.AirportMapper;
import iuh.fit.airsky.model.Airport;
import iuh.fit.airsky.model.Country;
import iuh.fit.airsky.repository.AirportRepository;
import iuh.fit.airsky.repository.CountryRepository;
import iuh.fit.airsky.service.AirportService;
import iuh.fit.airsky.service.CloudinaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class AirportServiceImpl implements AirportService {

    private final AirportRepository airportRepository;
    private final AirportMapper airportMapper;
    private final CountryRepository countryRepository;
    private final CloudinaryService cloudinaryService;

    public AirportServiceImpl(AirportRepository airportRepository, AirportMapper airportMapper, CountryRepository countryRepository, CloudinaryService cloudinaryService) {
        this.airportRepository = airportRepository;
        this.airportMapper = airportMapper;
        this.countryRepository = countryRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    public AirportResponse createAirport(AirportRequest request) {
        log.info("Creating new airport with name: {}", request.getAirportName());
        Airport airport = airportMapper.toEntity(request);
        airport.setAirportCode(request.getAirportCode());
        airport.setThumbnail(request.getThumbnail());
        // Set active status if provided, otherwise default to true
        if (request.getActive() != null) {
            airport.setActive(request.getActive());
        }
        // Set cityNames (List<String>) and let entity handle cityName string
        if (request.getCityNames() != null) {
            airport.setCityNames(request.getCityNames());
        }
        // Set country if countryId is provided
        if (request.getCountryId() != null) {
            Country country = countryRepository.findById(request.getCountryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Country not found with id " + request.getCountryId()));
            airport.setCountry(country);
        }
        Airport saved = airportRepository.save(airport);
        log.info("Airport created with ID: {}", saved.getAirportId());
        return airportMapper.toResponseDTO(saved);
    }

    @Override
    public AirportResponse updateAirport(Long id, AirportRequest request) {
        log.info("Updating airport with ID: {}", id);
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with id " + id));
        // Update all fields from request
        if (request.getAirportCode() != null && !request.getAirportCode().trim().isEmpty()) {
            airport.setAirportCode(request.getAirportCode());
        }
        if (request.getAirportName() != null && !request.getAirportName().trim().isEmpty()) {
            airport.setAirportName(request.getAirportName());
        }
        if (request.getCityNames() != null) {
            airport.setCityNames(request.getCityNames());
        }
        if (request.getThumbnail() != null) {
            airport.setThumbnail(request.getThumbnail());
        }
        if (request.getActive() != null) {
            airport.setActive(request.getActive());
        }
        if (request.getCountryId() != null) {
            Country country = countryRepository.findById(request.getCountryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Country not found with id " + request.getCountryId()));
            airport.setCountry(country);
        }
        Airport updated = airportRepository.save(airport);
        log.info("Airport updated with ID: {}", updated.getAirportId());

        return airportMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<AirportResponse> findById(Long id) {
        log.info("Finding airport by ID: {}", id);
        return airportRepository.findById(id).map(airportMapper::toResponseDTO);
    }

    @Override
    public PageResponse<AirportResponse> findAll(Pageable pageable) {
        log.info("Finding all airports with pagination: {}", pageable);
        Page<Airport> page = airportRepository.findAll(pageable);
        return new PageResponse<>(page.map(airportMapper::toResponseDTO));
    }

    @Override
    public void softDelete(Long id) {
        log.info("Soft deleting airport with ID: {}", id);
        if (airportRepository.findById(id).isEmpty()) {
            log.warn("Airport not found for soft delete: {}", id);
            throw new ResourceNotFoundException("Airport not found with id " + id);
        }
        airportRepository.softDeleteById(id, LocalDateTime.now());
        log.info("Airport soft deleted: {}", id);
    }

    @Override
    public Optional<AirportResponse> findByAirportCode(String airportCode) {
        return airportRepository.findByAirportCode(airportCode)
                .filter(a -> !a.isDeleted())
                .map(airportMapper::toResponseDTO);
    }
}