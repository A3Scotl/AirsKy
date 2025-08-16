package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.AirportRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.AirportMapper;
import iuh.fit.airsky.model.Airport;
import iuh.fit.airsky.repository.AirportRepository;
import iuh.fit.airsky.service.AirportService;
import iuh.fit.airsky.util.GenerateCodeUtil;
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
    private final GenerateCodeUtil generateCodeUtil;

    public AirportServiceImpl(AirportRepository airportRepository, AirportMapper airportMapper, GenerateCodeUtil generateCodeUtil) {
        this.airportRepository = airportRepository;
        this.airportMapper = airportMapper;
        this.generateCodeUtil = generateCodeUtil;
    }

    @Override
    public AirportResponse createAirport(AirportRequest request) {
        log.info("Creating new airport with name: {}", request.getAirportName());
        Airport airport = airportMapper.toEntity(request);
        airport.setAirportCode(generateCodeUtil.generateAirportCode(airportRepository));
        Airport saved = airportRepository.save(airport);
        log.info("Airport created with ID: {}", saved.getAirportId());
        return airportMapper.toResponseDTO(saved);
    }

    @Override
    public AirportResponse updateAirport(Long id, AirportRequest request) {
        log.info("Updating airport with ID: {}", id);
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with id " + id));
        airport.setAirportName(request.getAirportName());
        airport.setCity(request.getCity());
        airport.setCountry(request.getCountry());
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
}