package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.response.QRVerificationResponse;
import iuh.fit.airsky.model.CheckIn;

public interface BoardingPassService {
    /**
     * Generate PDF boarding pass and return download URL
     */
    String generateBoardingPassPdf(CheckIn checkIn);

    /**
     * Send boarding pass PDF via email
     */
    void sendBoardingPassEmail(CheckIn checkIn, String pdfUrl);

    /**
     * Generate and send boarding pass in one call
     */
    String generateAndSendBoardingPass(CheckIn checkIn);

    /**
     * Verify QR code from boarding pass
     */
    QRVerificationResponse verifyQRCode(String qrCode);

    /**
     * Verify boarding pass by booking code and passenger ID
     */
    QRVerificationResponse verifyBoardingPass(String bookingCode, Long passengerId);
}