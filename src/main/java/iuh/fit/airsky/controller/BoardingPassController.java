package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.response.QRVerificationResponse;
import iuh.fit.airsky.repository.CheckinRepository;
import iuh.fit.airsky.service.BoardingPassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/boarding-passes")
@RequiredArgsConstructor
@Slf4j
public class BoardingPassController {

    private final BoardingPassService boardingPassService;
    private final CheckinRepository checkinRepository;

    @GetMapping("/download/{bookingCode}/{passengerId}")
    public ResponseEntity<String> getBoardingPassUrl(
            @PathVariable String bookingCode,
            @PathVariable Long passengerId) {

        try {
            // Find the CheckIn record to get the boarding pass URL
            var response = boardingPassService.verifyBoardingPass(bookingCode, passengerId);

            if (!response.isValid()) {
                return ResponseEntity.badRequest().body("Boarding pass not found");
            }

            // Get boarding pass URL from CheckIn record
            var checkInOpt = checkinRepository.findByBookingCodeAndPassengerId(bookingCode, passengerId);

            if (checkInOpt.isEmpty() || checkInOpt.get().getBoardingPassUrl() == null) {
                return ResponseEntity.notFound().build();
            }

            String boardingPassUrl = checkInOpt.get().getBoardingPassUrl();

            // Return the Cloudinary URL
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(boardingPassUrl);

        } catch (Exception e) {
            log.error("Error getting boarding pass URL for {} {}: {}", bookingCode, passengerId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error retrieving boarding pass URL");
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<QRVerificationResponse> verifyQRCode(@RequestParam("code") String qrCode) {
        log.info("Verifying QR code for boarding pass");
        
        try {
            QRVerificationResponse response = boardingPassService.verifyQRCode(qrCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error verifying QR code: {}", e.getMessage());
            
            QRVerificationResponse errorResponse = QRVerificationResponse.builder()
                .valid(false)
                .message("Invalid or expired boarding pass")
                .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/scan/{bookingCode}/{passengerId}")
    public ResponseEntity<QRVerificationResponse> scanBoardingPass(
            @PathVariable String bookingCode,
            @PathVariable Long passengerId) {
        
        log.info("Scanning boarding pass for booking: {} and passenger: {}", bookingCode, passengerId);
        
        try {
            QRVerificationResponse response = boardingPassService.verifyBoardingPass(bookingCode, passengerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error scanning boarding pass: {}", e.getMessage());
            
            QRVerificationResponse errorResponse = QRVerificationResponse.builder()
                .valid(false)
                .message("Boarding pass not found or invalid")
                .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}