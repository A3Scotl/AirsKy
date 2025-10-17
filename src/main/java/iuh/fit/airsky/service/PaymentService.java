// PaymentService.java
package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.response.PaymentResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.PaymentMethod;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
     * Get all payments with pagination and filters
     */
    PageResponse<PaymentResponse> findAllWithFilters(Pageable pageable, String search, String status, String paymentMethod, String startDate, String endDate);

    /**
     * Get all payments for a specific booking
     */
    List<PaymentResponse> findByBookingId(Long bookingId);

    /**
     * Get payment status by booking code
     */
    Optional<PaymentResponse> findByBookingCode(String bookingCode);

    /**
     * Delete a payment
     */
    void delete(Long id);

    /**
     * Create additional payment for booking modifications (seat changes, services)
     */
    PaymentResponse createAdditionalPayment(Long bookingId, BigDecimal additionalAmount, PaymentMethod paymentMethod);

    @Transactional
    boolean checkSepayTransaction(String bookingCode);

    void updateSepayPaymentStatus(String orderCode, String status, Double amount);

    /**
     * Process refund for cancelled booking
     */
    void processRefundForCancelledBooking(iuh.fit.airsky.model.Booking booking, String reason);
}