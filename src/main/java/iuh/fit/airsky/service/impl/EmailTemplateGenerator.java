package iuh.fit.airsky.service.impl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import iuh.fit.airsky.dto.response.BookingResponse;
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

    public String generateEmailTemplate(BookingResponse bookingResponse) {
        try {
            // Load template từ classpath: templates/email/ConfirmBooking.html
            Template template = freemarkerConfig.getTemplate("ConfirmBooking.html");

            // Chuẩn bị data model từ booking
            Map<String, Object> dataModel = prepareDataModel(bookingResponse);

            // Render template thành HTML string
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, dataModel);

        } catch (IOException | TemplateException e) {
            log.error("Error generating email template for booking {}: {}", bookingResponse.getBookingId(), e.getMessage(), e);
            // Fallback: Trả về body đơn giản nếu render fail
            return "<h3>Xác nhận đặt vé thành công</h3><p>Mã đặt vé: " + bookingResponse.getBookingCode() + "</p>";
        }
    }

    private Map<String, Object> prepareDataModel(BookingResponse bookingResponse) {
        Map<String, Object> dataModel = new HashMap<>();

        // Thông tin cơ bản
        dataModel.put("contactName", Optional.ofNullable(bookingResponse.getContactName()).orElse("Guest"));
        dataModel.put("bookingCode", Optional.ofNullable(bookingResponse.getBookingCode()).orElse("N/A"));
        dataModel.put("bookingDate", Optional.ofNullable(bookingResponse.getBookingDate()).map(d -> d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).orElse("N/A"));

        // Thông tin chuyến bay (lặp qua các chặng)
        List<Map<String, Object>> flightSegments = bookingResponse.getFlightSegments().stream()
                .map(segment -> {
                    Map<String, Object> segMap = new HashMap<>();
                    segMap.put("segmentOrder", segment.getSegmentOrder());
                    segMap.put("flightNumber", segment.getFlightNumber());
                    segMap.put("airlineName",  "VietNam Airlines");
                    segMap.put("flightDate", segment.getDepartureTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    segMap.put("departureTime", segment.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                    segMap.put("departureCode", segment.getDepartureAirport().getAirportCode());
                    segMap.put("departureAirport", segment.getDepartureAirport().getAirportName());
                    segMap.put("arrivalTime", segment.getArrivalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                    segMap.put("arrivalCode", segment.getArrivalAirport().getAirportCode());
                    segMap.put("arrivalAirport", segment.getArrivalAirport().getAirportName());
                    segMap.put("duration", segment.getDuration() != null ? segment.getDuration() : "N/A");
                    segMap.put("travelClass", segment.getClassName());
                    return segMap;
                }).collect(Collectors.toList());
        dataModel.put("flightSegments", flightSegments);

        // Danh sách hành khách
        List<Map<String, Object>> passengersList = bookingResponse.getPassengers().stream()
                .map(p -> {
                    Map<String, Object> pMap = new HashMap<>();
                    pMap.put("firstName", p.getFirstName());
                    pMap.put("lastName", p.getLastName());
                    // Lấy ghế từ chặng đầu tiên làm ví dụ
                    String seatNumber = p.getSeatAssignments() != null && !p.getSeatAssignments().isEmpty() ?
                                        p.getSeatAssignments().get(0).getSeatNumber() : "Chưa chọn";
                    pMap.put("seatNumber", seatNumber);
                    return pMap;
                })
                .collect(Collectors.toList());
        dataModel.put("passengers", passengersList);

        // Thanh toán
        var payment = bookingResponse.getPayment();
        dataModel.put("paymentDate", payment != null && payment.getPaymentDate() != null
                ? payment.getPaymentDate().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        dataModel.put("paymentMethod", payment != null ? payment.getPaymentMethod().name() : "N/A");
        
        // Chi tiết giá
        // dataModel.put("baseFare", String.format("%,.0f VND", bookingResponse.getBaseFare() != null ? bookingResponse.getBaseFare() : 0));
        // dataModel.put("taxesAndFees", String.format("%,.0f VND", bookingResponse.getTaxesAndFees() != null ? bookingResponse.getTaxesAndFees() : 0));
        dataModel.put("extrasTotal", String.format("%,.0f VND", bookingResponse.getAncillaryServicesAmount() != null ? bookingResponse.getAncillaryServicesAmount() : 0));
        dataModel.put("discountAmount", String.format("%,.0f VND", bookingResponse.getDiscountAmount() != null ? bookingResponse.getDiscountAmount() : 0));
        dataModel.put("appliedDealCode", bookingResponse.getAppliedDealCode());

        String formattedAmount = Optional.ofNullable(bookingResponse.getTotalAmount())
                .map(amount -> String.format("%,.0f", amount) + " VND").orElse("0 VND");
        dataModel.put("totalAmount", formattedAmount);


        return dataModel;
    }

}