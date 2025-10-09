package iuh.fit.airsky.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import iuh.fit.airsky.model.CheckIn;
import iuh.fit.airsky.service.BoardingPassService;
import iuh.fit.airsky.service.CloudinaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@Slf4j
public class BoardingPassServiceImpl implements BoardingPassService {

    private final JavaMailSender mailSender;
    private final CloudinaryService cloudinaryService;

    @Value("${app.boarding-pass.base-url:http://localhost:8080/api/v1/boarding-passes}")
    private String baseUrl;

    public BoardingPassServiceImpl(JavaMailSender mailSender, CloudinaryService cloudinaryService) {
        this.mailSender = mailSender;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    public String generateBoardingPassPdf(CheckIn checkIn) {
        try {
            // Generate unique filename
            String fileName = String.format("boarding-pass-%s-%s.pdf",
                checkIn.getBooking().getBookingCode(),
                checkIn.getPassenger().getPassengerId());

            // Generate PDF bytes
            byte[] pdfBytes = generatePdfBoardingPass(checkIn);

            // Upload to Cloudinary
            String cloudinaryUrl = cloudinaryService.uploadPdfFile(pdfBytes, fileName);

            log.info("Boarding pass PDF uploaded to Cloudinary: {}", cloudinaryUrl);
            return cloudinaryUrl;

        } catch (Exception e) {
            log.error("Error generating boarding pass PDF for check-in {}: {}", checkIn.getCheckInId(), e.getMessage(), e);
            // Fallback to simple URL
            return generateSimpleBoardingPassUrl(checkIn);
        }
    }

    @Override
    public void sendBoardingPassEmail(CheckIn checkIn, String pdfUrl) {
        try {
            String passengerEmail = checkIn.getPassenger().getEmail();
            if (passengerEmail == null || passengerEmail.trim().isEmpty()) {
                log.warn("No email address found for passenger {}", checkIn.getPassenger().getPassengerId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(passengerEmail);
            helper.setSubject("Your AirsKy Boarding Pass - " + checkIn.getBooking().getBookingCode());
            helper.setFrom("noreply@airsky.com");

            String emailContent = buildEmailContent(checkIn, pdfUrl);
            helper.setText(emailContent, true);

            mailSender.send(message);
            log.info("Boarding pass email sent successfully to {} for check-in {}",
                passengerEmail, checkIn.getCheckInId());

        } catch (MessagingException e) {
            log.error("Error sending boarding pass email for check-in {}: {}", checkIn.getCheckInId(), e.getMessage(), e);
        }
    }

    @Override
    public String generateAndSendBoardingPass(CheckIn checkIn) {
        String pdfUrl = generateBoardingPassPdf(checkIn);
        sendBoardingPassEmail(checkIn, pdfUrl);
        return pdfUrl;
    }

    private byte[] generatePdfBoardingPass(CheckIn checkIn) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            // Header with airline logo placeholder
            Paragraph header = new Paragraph("AIRSKY AIRLINES")
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setMarginBottom(10);
            document.add(header);

            Paragraph subHeader = new Paragraph("BOARDING PASS")
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(subHeader);

            // Main boarding pass table
            Table mainTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

            // Left side - Passenger and flight info
            Cell leftCell = new Cell()
                .add(new Paragraph("PASSENGER NAME").setFontSize(8).setBold())
                .add(new Paragraph(checkIn.getPassenger().getFirstName() + " " + checkIn.getPassenger().getLastName())
                    .setFontSize(14).setBold())
                .add(new Paragraph("\nBOOKING REFERENCE").setFontSize(8).setBold())
                .add(new Paragraph(checkIn.getBooking().getBookingCode()).setFontSize(12))
                .add(new Paragraph("\nPASSENGER ID").setFontSize(8).setBold())
                .add(new Paragraph(checkIn.getPassenger().getPassengerId().toString()).setFontSize(12))
                .add(new Paragraph("\nPASSPORT NUMBER").setFontSize(8).setBold())
                .add(new Paragraph(checkIn.getPassenger().getPassportNumber() != null ?
                    checkIn.getPassenger().getPassportNumber() : "N/A").setFontSize(12))
                .add(new Paragraph("\nFLIGHT NUMBER").setFontSize(8).setBold())
                .add(new Paragraph(checkIn.getBooking().getFlight().getFlightNumber()).setFontSize(14).setBold())
                .add(new Paragraph("\nDEPARTURE").setFontSize(8).setBold())
                .add(new Paragraph(checkIn.getBooking().getFlight().getDepartureAirport().getCityName() +
                    " (" + checkIn.getBooking().getFlight().getDepartureAirport().getAirportCode() + ")")
                    .setFontSize(12))
                .add(new Paragraph("\nARRIVAL").setFontSize(8).setBold())
                .add(new Paragraph(checkIn.getBooking().getFlight().getArrivalAirport().getCityName() +
                    " (" + checkIn.getBooking().getFlight().getArrivalAirport().getAirportCode() + ")")
                    .setFontSize(12));

            // Right side - Boarding info and QR code
            Cell rightCell = new Cell();

            // Boarding information
            rightCell.add(new Paragraph("SEAT").setFontSize(8).setBold())
                .add(new Paragraph(checkIn.getSeatNumber()).setFontSize(20).setBold());
            rightCell.add(new Paragraph("\nCLASS").setFontSize(8).setBold())
                .add(new Paragraph(checkIn.getBooking().getTravelClass().getClassName()).setFontSize(12));
            rightCell.add(new Paragraph("\nDEPARTURE TIME").setFontSize(8).setBold())
                .add(new Paragraph(checkIn.getBooking().getFlight().getDepartureTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm\ndd/MM/yyyy"))).setFontSize(12));
            rightCell.add(new Paragraph("\nGATE").setFontSize(8).setBold())
                .add(new Paragraph(checkIn.getBooking().getFlight().getGate() != null ?
                    checkIn.getBooking().getFlight().getGate().getGateName() : "TBA").setFontSize(12));

            // Add QR Code
            try {
                Image qrCodeImage = generateQRCode(checkIn);
                qrCodeImage.setWidth(80);
                qrCodeImage.setHeight(80);
                rightCell.add(new Paragraph("\n\n").setFontSize(8));
                rightCell.add(qrCodeImage);
                rightCell.add(new Paragraph("Scan to verify").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            } catch (Exception e) {
                log.warn("Could not generate QR code for boarding pass: {}", e.getMessage());
                rightCell.add(new Paragraph("\n\nQR CODE").setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            }

            mainTable.addCell(leftCell);
            mainTable.addCell(rightCell);
            document.add(mainTable);

            // Additional flight information
            Table flightTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

            flightTable.addCell(createInfoCell("AIRCRAFT", checkIn.getBooking().getFlight().getAircraft() != null ?
                checkIn.getBooking().getFlight().getAircraft().getAircraftCode() : "TBA"));
            flightTable.addCell(createInfoCell("DURATION", checkIn.getBooking().getFlight().getDuration() + " minutes"));
            flightTable.addCell(createInfoCell("TICKET PRICE", String.format("%.0f VND", checkIn.getTicketPrice())));
            flightTable.addCell(createInfoCell("CHECK-IN TIME", checkIn.getCheckedAt()
                .format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))));

            document.add(flightTable);

            // Important notices
            Paragraph notice = new Paragraph(
                "• Please arrive at the airport 2 hours before departure\n" +
                "• Boarding gate closes 15 minutes before departure\n" +
                "• Have your passport and boarding pass ready\n" +
                "• This boarding pass is non-transferable\n" +
                "• For assistance, contact AirsKy customer service"
            ).setFontSize(9).setMarginTop(20);
            document.add(notice);

            // Footer
            Paragraph footer = new Paragraph("Thank you for choosing AirsKy Airlines • Safe travels!")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30)
                .setItalic();
            document.add(footer);

            log.info("Boarding pass PDF generated successfully for check-in {}", checkIn.getCheckInId());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF boarding pass: {}", e.getMessage(), e);
            throw new IOException("Failed to generate boarding pass PDF", e);
        }
    }

    private Cell createInfoCell(String label, String value) {
        return new Cell()
            .add(new Paragraph(label).setFontSize(8).setBold())
            .add(new Paragraph(value).setFontSize(10))
            .setTextAlignment(TextAlignment.CENTER);
    }

    private Image generateQRCode(CheckIn checkIn) throws WriterException, IOException {
        // Create QR code content with boarding pass information
        String qrContent = String.format("BOARDINGPASS|%s|%s|%s|%s|%s|%s",
            checkIn.getBooking().getBookingCode(),
            checkIn.getPassenger().getPassengerId(),
            checkIn.getBooking().getFlight().getFlightNumber(),
            checkIn.getSeatNumber(),
            checkIn.getBooking().getFlight().getDepartureTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            checkIn.getBooking().getFlight().getDepartureAirport().getAirportCode() + "-" +
            checkIn.getBooking().getFlight().getArrivalAirport().getAirportCode()
        );

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 200, 200);

        try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(pngData);
            ImageData imageData = ImageDataFactory.create("data:image/png;base64," + base64Image);
            return new Image(imageData);
        }
    }

    private String buildEmailContent(CheckIn checkIn, String pdfUrl) {
        String passengerName = checkIn.getPassenger().getFirstName() + " " + checkIn.getPassenger().getLastName();

        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #f8f9fa;">
                <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1 style="margin: 0; font-size: 28px;">✈️ AirsKy Boarding Pass</h1>
                    <p style="margin: 10px 0 0 0; opacity: 0.9;">Your check-in has been completed successfully!</p>
                </div>

                <div style="background: white; padding: 30px; border-radius: 0 0 10px 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 25px;">
                        <h3 style="margin-top: 0; color: #333;">📋 Flight Details</h3>
                        <table style="width: 100%%; border-collapse: collapse;">
                            <tr>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee;"><strong>Passenger:</strong></td>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee;"><strong>Booking Code:</strong></td>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee; font-weight: bold; color: #007bff;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee;"><strong>Flight:</strong></td>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee; font-weight: bold;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee;"><strong>Route:</strong></td>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee;">%s → %s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee;"><strong>Departure:</strong></td>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee;"><strong>Seat:</strong></td>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee; font-weight: bold; font-size: 18px; color: #28a745;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee;"><strong>Class:</strong></td>
                                <td style="padding: 8px 0; border-bottom: 1px solid #eee;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0;"><strong>Gate:</strong></td>
                                <td style="padding: 8px 0; font-weight: bold;">%s</td>
                            </tr>
                        </table>
                    </div>

                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                           color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px;
                           font-weight: bold; font-size: 16px; display: inline-block; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);">
                            📄 Download Boarding Pass PDF
                        </a>
                    </div>

                    <div style="background-color: #fff3cd; border: 1px solid #ffeaa7; border-radius: 8px; padding: 20px; margin: 20px 0;">
                        <h4 style="margin-top: 0; color: #856404;">⚠️ Important Travel Information</h4>
                        <ul style="color: #856404; margin: 0; padding-left: 20px;">
                            <li>Arrive at the airport at least <strong>2 hours</strong> before departure</li>
                            <li>Boarding gate closes <strong>15 minutes</strong> before departure</li>
                            <li>Bring your <strong>passport</strong> and this boarding pass</li>
                            <li>This boarding pass is <strong>non-transferable</strong></li>
                            <li>Check airport website for any updates or delays</li>
                        </ul>
                    </div>

                    <div style="background-color: #d1ecf1; border: 1px solid #bee5eb; border-radius: 8px; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0; color: #0c5460;">
                            <strong>💡 Tip:</strong> Save this email and keep your boarding pass PDF accessible on your mobile device for easy airport check-in.
                        </p>
                    </div>

                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">

                    <div style="text-align: center; color: #666;">
                        <p style="margin: 5px 0;"><strong>Thank you for choosing AirsKy Airlines!</strong></p>
                        <p style="margin: 5px 0;">Safe travels and have a pleasant journey! ✈️</p>
                        <p style="margin: 15px 0 0 0; font-size: 12px;">
                            For assistance, contact our customer service at support@airsky.com or call +84 xxx xxx xxx
                        </p>
                    </div>
                </div>

                <div style="text-align: center; padding: 20px; color: #999; font-size: 11px;">
                    <p>This is an automated email from AirsKy Airlines. Please do not reply to this message.</p>
                    <p>© 2025 AirsKy Airlines. All rights reserved.</p>
                </div>
            </body>
            </html>
            """,
            passengerName,
            checkIn.getBooking().getBookingCode(),
            checkIn.getBooking().getFlight().getFlightNumber(),
            checkIn.getBooking().getFlight().getDepartureAirport().getCityName(),
            checkIn.getBooking().getFlight().getArrivalAirport().getCityName(),
            checkIn.getBooking().getFlight().getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")),
            checkIn.getSeatNumber(),
            checkIn.getBooking().getTravelClass().getClassName(),
            checkIn.getBooking().getFlight().getGate() != null ?
                checkIn.getBooking().getFlight().getGate().getGateName() : "TBA",
            pdfUrl
        );
    }

    private String generateSimpleBoardingPassUrl(CheckIn checkIn) {
        return String.format("https://airsky.com/boarding-pass/%s/%s",
            checkIn.getBooking().getBookingCode(),
            checkIn.getPassenger().getPassengerId());
    }
}