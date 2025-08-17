package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.AircraftRequest;
import iuh.fit.airsky.dto.response.AircraftResponse;
import iuh.fit.airsky.model.Aircraft;

import java.util.List;

public interface AircraftService {
    AircraftResponse createAircraft(AircraftRequest request);
    List<AircraftResponse> getAllAircrafts();
    AircraftResponse getAircraftById(Long id);
    Aircraft findEntityById(Long id);
}
