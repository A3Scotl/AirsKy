// PaymentService.java
package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.response.PaymentResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PaymentService {
    /**
     * Create a new payment
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * Execute PayPal payment after user approval
     */
    PaymentResponse executePayPalPayment(String paymentId, String payerId, Long bookingId);

    /**
     * Get payment by ID
     */
    Optional<PaymentResponse> findById(Long id);

    /**
     * Get all payments with pagination
     */
    PageResponse<PaymentResponse> findAll(Pageable pageable);

    /**
     * Get all payments for a specific booking
     */
    List<PaymentResponse> findByBookingId(Long bookingId);

    /**
     * Delete a payment
     */
    void delete(Long id);


    @Transactional
    boolean checkSepayTransaction(String bookingCode);

    void updateSepayPaymentStatus(String orderCode, String status, Double amount);

    /**
     * Process refund for cancelled booking
     */
    void processRefundForCancelledBooking(iuh.fit.airsky.model.Booking booking, String reason);
}