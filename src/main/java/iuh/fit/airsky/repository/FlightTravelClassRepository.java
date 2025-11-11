package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.model.FlightTravelClass;
import iuh.fit.airsky.model.TravelClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightTravelClassRepository extends JpaRepository<FlightTravelClass, Long> {

    FlightTravelClass findByFlightAndTravelClass(Flight flight, TravelClass travelClass);
}

