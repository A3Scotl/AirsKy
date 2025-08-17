package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.AircraftRequest;
import iuh.fit.airsky.dto.response.AircraftResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.AircraftMapper;
import iuh.fit.airsky.model.Aircraft;
import iuh.fit.airsky.repository.AircraftRepository;
import iuh.fit.airsky.service.AircraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AircraftServiceImpl implements AircraftService {

    private final AircraftRepository aircraftRepository;
    private final AircraftMapper aircraftMapper;

    @Override
    public AircraftResponse createAircraft(AircraftRequest request) {
        Aircraft aircraft = aircraftMapper.toEntity(request);
        aircraftRepository.save(aircraft);
        return aircraftMapper.toResponseDTO(aircraft);
    }

    @Override
    public List<AircraftResponse> getAllAircrafts() {
        return aircraftRepository.findAll()
                .stream()
                .map(aircraftMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AircraftResponse getAircraftById(Long id) {
        Aircraft aircraft = findEntityById(id);
        return aircraftMapper.toResponseDTO(aircraft);
    }

    @Override
    public Aircraft findEntityById(Long id) {
        return aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found with id: " + id));
    }
}

