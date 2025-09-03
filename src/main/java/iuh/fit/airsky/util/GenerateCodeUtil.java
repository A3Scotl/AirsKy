package iuh.fit.airsky.util;

import iuh.fit.airsky.repository.FlightRepository;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class GenerateCodeUtil {

    /**
     * Generate unique flight number (airlineCode + 4 digits)
     */
    public String generateFlightNumber(FlightRepository repository, String airlineCode) {
        Random random = new Random();
        String number;
        do {
            number = airlineCode + String.format("%04d", random.nextInt(10000));
        } while (repository.findByFlightNumber(number).isPresent());
        return number;
    }
}