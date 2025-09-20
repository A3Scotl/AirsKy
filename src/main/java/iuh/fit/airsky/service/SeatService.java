package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.response.SeatResponse;
import iuh.fit.airsky.model.Flight;

import java.util.List;

public interface SeatService {

    List<SeatResponse> getSeatsByFlight(Long flightId);

    void createSeatsForFlight(Flight flight);
}
