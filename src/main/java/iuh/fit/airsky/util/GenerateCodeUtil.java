package iuh.fit.airsky.util;

import iuh.fit.airsky.repository.AirlineRepository;
import iuh.fit.airsky.repository.AirportRepository;
import iuh.fit.airsky.repository.FlightRepository;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class GenerateCodeUtil {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Generate unique airline code (e.g., 2 chars uppercase)
     */
    public String generateAirlineCode(AirlineRepository repository) {
        Random random = new Random();
        String code;
        do {
            code = generateRandomString(2, random);
        } while (repository.findByAirlineCode(code).isPresent());
        return code;
    }

    /**
     * Generate unique airport code (e.g., 3 chars uppercase)
     */
    public String generateAirportCode(AirportRepository repository) {
        Random random = new Random();
        String code;
        do {
            code = generateRandomString(3, random);
        } while (repository.findByAirportCode(code).isPresent());
        return code;
    }

    /**
     * Generate unique flight number (e.g., airlineCode + 4 digits)
     */
    public String generateFlightNumber(FlightRepository repository, String airlineCode) {
        Random random = new Random();
        String number;
        do {
            number = airlineCode + String.format("%04d", random.nextInt(10000));
        } while (repository.findByFlightNumber(number).isPresent());
        return number;
    }

    private String generateRandomString(int length, Random random) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}