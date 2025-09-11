package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.request.FlightSearchRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.RoundTripFlightResponse;
import iuh.fit.airsky.dto.response.UnifiedFlightSearchResponse;
import iuh.fit.airsky.enums.FlightStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FlightService {
    FlightResponse createFlight(FlightRequest request);
    FlightResponse updateFlight(Long id, FlightRequest request);
    Optional<FlightResponse> findById(Long id);
    PageResponse<FlightResponse> findAll(Pageable pageable);
    PageResponse<FlightResponse> searchFlights(Long departureAirportId, Long arrivalAirportId, LocalDateTime startTime, LocalDateTime endTime, FlightStatus status, String tripType, Pageable pageable);
    void delete(Long id);

    // Tìm chuyến bay nội địa (trong cùng một quốc gia)
    PageResponse<FlightResponse> findDomesticFlights(String country, Pageable pageable);

    // Tìm chuyến bay từ quốc gia A đến quốc gia B
    PageResponse<FlightResponse> findFlightsBetweenCountries(String departureCountry, String arrivalCountry, Pageable pageable);

    // Tìm chuyến bay khứ hồi
    RoundTripFlightResponse searchRoundTripFlights(
        Long departureAirportId,
        Long arrivalAirportId,
        LocalDateTime outboundDate,
        LocalDateTime returnDate,
        FlightStatus status,
        Pageable pageable
    );

    // Tìm chuyến bay khứ hồi theo groupId
    PageResponse<FlightResponse> findRoundTripFlightsByGroupId(String groupId, Pageable pageable);

    // Tìm chuyến bay thống nhất dựa trên TripType
    UnifiedFlightSearchResponse searchUnifiedFlights(FlightSearchRequest request, Pageable pageable);
}