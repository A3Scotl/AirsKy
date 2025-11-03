package iuh.fit.airsky.util;

import iuh.fit.airsky.model.Aircraft;
import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.model.FlightTravelClass;
import iuh.fit.airsky.model.Seat;
import iuh.fit.airsky.model.TravelClass;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.enums.SeatTypes;
import java.util.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SeatGeneratorUtil {

    /**
     * Tạo danh sách Seat cho một flight
     *
     * @param flight       flight cần tạo ghế
     * @param aircraft     aircraft gắn với flight
     * @param travelClasses danh sách hạng ghế
     * @return danh sách Seat đã tạo
     */
    private static SeatTypes determineSeatTypeForRow(String className, int row, int totalRows) {
        Random random = new Random();

        if ("First".equalsIgnoreCase(className)) {
            // First Class: ưu tiên FRONT_ROW và ACCESSIBLE
            double frontRowChance = 0.7;  // 70% FRONT_ROW
            double accessibleChance = 0.3; // 30% ACCESSIBLE

            double randomValue = random.nextDouble();
            if (randomValue < frontRowChance) {
                return SeatTypes.FRONT_ROW;
            } else {
                return SeatTypes.ACCESSIBLE;
            }

        } else if ("Business".equalsIgnoreCase(className)) {
            // Business Class: ưu tiên EXTRA_LEGROOM và FRONT_ROW
            double extraLegroomChance = 0.6;  // 60% EXTRA_LEGROOM
            double frontRowChance = 0.4;      // 40% FRONT_ROW

            double randomValue = random.nextDouble();
            if (randomValue < extraLegroomChance) {
                return SeatTypes.EXTRA_LEGROOM;
            } else {
                return SeatTypes.FRONT_ROW;
            }

        } else {
            // Economy Class: hỗn hợp các loại ghế
            double standardChance = 0.5;      // 50% STANDARD
            double extraLegroomChance = 0.25; // 25% EXTRA_LEGROOM
            double exitRowChance = 0.15;      // 15% EXIT_ROW
            double accessibleChance = 0.1;    // 10% ACCESSIBLE

            double randomValue = random.nextDouble();
            if (randomValue < standardChance) {
                return SeatTypes.STANDARD;
            } else if (randomValue < standardChance + extraLegroomChance) {
                return SeatTypes.EXTRA_LEGROOM;
            } else if (randomValue < standardChance + extraLegroomChance + exitRowChance) {
                return SeatTypes.EXIT_ROW;
            } else {
                return SeatTypes.ACCESSIBLE;
            }
        }
    }

    private static void generateSeatsForSection(List<Seat> seats, Flight flight,
            TravelClass firstClass, TravelClass businessClass, TravelClass economyClass,
            int startRow, int endRow, int sectionFirstSeats, int sectionBusinessSeats, int sectionEconomySeats,
            int seatsPerRow, char[] seatLetters, int currentSeatCount) {

        int currentRow = startRow;

        // Generate First Class seats (first 2 rows of section)
        if (firstClass != null && sectionFirstSeats > 0) {
            int firstClassRows = Math.min(2, (sectionFirstSeats + seatsPerRow - 1) / seatsPerRow); // Ceiling division
            for (int r = 0; r < firstClassRows && currentRow <= endRow; r++) {
                for (int s = 0; s < seatsPerRow; s++) {
                    String seatNumber = currentRow + String.valueOf(seatLetters[s]);
                    Seat seat = Seat.builder()
                            .seatNumber(seatNumber)
                            .flight(flight)
                            .travelClass(firstClass)
                            .status(SeatStatus.AVAILABLE)
                            .type(SeatTypes.FRONT_ROW)
                            .build();
                    seats.add(seat);
                }
                currentRow++;
            }
        }

        // Generate Business Class seats (next 3 rows of section)
        if (businessClass != null && sectionBusinessSeats > 0) {
            int businessClassRows = Math.min(3, (sectionBusinessSeats + seatsPerRow - 1) / seatsPerRow);
            for (int r = 0; r < businessClassRows && currentRow <= endRow; r++) {
                for (int s = 0; s < seatsPerRow; s++) {
                    String seatNumber = currentRow + String.valueOf(seatLetters[s]);
                    Seat seat = Seat.builder()
                            .seatNumber(seatNumber)
                            .flight(flight)
                            .travelClass(businessClass)
                            .status(SeatStatus.AVAILABLE)
                            .type(SeatTypes.EXTRA_LEGROOM)
                            .build();
                    seats.add(seat);
                }
                currentRow++;
            }
        }

        // Generate Economy Class seats (remaining rows of section)
        if (economyClass != null && sectionEconomySeats > 0) {
            while (currentRow <= endRow) {
                // Random loại ghế cho cả hàng thay vì từng ghế
                SeatTypes rowSeatType = determineSeatTypeForRow("Economy", currentRow, endRow - startRow + 1);

                for (int s = 0; s < seatsPerRow; s++) {
                    String seatNumber = currentRow + String.valueOf(seatLetters[s]);

                    Seat seat = Seat.builder()
                            .seatNumber(seatNumber)
                            .flight(flight)
                            .travelClass(economyClass)
                            .status(SeatStatus.AVAILABLE)
                            .type(rowSeatType)
                            .build();
                    seats.add(seat);
                }
                currentRow++;
            }
        }
    }

    public static List<Seat> generateSeats(Flight flight, Aircraft aircraft, List<TravelClass> travelClasses) {
        List<Seat> seats = new ArrayList<>();

        // Sơ đồ cố định 3-3 (6 ghế mỗi hàng: A B C D E F)
        int seatsPerRow = 6;
        char[] seatLetters = {'A', 'B', 'C', 'D', 'E', 'F'};

        // 180 ghế chia thành 2 dãy (mỗi dãy 90 ghế)
        // Mỗi dãy có 15 hàng x 6 cột = 90 ghế
        int totalRowsPerSection = 15;
        int totalSections = 2;

        // Find the 3 standard travel classes
        TravelClass firstClass = travelClasses.stream()
                .filter(tc -> "First".equals(tc.getClassName()))
                .findFirst().orElse(null);
        TravelClass businessClass = travelClasses.stream()
                .filter(tc -> "Business".equals(tc.getClassName()))
                .findFirst().orElse(null);
        TravelClass economyClass = travelClasses.stream()
                .filter(tc -> "Economy".equals(tc.getClassName()))
                .findFirst().orElse(null);

        // Calculate capacities based on aircraft total seats
        // Seat distribution: First(10%), Business(20%), Economy(70%)
        int totalSeats = aircraft.getTotalSeats();
        int firstSeats = (int) (totalSeats * 0.10);
        int businessSeats = (int) (totalSeats * 0.20);
        int economySeats = totalSeats - firstSeats - businessSeats;

        // Update the FlightTravelClass capacities based on actual seats generated
        // This is now done after seat generation below

        // Generate seats for each section (2 sections with separate row ranges)
        int sectionFirstSeats = firstSeats / totalSections;
        int sectionBusinessSeats = businessSeats / totalSections;
        int sectionEconomySeats = economySeats / totalSections;

        // Adjust for odd numbers in section 1
        int section1FirstSeats = sectionFirstSeats + (firstSeats % totalSections);
        int section1BusinessSeats = sectionBusinessSeats + (businessSeats % totalSections);
        int section1EconomySeats = sectionEconomySeats + (economySeats % totalSections);

        // Section 1: Rows 1-15
        generateSeatsForSection(seats, flight, firstClass, businessClass, economyClass,
                               1, 15, section1FirstSeats, section1BusinessSeats, section1EconomySeats,
                               seatsPerRow, seatLetters, 0);

        // Section 2: Rows 16-30 (use base section sizes)
        generateSeatsForSection(seats, flight, firstClass, businessClass, economyClass,
                               16, 30, sectionFirstSeats, sectionBusinessSeats, sectionEconomySeats,
                               seatsPerRow, seatLetters, 0);

        // Count actual seats generated per class
        Map<String, Long> seatsPerClass = seats.stream()
                .collect(Collectors.groupingBy(seat -> seat.getTravelClass().getClassName(), Collectors.counting()));

        // Update FlightTravelClass capacities based on actual seats generated
        if (flight.getFlightTravelClasses() != null) {
            for (FlightTravelClass ftc : flight.getFlightTravelClasses()) {
                String className = ftc.getTravelClass().getClassName();
                Long actualSeats = seatsPerClass.get(className);
                if (actualSeats != null) {
                    ftc.setCapacity(actualSeats.intValue());
                }
            }
        }

        System.out.println("Generated " + seats.size() + " seats total");

        // Quick validation
        long uniqueSeatNumbers = seats.stream().map(Seat::getSeatNumber).distinct().count();
        if (uniqueSeatNumbers != seats.size()) {
            System.err.println("ERROR: Found duplicate seat numbers! Expected " + seats.size() + " unique seats, got " + uniqueSeatNumbers);
        } else {
            System.out.println("✅ All " + seats.size() + " seats have unique numbers");
        }

        // Check row distribution
        Map<Integer, Long> seatsCountPerRow = seats.stream()
                .collect(Collectors.groupingBy(seat -> {
                    String seatNum = seat.getSeatNumber();
                    return Integer.parseInt(seatNum.replaceAll("[A-F]", ""));
                }, Collectors.counting()));

        System.out.println("Seats per row validation:");
        seatsCountPerRow.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (entry.getValue() != 6) {
                        System.err.println("ERROR: Row " + entry.getKey() + " has " + entry.getValue() + " seats (expected 6)");
                    }
                });

        System.out.println("Generated seats: First=" + seatsPerClass.getOrDefault("First", 0L) + 
                          ", Business=" + seatsPerClass.getOrDefault("Business", 0L) + 
                          ", Economy=" + seatsPerClass.getOrDefault("Economy", 0L) + 
                          ", Total=" + seats.size());
        return seats;
    }
}
