package iuh.fit.airsky.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
        dataModel.put("bookingCode", booking.getBookingCode());
        dataModel.put("bookingDate", booking.getBookingDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        Passenger primaryPassenger = booking.getPassengers().isEmpty() ? null : booking.getPassengers().get(0);
        dataModel.put("passengerName", primaryPassenger != null ? primaryPassenger.getFirstName() + " " + primaryPassenger.getLastName() : "");
        dataModel.put("phone", primaryPassenger != null ? primaryPassenger.getPhone() : "");
        dataModel.put("email", primaryPassenger != null ? primaryPassenger.getEmail() : "");

        // Thông tin chuyến bay (dùng flight chính)
        Flight flight = booking.getFlight();
        dataModel.put("flightNumber", flight.getFlightNumber());
        dataModel.put("flightDate", flight.getDepartureTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        dataModel.put("departureTime", flight.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        dataModel.put("departureAirport", flight.getDepartureAirport().getAirportName());
        dataModel.put("arrivalTime", flight.getArrivalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        dataModel.put("arrivalAirport", flight.getArrivalAirport().getAirportName());
        dataModel.put("travelClass",booking.getTravelClass().getClassName());

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

        String qrBase64 = generateQRBase64(booking.getBookingCode());
        if (qrBase64 != null) {
            dataModel.put("qrCode", qrBase64);
        }
        // Thanh toán
        Payment payment = booking.getPayment();
        dataModel.put("paymentDate", payment != null
                ? payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        dataModel.put("paymentMethod", payment != null ? payment.getPaymentMethod().name() : "Agency Credit");
        String formattedAmount = String.format("%,.0f", booking.getTotalAmount()) + " VND";
        dataModel.put("totalAmount", formattedAmount);


        return dataModel;
    }
    private String generateQRBase64(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 250, 250);

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(qrImage, "png", baos);
            baos.flush();
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            baos.close();

            return "data:image/png;base64," + base64Image; // 🔥 thêm prefix tại đây
        } catch (WriterException | IOException e) {
            log.error("Error generating QR code: {}", e.getMessage(), e);
            return null;
        }
    }


}