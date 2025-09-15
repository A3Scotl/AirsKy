package iuh.fit.airsky.controller;

import iuh.fit.airsky.mapper.*;
import iuh.fit.airsky.repository.*;
import iuh.fit.airsky.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AirlineMapper airlineMapper;

    @Autowired
    private DealMapper dealMapper;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private CountryMapper countryMapper;

    @Autowired
    private AirportMapper airportMapper;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private AircraftMapper aircraftMapper;

    @Autowired
    AircraftRepository aircraftRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AirlineRepository airlineRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private FlightMapper flightMapper;

    @GetMapping("/{entity}/{format}")
    public void exportData(@PathVariable String entity, @PathVariable String format,
                           @RequestParam(required = false) LocalDate startDate,
                           @RequestParam(required = false) LocalDate endDate,
                           @RequestParam(defaultValue = "", required = false) List<String> fields,
                           HttpServletResponse response) throws IOException {
        List<?> data = getDataByEntity(entity, startDate, endDate);
        if (data == null || data.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        switch (format.toLowerCase()) {
            case "csv":
                exportService.exportToCsv(response, data, entity, fields);
                break;
            case "excel":
            case "xlsx":
                exportService.exportToExcel(response, data, entity, fields);
                break;
            case "pdf":
                exportService.exportToPdf(response, data, entity, fields);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Unsupported format: " + format);
        }
    }

    private List<?> getDataByEntity(String entity, LocalDate startDate, LocalDate endDate) {
        switch (entity.toLowerCase()) {
            case "bookings":
                return bookingMapper.toResponseDTOList(bookingRepository.findAll());
            case "users":
                return userMapper.toResponseDTOList(userRepository.findAll());
            case "airlines":
                return airlineMapper.toResponseDTOList(airlineRepository.findAll());
            case "airports":
                return airportMapper.toResponseDTOList(airportRepository.findAll());
            case "aircrafts":
                return aircraftMapper.toResponseDTOList(aircraftRepository.findAll());
            case "deals":
                return dealMapper.toResponseDTOList(dealRepository.findAll());
            case "blogs":
                return blogMapper.toResponseDTOList(blogRepository.findAll());
            case "flights":
                return flightMapper.toResponseDTOList(flightRepository.findAll());
            case "countries":
                return countryMapper.toResponseDTOList(countryRepository.findAll());
            default:
                return null;
        }
    }
}
