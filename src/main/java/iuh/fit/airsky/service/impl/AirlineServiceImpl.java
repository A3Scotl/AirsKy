package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.AirlineRequest;
import iuh.fit.airsky.dto.response.AirlineResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.AirlineMapper;
import iuh.fit.airsky.model.Airline;
import iuh.fit.airsky.repository.AirlineRepository;
import iuh.fit.airsky.service.AirlineService;
import iuh.fit.airsky.service.CloudinaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j

public class AirlineServiceImpl implements AirlineService {

    private final AirlineRepository airlineRepository;
    private final AirlineMapper airlineMapper;
    private final CloudinaryService cloudinaryService;

    public AirlineServiceImpl(AirlineRepository airlineRepository, AirlineMapper airlineMapper, CloudinaryService cloudinaryService) {
        this.airlineRepository = airlineRepository;
        this.airlineMapper = airlineMapper;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    public AirlineResponse createAirline(AirlineRequest request) {
        log.info("Creating new airline with name: {}", request.getAirlineName());
        Airline airline = airlineMapper.toEntity(request);
        airline.setAirlineCode(request.getAirlineCode());
        airline.setThumbnail(request.getThumbnail()); // Set thumbnail
        airline.setActive(request.getActive() != null ? request.getActive() : true); // Set active with default true
        Airline saved = airlineRepository.save(airline);
        log.info("Airline created with ID: {}", saved.getAirlineId());
        return airlineMapper.toResponseDTO(saved);
    }

    @Override
    public AirlineResponse updateAirline(Long id, AirlineRequest request) {
        log.info("Updating airline with ID: {}", id);
        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id " + id));
        airline.setAirlineName(request.getAirlineName());
        airline.setContact(request.getContact());
        airline.setThumbnail(request.getThumbnail()); // Set thumbnail
        if (request.getActive() != null) {
            airline.setActive(request.getActive());
        }
        Airline updated = airlineRepository.save(airline);
        log.info("Airline updated with ID: {}", updated.getAirlineId());
        return airlineMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<AirlineResponse> findById(Long id) {
        log.info("Finding airline by ID: {}", id);
        return airlineRepository.findById(id).map(airlineMapper::toResponseDTO);
    }

    @Override
    public PageResponse<AirlineResponse> findAll(Pageable pageable) {
        log.info("Finding all airlines with pagination: {}", pageable);
        Page<Airline> page = airlineRepository.findAll(pageable);
        return new PageResponse<>(page.map(airlineMapper::toResponseDTO));
    }

    @Override
    public void softDelete(Long id) {
        log.info("Soft deleting airline with ID: {}", id);
        if (airlineRepository.findById(id).isEmpty()) {
            log.warn("Airline not found for soft delete: {}", id);
            throw new ResourceNotFoundException("Airline not found with id " + id);
        }
        airlineRepository.softDeleteById(id, LocalDateTime.now());
        log.info("Airline soft deleted: {}", id);
    }
}