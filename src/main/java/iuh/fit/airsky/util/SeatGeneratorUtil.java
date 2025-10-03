package iuh.fit.airsky.util;

import iuh.fit.airsky.model.Aircraft;
import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.model.Seat;
import iuh.fit.airsky.model.TravelClass;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.enums.SeatTypes;
import java.util.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SeatGeneratorUtil {

    /**
     * Tạo danh sách Seat cho một flight
     *
     * @param flight       flight cần tạo ghế
     * @param aircraft     aircraft gắn với flight
     * @param travelClasses danh sách hạng ghế
     * @return danh sách Seat đã tạo
     */
    private static SeatTypes determineSeatType(String className, int row, int totalRows, int seatIndex, int totalSeatsInRow) {
        Random random = new Random();
        // Tỷ lệ phần trăm cho mỗi loại ghế
        double standardChance = 0.7; // 70% cơ hội là ghế tiêu chuẩn
        double specialChance = 0.15;  // 15% cơ hội cho mỗi loại đặc biệt

        double randomValue = random.nextDouble();
        
        if (randomValue < standardChance) {
            return SeatTypes.STANDARD;
        } else if (randomValue < standardChance + specialChance) {
            // Loại ghế đặc biệt phụ thuộc vào hạng ghế
            if ("Economy".equalsIgnoreCase(className)) {
                return random.nextBoolean() ? SeatTypes.EXTRA_LEGROOM : SeatTypes.EXIT_ROW;
            } else {
                return random.nextBoolean() ? SeatTypes.FRONT_ROW : SeatTypes.ACCESSIBLE;
            }
        } else {
            // Loại ghế đặc biệt thứ hai
            if ("Economy".equalsIgnoreCase(className)) {
                return SeatTypes.EXIT_ROW;
            } else {
                return SeatTypes.ACCESSIBLE;
            }
        }
    }

    public static List<Seat> generateSeats(Flight flight, Aircraft aircraft, List<TravelClass> travelClasses) {
        List<Seat> seats = new ArrayList<>();
        Random random = new Random();

        // parse layout: "4-3" -> left=4, right=3
        String layout = Optional.ofNullable(aircraft.getSeatLayout()).orElse("3-3");
        String[] parts = layout.split("-");
        int left = Integer.parseInt(parts[0]);
        int right = Integer.parseInt(parts[1]);
        int seatsPerRow = left + right;

        // tạo bảng chữ ghế: A, B, C, ...
        char[] seatLetters = new char[seatsPerRow];
        for (int i = 0; i < seatsPerRow; i++) {
            seatLetters[i] = (char) ('A' + i);
        }

        int row = 1;
        int totalSeats = Optional.ofNullable(flight.getAvailableSeats()).orElse(seatsPerRow * 30); // default 30 row
        int businessSeats = 0;
        int economySeats = 0;

        // phân loại Business / Economy
        for (TravelClass tc : travelClasses) {
            if ("Business".equalsIgnoreCase(tc.getClassName())) {
                businessSeats = (int) (totalSeats * 0.2); // 20% Business
            } else {
                economySeats = totalSeats - businessSeats;
            }
        }

        // tạo Business
        TravelClass businessClass = travelClasses.stream()
                .filter(tc -> "Business".equalsIgnoreCase(tc.getClassName()))
                .findFirst().orElse(null);

        for (int i = 0; i < businessSeats; i++) {
            String seatNumber = row + String.valueOf(seatLetters[i % seatsPerRow]);
            if (i % seatsPerRow == 0 && i != 0) row++;
            Seat seat = Seat.builder()
                    .seatNumber(seatNumber)
                    .flight(flight)
                    .travelClass(businessClass)
                    .status(SeatStatus.AVAILABLE)
                    .build();
            seats.add(seat);
        }

        // tạo Economy
        TravelClass economyClass = travelClasses.stream()
                .filter(tc -> "Economy".equalsIgnoreCase(tc.getClassName()))
                .findFirst().orElse(null);

        for (int i = 0; i < economySeats; i++) {
            String seatNumber = row + String.valueOf(seatLetters[i % seatsPerRow]);
            if (i % seatsPerRow == 0 && i != 0) row++;
            SeatTypes seatType = determineSeatType(economyClass.getClassName(), row, totalSeats, i, seatsPerRow);
                
            Seat seat = Seat.builder()
                    .seatNumber(row + String.valueOf(seatLetters[i]))
                    .flight(flight)
                    .travelClass(economyClass)
                    .status(SeatStatus.AVAILABLE)
                    .type(seatType)
                    .build();
            seats.add(seat);
        }

        return seats;
    }
}
