package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.AirportRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.AirportMapper;
import iuh.fit.airsky.model.Airport;
import iuh.fit.airsky.model.Country;
import iuh.fit.airsky.model.Gate;
import iuh.fit.airsky.repository.AirportRepository;
import iuh.fit.airsky.repository.CountryRepository;
import iuh.fit.airsky.repository.GateRepository;
import iuh.fit.airsky.service.AirportService;
import iuh.fit.airsky.service.CloudinaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AirportServiceImpl implements AirportService {

    private final AirportRepository airportRepository;
    private final AirportMapper airportMapper;
    private final CountryRepository countryRepository;
    private final CloudinaryService cloudinaryService;
    private final GateRepository gateRepository;

    public AirportServiceImpl(AirportRepository airportRepository, AirportMapper airportMapper, CountryRepository countryRepository, CloudinaryService cloudinaryService, GateRepository gateRepository) {
        this.airportRepository = airportRepository;
        this.airportMapper = airportMapper;
        this.countryRepository = countryRepository;
        this.cloudinaryService = cloudinaryService;
        this.gateRepository = gateRepository;
    }

    @Override
    public AirportResponse createAirport(AirportRequest request) {
        log.info("Creating new airport with name: {}", request.getAirportName());
        Airport airport = airportMapper.toEntity(request);
        airport.setAirportCode(request.getAirportCode());
        // Set active status if provided, otherwise default to true
        if (request.getActive() != null) {
            airport.setActive(request.getActive());
        } else {
            airport.setActive(true);
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
        if (request.getThumbnailUrl() != null && !request.getThumbnailUrl().trim().isEmpty()) {
            airport.setThumbnail(request.getThumbnailUrl());
        }
        if (request.getActive() != null) {
            airport.setActive(request.getActive());
        }
        if (request.getCountryId() != null) {
            Country country = countryRepository.findById(request.getCountryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Country not found with id " + request.getCountryId()));
            airport.setCountry(country);
        }
        if (request.getGates() != null) {
            boolean allEmpty = request.getGates().isEmpty() || request.getGates().stream().allMatch(g ->
                (g == null) ||
                ((g.getGateName() == null || g.getGateName().trim().isEmpty()) &&
                 (g.getTerminal() == null || g.getTerminal().trim().isEmpty()))
            );
            if (airport.getGates() != null) {
                airport.getGates().clear();
            } else {
                airport.setGates(new java.util.ArrayList<>());
            }
            if (!allEmpty) {
                for (var gateReq : request.getGates()) {
                    if (gateReq == null) continue;
                    if ((gateReq.getGateName() == null || gateReq.getGateName().trim().isEmpty()) &&
                        (gateReq.getTerminal() == null || gateReq.getTerminal().trim().isEmpty())) {
                        continue;
                    }
                    Gate gate = new Gate();
                    gate.setGateName(gateReq.getGateName());
                    gate.setTerminal(gateReq.getTerminal());
                    gate.setAirport(airport);
                    airport.getGates().add(gate);
                }
            }
        }
        // Cập nhật updatedAt thủ công nếu cần
        airport.setUpdatedAt(java.time.LocalDateTime.now());
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