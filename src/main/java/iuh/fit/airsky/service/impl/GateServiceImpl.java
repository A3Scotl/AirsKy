package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.GateRequest;
import iuh.fit.airsky.dto.response.GateResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.GateMapper;
import iuh.fit.airsky.model.Gate;
import iuh.fit.airsky.repository.AirportRepository;
import iuh.fit.airsky.repository.GateRepository;
import iuh.fit.airsky.service.GateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class GateServiceImpl implements GateService {

    private final GateRepository gateRepository;
    private final GateMapper gateMapper;
    private final AirportRepository airportRepository;

    public GateServiceImpl(GateRepository gateRepository, GateMapper gateMapper, AirportRepository airportRepository) {
        this.gateRepository = gateRepository;
        this.gateMapper = gateMapper;
        this.airportRepository = airportRepository;
    }

    @Override
    public GateResponse createGate(GateRequest request) {
        log.info("Creating new gate for airport ID: {}", request.getAirportId());
        Gate gate = gateMapper.toEntity(request);
        gate.setAirport(airportRepository.findById(request.getAirportId())
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with id " + request.getAirportId())));
        Gate saved = gateRepository.save(gate);
        log.info("Gate created with ID: {}", saved.getGateId());
        return gateMapper.toResponseDTO(saved);
    }

    @Override
    public GateResponse updateGate(Long id, GateRequest request) {
        log.info("Updating gate with ID: {}", id);
        Gate gate = gateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gate not found with id " + id));
        gate.setGateName(request.getGateName());
        Gate updated = gateRepository.save(gate);
        log.info("Gate updated with ID: {}", updated.getGateId());
        return gateMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<GateResponse> findById(Long id) {
        log.info("Finding gate by ID: {}", id);
        return gateRepository.findById(id).map(gateMapper::toResponseDTO);
    }

    @Override
    public PageResponse<GateResponse> findAll(Pageable pageable) {
        log.info("Finding all gates with pagination: {}", pageable);
        Page<Gate> page = gateRepository.findAll(pageable);
        return new PageResponse<>(page.map(gateMapper::toResponseDTO));
    }

    @Override
    public void softDelete(Long id) {
        log.info("Soft deleting gate with ID: {}", id);
        if (gateRepository.findById(id).isEmpty()) {
            log.warn("Gate not found for soft delete: {}", id);
            throw new ResourceNotFoundException("Gate not found with id " + id);
        }
        gateRepository.softDeleteById(id, LocalDateTime.now());
        log.info("Gate soft deleted: {}", id);
    }


}