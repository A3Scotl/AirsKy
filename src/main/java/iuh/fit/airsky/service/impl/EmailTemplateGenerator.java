package iuh.fit.airsky.service.impl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.model.Passenger;
import iuh.fit.airsky.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EmailTemplateGenerator {

    @Autowired
    private Configuration freemarkerConfig;

    public String generateEmailTemplate(Booking booking) {
        try {
            // Load template từ classpath: templates/email/ConfirmBooking.html
            Template template = freemarkerConfig.getTemplate("ConfirmBooking.html");

            // Chuẩn bị data model từ booking
            Map<String, Object> dataModel = prepareDataModel(booking);

            // Render template thành HTML string
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, dataModel);

        } catch (IOException | TemplateException e) {
            log.error("Error generating email template for booking {}: {}", booking.getBookingId(), e.getMessage(), e);
            // Fallback: Trả về body đơn giản nếu render fail
            return "<h3>Xác nhận đặt vé thành công</h3><p>Mã đặt vé: " + booking.getBookingCode() + "</p>";
        }
    }

    private Map<String, Object> prepareDataModel(Booking booking) {
        Map<String, Object> dataModel = new HashMap<>();

        // Thông tin cơ bản
        dataModel.put("bookingCode", Optional.ofNullable(booking.getBookingCode()).orElse("N/A"));
        dataModel.put("bookingDate", Optional.ofNullable(booking.getBookingDate()).map(d -> d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).orElse("N/A"));
        Passenger primaryPassenger = booking.getPassengers().isEmpty() ? null : booking.getPassengers().get(0);
        dataModel.put("passengerName", primaryPassenger != null ? String.format("%s %s", primaryPassenger.getFirstName(), primaryPassenger.getLastName()) : "Guest");
        dataModel.put("phone", primaryPassenger != null ? Optional.ofNullable(primaryPassenger.getPhone()).orElse("N/A") : "N/A");
        dataModel.put("email", primaryPassenger != null ? Optional.ofNullable(primaryPassenger.getEmail()).orElse("N/A") : "N/A");

        // Thông tin chuyến bay (dùng flight chính)
        Flight flight = booking.getFlight();
        dataModel.put("flightNumber", flight.getFlightNumber());
        dataModel.put("flightDate", flight.getDepartureTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        dataModel.put("departureTime", flight.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        dataModel.put("departureAirport", flight.getDepartureAirport().getAirportName());
        dataModel.put("arrivalTime", flight.getArrivalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        dataModel.put("arrivalAirport", flight.getArrivalAirport().getAirportName());
        dataModel.put("travelClass", booking.getTravelClass() != null ? booking.getTravelClass().getClassName() : "N/A");

        // Danh sách hành khách
        List<Map<String, Object>> passengersList = booking.getPassengers().stream()
                .map(p -> {
                    Map<String, Object> pMap = new HashMap<>();
                    pMap.put("firstName", p.getFirstName());
                    pMap.put("lastName", p.getLastName());
                    pMap.put("seatNumber", p.getSeat() != null ? p.getSeat().getSeatNumber() : "--");
                    return pMap;
                })
                .collect(Collectors.toList());
        dataModel.put("passengers", passengersList);

        // Thanh toán
        Payment payment = booking.getPayment();
        dataModel.put("paymentDate", payment != null
                ? payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        dataModel.put("paymentMethod", payment != null ? payment.getPaymentMethod().name() : "Agency Credit");
        
        String formattedAmount = Optional.ofNullable(booking.getTotalAmount())
                .map(amount -> String.format("%,.0f", amount) + " VND").orElse("0 VND");
        dataModel.put("totalAmount", formattedAmount);


        return dataModel;
    }

}