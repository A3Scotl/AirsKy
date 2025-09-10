package iuh.fit.airsky.util;

import iuh.fit.airsky.model.Aircraft;
import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.model.Seat;
import iuh.fit.airsky.model.TravelClass;
import iuh.fit.airsky.enums.SeatStatus;

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
    public static List<Seat> generateSeats(Flight flight, Aircraft aircraft, List<TravelClass> travelClasses) {
        List<Seat> seats = new ArrayList<>();

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

        // chia theo tỷ lệ
        int businessSeats = (int) (totalSeats * 0.2);
        int standardSeats = (int) (totalSeats * 0.3);
        int economySeats = totalSeats - businessSeats - standardSeats;

        // lấy TravelClass
        TravelClass businessClass = travelClasses.stream()
                .filter(tc -> "Business".equalsIgnoreCase(tc.getClassName()))
                .findFirst().orElse(null);

        TravelClass standardClass = travelClasses.stream()
                .filter(tc -> "Standard".equalsIgnoreCase(tc.getClassName()))
                .findFirst().orElse(null);

        TravelClass economyClass = travelClasses.stream()
                .filter(tc -> "Economy".equalsIgnoreCase(tc.getClassName()))
                .findFirst().orElse(null);

        // tạo ghế Business
        for (int i = 0; i < businessSeats; i++) {
            if (i % seatsPerRow == 0 && i != 0) row++;
            String seatNumber = row + String.valueOf(seatLetters[i % seatsPerRow]);
            seats.add(Seat.builder()
                    .seatNumber(seatNumber)
                    .flight(flight)
                    .travelClass(businessClass)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }

        // tạo ghế Standard
        for (int i = 0; i < standardSeats; i++) {
            if (i % seatsPerRow == 0 && i != 0) row++;
            String seatNumber = row + String.valueOf(seatLetters[i % seatsPerRow]);
            seats.add(Seat.builder()
                    .seatNumber(seatNumber)
                    .flight(flight)
                    .travelClass(standardClass)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }

        // tạo ghế Economy
        for (int i = 0; i < economySeats; i++) {
            if (i % seatsPerRow == 0 && i != 0) row++;
            String seatNumber = row + String.valueOf(seatLetters[i % seatsPerRow]);
            seats.add(Seat.builder()
                    .seatNumber(seatNumber)
                    .flight(flight)
                    .travelClass(economyClass)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }

        return seats;
    }

}
