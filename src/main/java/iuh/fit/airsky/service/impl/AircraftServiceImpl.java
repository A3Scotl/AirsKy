package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.AircraftRequest;
import iuh.fit.airsky.dto.response.AircraftResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.AircraftMapper;
import iuh.fit.airsky.model.Aircraft;
import iuh.fit.airsky.repository.AircraftRepository;
import iuh.fit.airsky.repository.FlightRepository;
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
    private final FlightRepository flightRepository;

    @Override
    public List<AircraftResponse> getAllAircrafts() {
        return aircraftRepository.findAll()
                .stream()
                .map(aircraft -> {
                    AircraftResponse response = aircraftMapper.toResponseDTO(aircraft);
                    response.setIsInFlight(flightRepository.existsOnTimeFlightByAircraftId(aircraft.getAircraftId()));
                    response.setIsActive(aircraft.isActive());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public AircraftResponse getAircraftById(Long id) {
        Aircraft aircraft = findEntityById(id);
        AircraftResponse response = aircraftMapper.toResponseDTO(aircraft);
        response.setIsInFlight(flightRepository.existsOnTimeFlightByAircraftId(aircraft.getAircraftId()));
        response.setIsActive(aircraft.isActive());
        return response;
    }

    @Override
    public Aircraft findEntityById(Long id) {
        return aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found with id: " + id));
    }

    @Override
    public AircraftResponse createAircraft(AircraftRequest request) {
        // Basic validation for seat layout format
        if (request.getSeatLayout() != null && !request.getSeatLayout().matches("^\\d+(-\\d+)+$")) {
            throw new IllegalArgumentException("Invalid seat layout format. Expected format like '3-3', '3-2-3', etc.");
        }

        Aircraft aircraft = aircraftMapper.toEntity(request);
        aircraftRepository.save(aircraft);
        AircraftResponse response = aircraftMapper.toResponseDTO(aircraft);
        response.setIsInFlight(false); // Máy bay mới tạo chắc chắn chưa có chuyến bay ON_TIME
        response.setIsActive(aircraft.isActive());
        return response;
    }

    @Override
    public AircraftResponse updateAircraft(Long id, AircraftRequest request) {
        Aircraft aircraft = findEntityById(id);
        if (request.getAircraftCode() != null) {
            aircraft.setAircraftCode(request.getAircraftCode());
        }
        if (request.getAircraftName() != null) {
            aircraft.setAircraftName(request.getAircraftName());
        }
        if (request.getTotalSeats() != null) {
            aircraft.setTotalSeats(request.getTotalSeats());
        }
        if (request.getSeatLayout() != null) {
            if (!request.getSeatLayout().matches("^\\d+(-\\d+)+$")) {
                throw new IllegalArgumentException("Invalid seat layout format. Expected format like '3-3', '3-2-3', etc.");
            }
            aircraft.setSeatLayout(request.getSeatLayout());
        }
        aircraftRepository.save(aircraft);
        AircraftResponse response = aircraftMapper.toResponseDTO(aircraft);
        response.setIsInFlight(flightRepository.existsOnTimeFlightByAircraftId(aircraft.getAircraftId()));
        response.setIsActive(aircraft.isActive());

        return response;
    }

    @Override
    public void deleteAircraft(Long id) {
        Aircraft aircraft = findEntityById(id);
        aircraft.softDelete();
        aircraftRepository.save(aircraft);
    }
}
