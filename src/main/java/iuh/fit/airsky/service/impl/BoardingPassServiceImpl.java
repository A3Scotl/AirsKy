package iuh.fit.airsky.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import iuh.fit.airsky.dto.response.QRVerificationResponse;
import iuh.fit.airsky.enums.CheckinStatus;
import iuh.fit.airsky.model.CheckIn;
import iuh.fit.airsky.repository.CheckinRepository;
import iuh.fit.airsky.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;
import javax.imageio.ImageIO;

@Service
@Slf4j
public class BoardingPassServiceImpl implements BoardingPassService {

    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;
    private final CheckinRepository checkinRepository;

    @Value("${app.boarding-pass.base-url}")
    private String baseUrl;

    public BoardingPassServiceImpl(EmailService emailService, CloudinaryService cloudinaryService,
                                   CheckinRepository checkinRepository) {
        this.emailService = emailService;
        this.cloudinaryService = cloudinaryService;
        this.checkinRepository = checkinRepository;
    }

    @Override
    public String generateBoardingPassPdf(CheckIn checkIn) {
        try {
            // Generate unique filename
            String fileName = "boarding-pass-" + checkIn.getBooking().getBookingCode() + "-" + checkIn.getPassenger().getPassengerId() + ".png";
            // Generate boarding pass image directly
            byte[] imageBytes = generateBoardingPassImage(checkIn);
            // Upload to Cloudinary as image
            String cloudinaryUrl = cloudinaryService.uploadImageFile(imageBytes, fileName);
            log.info("Boarding pass image uploaded to Cloudinary: {}", cloudinaryUrl);
            return cloudinaryUrl;
        } catch (Exception e) {
            log.error("Error generating boarding pass for check-in {}: {}", checkIn.getCheckInId(), e.getMessage(), e);
            // Fallback to simple URL
            return baseUrl + "/download/" + checkIn.getBooking().getBookingCode() + "/" + checkIn.getPassenger().getPassengerId();
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
            String subject = "Your AirsKy Boarding Pass - " + checkIn.getBooking().getBookingCode();
            String emailContent = buildEmailContent(checkIn, pdfUrl);

            emailService.sendEmail(passengerEmail, subject, emailContent);

            log.info("Boarding pass email sent successfully to {} for check-in {}",
                    passengerEmail, checkIn.getCheckInId());
        } catch (Exception e) {
            log.error("Error sending boarding pass email for check-in {}: {}", checkIn.getCheckInId(), e.getMessage(), e);
        }
    }

    @Override
    public String generateAndSendBoardingPass(CheckIn checkIn) {
        String pdfUrl = generateBoardingPassPdf(checkIn);
        try {
            sendBoardingPassEmail(checkIn, pdfUrl);
        } catch (Exception e) {
            log.warn("Failed to send boarding pass email for check-in {}: {}", checkIn.getCheckInId(), e.getMessage());
            // Continue without failing the boarding pass generation
        }
        return pdfUrl;
    }

    private byte[] generateBoardingPassImage(CheckIn checkIn) throws IOException {
        try {
            // Validate required data
            if (checkIn == null || checkIn.getPassenger() == null || checkIn.getBooking() == null) {
                throw new IllegalArgumentException("CheckIn data is incomplete for boarding pass generation");
            }
            
            // Get flight info from segment if available, otherwise use booking flight
            var flight = (checkIn.getFlightSegment() != null && checkIn.getFlightSegment().getFlight() != null) 
                ? checkIn.getFlightSegment().getFlight() 
                : checkIn.getBooking().getFlight();
                
            if (flight == null) {
                throw new IllegalArgumentException("Flight information is missing for boarding pass generation");
            }

            // Increase size for better readability, similar to major airlines
            int width = 900;
            int height = 600;  // Reduced height since removing baggage and services
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            // Get graphics context
            Graphics2D g2d = image.createGraphics();

            // Set rendering hints for better quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Fill background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            // Header with gradient (inspired by Vietnam Airlines)
            GradientPaint gradient = new GradientPaint(0, 0, new Color(25, 118, 210),
                    width, 0, new Color(13, 71, 161));
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, width, 100);

            // Logo and title
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 32));
            g2d.drawString("AIRSKY", 50, 45);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("BOARDING PASS", 50, 75);

            // Flight information prominent
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("FLIGHT", 50, 120);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.setColor(new Color(25, 118, 210));
            g2d.drawString(flight.getFlightNumber() != null ? flight.getFlightNumber() : "", 50, 150);
            g2d.setColor(Color.BLACK);

            // Route - prominent route
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("FROM - TO", 50, 185);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            String route = (flight.getDepartureAirport() != null && flight.getDepartureAirport().getAirportCode() != null ? flight.getDepartureAirport().getAirportCode() : "") +
                    " → " + (flight.getArrivalAirport() != null && flight.getArrivalAirport().getAirportCode() != null ? flight.getArrivalAirport().getAirportCode() : "");
            g2d.drawString(route, 50, 215);

            // Passenger information
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("PASSENGER NAME", 50, 250);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            String passengerName = (checkIn.getPassenger().getFirstName() != null ? checkIn.getPassenger().getFirstName() : "") + " " +
                    (checkIn.getPassenger().getLastName() != null ? checkIn.getPassenger().getLastName() : "");
            g2d.drawString(passengerName.trim().toUpperCase(), 50, 275);

            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("BOOKING CODE", 50, 305);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            g2d.drawString(checkIn.getBooking().getBookingCode() != null ? checkIn.getBooking().getBookingCode() : "", 50, 330);

            // Right side - Key boarding info (inspired by major airlines)
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("SEAT", 550, 130);
            g2d.setFont(new Font("Arial", Font.BOLD, 32));
            g2d.setColor(new Color(25, 118, 210));
            String seatNumber = getCorrectSeatNumber(checkIn);
            g2d.drawString(seatNumber != null ? seatNumber : "", 550, 170);
            g2d.setColor(Color.BLACK);

            // Class - large and in English
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("CLASS", 550, 210);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.setColor(new Color(25, 118, 210));
            String className = checkIn.getBooking().getTravelClass() != null && checkIn.getBooking().getTravelClass().getClassName() != null
                    ? checkIn.getBooking().getTravelClass().getClassName().toUpperCase() : "";
            g2d.drawString(className, 550, 240);
            g2d.setColor(Color.BLACK);

            // Boarding Time (45 min before departure)
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("BOARDING TIME", 550, 280);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            String boardingTime = "";
            if (flight.getDepartureTime() != null) {
                LocalDateTime departure = flight.getDepartureTime();
                LocalDateTime boarding = departure.minusMinutes(45);
                boardingTime = boarding.format(DateTimeFormatter.ofPattern("HH:mm"));
            }
            g2d.drawString(boardingTime, 550, 305);

            // Departure Time
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("DEPARTURE TIME", 550, 335);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            String departureTime = flight.getDepartureTime() != null
                    ? flight.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                    : "";
            g2d.drawString(departureTime, 550, 360);

            // Gate
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("GATE", 550, 390);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.setColor(new Color(25, 118, 210));
            String gate = flight.getGate() != null
                    ? flight.getGate().getGateName() : "TBA";
            g2d.drawString(gate, 550, 415);
            g2d.setColor(Color.BLACK);

            // Arrival Time
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("ARRIVAL TIME", 50, 360);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            String arrivalTime = flight.getArrivalTime() != null
                    ? flight.getArrivalTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                    : "";
            g2d.drawString(arrivalTime, 50, 385);

            // QR Code
            try {
                BufferedImage qrCodeImage = generateQRCodeImage(checkIn);
                g2d.drawImage(qrCodeImage, 550, 450, 150, 150, null);
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                g2d.drawString("Scan to verify", 580, 615);
            } catch (Exception e) {
                log.warn("Could not generate QR code for boarding pass: {}", e.getMessage());
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("QR CODE", 600, 560);
            }

            // Footer with important info (similar to major airlines)
            g2d.setColor(new Color(100, 100, 100));
            g2d.setFont(new Font("Arial", Font.ITALIC, 11));
            g2d.drawString("• Please arrive at the airport 2 hours before departure time", 50, 520);
            g2d.drawString("• Boarding gate closes 15 minutes before departure", 50, 540);
            g2d.drawString("• Please bring your passport and boarding pass", 50, 560);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.setColor(new Color(25, 118, 210));
            g2d.drawString("Thank you for choosing AirsKy • Have a pleasant journey!", 50, 590);

            g2d.dispose();

            // Convert to byte array
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "PNG", baos);
                return baos.toByteArray();
            }

        } catch (Exception e) {
            log.error("Error generating boarding pass image: {}", e.getMessage(), e);
            throw new IOException("Failed to generate boarding pass image", e);
        }
    }

    private BufferedImage generateQRCodeImage(CheckIn checkIn) throws WriterException, IOException {
        // Validate required data
        if (checkIn == null || checkIn.getBooking() == null || checkIn.getPassenger() == null) {
            throw new IllegalArgumentException("CheckIn data is incomplete for QR code generation");
        }
        
        // Get flight info from segment if available, otherwise use booking flight
        var flight = (checkIn.getFlightSegment() != null && checkIn.getFlightSegment().getFlight() != null) 
            ? checkIn.getFlightSegment().getFlight() 
            : checkIn.getBooking().getFlight();
            
        if (flight == null) {
            throw new IllegalArgumentException("Flight information is missing for QR code generation");
        }
        // Create QR code content as JSON and encode as Base64 for security
        String jsonContent = String.format("""
                {
                    "type": "BOARDING_PASS",
                    "bookingCode": "%s",
                    "passengerId": %d,
                    "checkInId": %d,
                    "flightNumber": "%s",
                    "seatNumber": "%s",
                    "departureTime": "%s",
                    "route": "%s-%s",
                    "passengerName": "%s",
                    "timestamp": "%s"
                }
                """,
                checkIn.getBooking().getBookingCode() != null ? checkIn.getBooking().getBookingCode() : "",
                checkIn.getPassenger().getPassengerId() != null ? checkIn.getPassenger().getPassengerId() : 0L,
                checkIn.getCheckInId() != null ? checkIn.getCheckInId() : 0L,
                flight.getFlightNumber() != null ? flight.getFlightNumber() : "",
                getCorrectSeatNumber(checkIn) != null ? getCorrectSeatNumber(checkIn) : "",
                flight.getDepartureTime() != null ? flight.getDepartureTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "",
                flight.getDepartureAirport() != null && flight.getDepartureAirport().getAirportCode() != null ? flight.getDepartureAirport().getAirportCode() : "",
                flight.getArrivalAirport() != null && flight.getArrivalAirport().getAirportCode() != null ? flight.getArrivalAirport().getAirportCode() : "",
                (checkIn.getPassenger().getFirstName() != null ? checkIn.getPassenger().getFirstName() : "") + " " + (checkIn.getPassenger().getLastName() != null ? checkIn.getPassenger().getLastName() : ""),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        // Encode JSON as Base64
        String qrContent = Base64.getEncoder().encodeToString(jsonContent.getBytes());
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 250, 250);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private String buildEmailContent(CheckIn checkIn, String pdfUrl) {
        // Get flight info from segment if available, otherwise use booking flight
        var flight = (checkIn.getFlightSegment() != null && checkIn.getFlightSegment().getFlight() != null) 
            ? checkIn.getFlightSegment().getFlight() 
            : checkIn.getBooking().getFlight();
            
        String passengerName = (checkIn.getPassenger().getFirstName() != null ? checkIn.getPassenger().getFirstName() : "") + " " +
                (checkIn.getPassenger().getLastName() != null ? checkIn.getPassenger().getLastName() : "");
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #f8f9fa;">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
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
                checkIn.getBooking().getBookingCode() != null ? checkIn.getBooking().getBookingCode() : "",
                flight != null && flight.getFlightNumber() != null ? flight.getFlightNumber() : "",
                flight != null && flight.getDepartureAirport() != null && flight.getDepartureAirport().getCityName() != null ? flight.getDepartureAirport().getCityName() : "",
                flight != null && flight.getArrivalAirport() != null && flight.getArrivalAirport().getCityName() != null ? flight.getArrivalAirport().getCityName() : "",
                flight != null && flight.getDepartureTime() != null ? flight.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) : "",
                getCorrectSeatNumber(checkIn) != null ? getCorrectSeatNumber(checkIn) : "",
                checkIn.getBooking().getTravelClass() != null && checkIn.getBooking().getTravelClass().getClassName() != null ? checkIn.getBooking().getTravelClass().getClassName() : "",
                flight != null && flight.getGate() != null ? flight.getGate().getGateName() : "TBA",
                pdfUrl != null ? pdfUrl : ""
        );
    }

    @Override
    public QRVerificationResponse verifyQRCode(String qrCode) {
        try {
            // Decode Base64
            String jsonContent = new String(Base64.getDecoder().decode(qrCode));
            log.info("Decoded QR content: {}", jsonContent);
            // Parse JSON manually (simple approach)
            if (!jsonContent.contains("\"type\": \"BOARDING_PASS\"")) {
                throw new IllegalArgumentException("Invalid QR code format");
            }
            // Extract booking code and passenger ID from JSON
            String bookingCode = extractJsonValue(jsonContent, "bookingCode");
            Long passengerId = Long.valueOf(extractJsonValue(jsonContent, "passengerId"));
            return verifyBoardingPass(bookingCode, passengerId);
        } catch (Exception e) {
            log.error("Error verifying QR code: {}", e.getMessage());
            return QRVerificationResponse.builder()
                    .valid(false)
                    .message("Invalid QR code format or content")
                    .build();
        }
    }

    @Override
    public QRVerificationResponse verifyBoardingPass(String bookingCode, Long passengerId) {
        try {
            // Find CheckIn record by booking code and passenger ID
            Optional<CheckIn> checkInOpt = checkinRepository.findByBookingCodeAndPassengerId(bookingCode, passengerId);
            if (checkInOpt.isEmpty()) {
                return QRVerificationResponse.builder()
                        .valid(false)
                        .message("Boarding pass not found")
                        .build();
            }
            CheckIn checkIn = checkInOpt.get();
            // Verify that check-in is completed
            if (checkIn.getStatus() != CheckinStatus.COMPLETED) {
                return QRVerificationResponse.builder()
                        .valid(false)
                        .message("Check-in not completed")
                        .build();
            }
            // Get correct flight info from segment if available
            var flight = (checkIn.getFlightSegment() != null && checkIn.getFlightSegment().getFlight() != null) 
                ? checkIn.getFlightSegment().getFlight() 
                : checkIn.getBooking().getFlight();
                
            // Return verification response with details
            return QRVerificationResponse.builder()
                    .valid(true)
                    .message("✅ Boarding pass verified successfully")
                    .bookingCode(checkIn.getBooking().getBookingCode())
                    .passengerId(checkIn.getPassenger().getPassengerId())
                    .passengerName(checkIn.getPassenger().getFirstName() + " " + checkIn.getPassenger().getLastName())
                    .flightNumber(flight.getFlightNumber())
                    .seatNumber(getCorrectSeatNumber(checkIn))
                    .route(flight.getDepartureAirport().getAirportCode() + " → " +
                            flight.getArrivalAirport().getAirportCode())
                    .departureTime(flight.getDepartureTime())
                    .checkedAt(checkIn.getCheckedAt())
                    .status("VERIFIED")
                    .build();
        } catch (Exception e) {
            log.error("Error verifying boarding pass: {}", e.getMessage());
            return QRVerificationResponse.builder()
                    .valid(false)
                    .message("Error verifying boarding pass")
                    .build();
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchPattern = "\"" + key + "\": ";
        int startIndex = json.indexOf(searchPattern);
        if (startIndex == -1) {
            throw new IllegalArgumentException("Key not found: " + key);
        }

        startIndex += searchPattern.length();

        // Handle string values (with quotes)
        if (json.charAt(startIndex) == '"') {
            startIndex++; // Skip opening quote
            int endIndex = json.indexOf('"', startIndex);
            return json.substring(startIndex, endIndex);
        }
        // Handle numeric values (without quotes)
        else {
            int endIndex = json.indexOf(',', startIndex);
            if (endIndex == -1) {
                endIndex = json.indexOf('}', startIndex);
            }
            return json.substring(startIndex, endIndex).trim();
        }
    }

    /**
     * Get the correct seat number for the check-in segment
     * For roundtrip flights, this ensures we get the seat for the specific segment being checked in
     */
    private String getCorrectSeatNumber(CheckIn checkIn) {
        // First priority: Use seat number stored in CheckIn entity (segment-specific)
        if (checkIn.getSeatNumber() != null && !checkIn.getSeatNumber().trim().isEmpty()) {
            return checkIn.getSeatNumber();
        }
        
        // If CheckIn doesn't have seat number, try to get from passenger seat assignments for this segment
        if (checkIn.getFlightSegment() != null && checkIn.getPassenger() != null) {
            // This would require access to PassengerSeatAssignmentRepository
            // For now, log a warning and return empty
            log.warn("CheckIn {} doesn't have seat number, segment-specific lookup not available in this context", 
                    checkIn.getCheckInId());
        }
        
        // Fallback: Use passenger's main seat (may not be accurate for roundtrip)
        if (checkIn.getPassenger() != null && checkIn.getPassenger().getSeat() != null) {
            log.warn("Using passenger's main seat as fallback for CheckIn {}, may not be segment-specific", 
                    checkIn.getCheckInId());
            return checkIn.getPassenger().getSeat().getSeatNumber();
        }
        
        return ""; // No seat information available
    }
}