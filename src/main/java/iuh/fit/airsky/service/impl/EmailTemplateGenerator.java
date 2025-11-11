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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EmailTemplateGenerator {

    @Autowired
    private Configuration freemarkerConfig;

    /**
     * Generate template cho email xác nhận đặt vé (giữ nguyên code cũ).
     */
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

    /**
     * Generate template cho email thông báo hủy vé (mới thêm).
     * @param booking Booking entity (sử dụng để lấy info chung).
     * @param reason Lý do hủy (từ event).
     * @param bankInfo Thông tin ngân hàng để hướng dẫn manual refund (từ Payment).
     * @return HTML string cho email.
     */
    public String generateCancellationTemplate(Booking booking, String reason, String bankInfo) {
        try {
            // Load template từ classpath: templates/email/CancellationBooking.html
            Template template = freemarkerConfig.getTemplate("CancellationBooking.html");

            // Chuẩn bị data model cho hủy
            Map<String, Object> dataModel = prepareCancellationDataModel(booking, reason, bankInfo);

            // Render template thành HTML string
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, dataModel);

        } catch (IOException | TemplateException e) {
            log.error("Error generating cancellation email template for booking {}: {}", booking.getBookingId(), e.getMessage(), e);
            // Fallback: Trả về body đơn giản nếu render fail
            String fallback = String.format(
                    "<h3>Thông Báo Hủy Vé</h3>" +
                    "<p>Xin chào %s,</p>" +
                    "<p>Đặt vé <b>%s</b> của bạn đã bị hủy do: <b>%s</b>.</p>" +
                    "<p>Hoàn tiền: %s</p>" +
                    "<p>Hủy lúc: %s</p>" +
                    "<p>Vui lòng liên hệ hỗ trợ nếu cần hỗ trợ.</p>",
                    "Khách hàng", booking.getBookingCode(), reason, bankInfo,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))  // Ngày 05/11/2025
            );
            return fallback;
        }
    }

    /**
     * Helper: Chuẩn bị data model chung cho confirm (giữ nguyên).
     */
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
                    segMap.put("airlineName", "VietNam Airlines");
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
        dataModel.put("extrasTotal", String.format("%,.0f VND", bookingResponse.getAncillaryServicesAmount() != null ? bookingResponse.getAncillaryServicesAmount() : 0));
        dataModel.put("discountAmount", String.format("%,.0f VND", bookingResponse.getDiscountAmount() != null ? bookingResponse.getDiscountAmount() : 0));
        dataModel.put("appliedDealCode", bookingResponse.getAppliedDealCode());

        String formattedAmount = Optional.ofNullable(bookingResponse.getTotalAmount())
                .map(amount -> String.format("%,.0f", amount) + " VND").orElse("0 VND");
        dataModel.put("totalAmount", formattedAmount);

        return dataModel;
    }

    /**
     * Helper mới: Chuẩn bị data model cho hủy (tương tự confirm nhưng thêm reason + bankInfo).
     */
    private Map<String, Object> prepareCancellationDataModel(Booking booking, String reason, String bankInfo) {
        Map<String, Object> dataModel = new HashMap<>();

        // Thông tin cơ bản (tương tự confirm, nhưng dùng Booking entity thay vì Response)
        dataModel.put("contactName", Optional.ofNullable(booking.getContactName()).orElse("Khách hàng"));
        dataModel.put("bookingCode", Optional.ofNullable(booking.getBookingCode()).orElse("N/A"));
        dataModel.put("cancellationDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));  // Ngày hủy: 05/11/2025

        // Lý do hủy và bank info
        dataModel.put("cancellationReason", reason);
        dataModel.put("bankInfo", bankInfo);  // Ví dụ: "Số TK: 123456, Ngân hàng: Vietcombank"

        // Thông tin chuyến bay (từ booking.flightSegments nếu có, hoặc fallback flight chính)
        List<Map<String, Object>> flightSegments = new ArrayList<>();
        if (booking.getFlightSegments() != null && !booking.getFlightSegments().isEmpty()) {
            flightSegments = booking.getFlightSegments().stream()
                    .map(segment -> {
                        Map<String, Object> segMap = new HashMap<>();
                        segMap.put("flightNumber", segment.getFlight().getFlightNumber());
                        segMap.put("departureTime", segment.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
                        segMap.put("arrivalTime", segment.getArrivalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                        segMap.put("departureAirport", segment.getDepartureAirport().getAirportName());
                        segMap.put("arrivalAirport", segment.getArrivalAirport().getAirportName());
                        return segMap;
                    }).collect(Collectors.toList());
        } else if (booking.getFlight() != null) {
            // Fallback cho single flight
            Map<String, Object> segMap = new HashMap<>();
            segMap.put("flightNumber", booking.getFlight().getFlightNumber());
            segMap.put("departureTime", booking.getFlight().getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
            segMap.put("arrivalTime", booking.getFlight().getArrivalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            segMap.put("departureAirport", booking.getFlight().getDepartureAirport().getAirportName());
            segMap.put("arrivalAirport", booking.getFlight().getArrivalAirport().getAirportName());
            flightSegments.add(segMap);
        }
        dataModel.put("flightSegments", flightSegments);

        // Danh sách hành khách (tương tự confirm)
        List<Map<String, Object>> passengersList = booking.getPassengers().stream()
                .map(p -> {
                    Map<String, Object> pMap = new HashMap<>();
                    pMap.put("firstName", p.getFirstName());
                    pMap.put("lastName", p.getLastName());
                    pMap.put("passportNumber", p.getPassportNumber());  // Thêm cho hủy
                    return pMap;
                })
                .collect(Collectors.toList());
        dataModel.put("passengers", passengersList);

        // Thanh toán (thêm refund amount nếu có)
        Payment payment = booking.getPayment();
        BigDecimal refundAmt = (payment != null && payment.getRefundAmount() != null) ? payment.getRefundAmount() : booking.getTotalAmount();
        dataModel.put("refundAmount", String.format("%,.0f VND", refundAmt));
        dataModel.put("paymentMethod", payment != null ? payment.getPaymentMethod().name() : "N/A");

        return dataModel;
    }
}