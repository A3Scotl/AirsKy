package iuh.fit.airsky.service.impl;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.PayPalRESTException;
import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.response.PaymentResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.PaymentMethod;
import iuh.fit.airsky.enums.PaymentStatus;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.exception.PaymentProcessingException;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.PaymentMapper;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Passenger;
import iuh.fit.airsky.model.Payment;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.repository.PaymentRepository;
import iuh.fit.airsky.repository.SeatRepository;
import iuh.fit.airsky.service.EmailService;
import iuh.fit.airsky.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final EmailService emailService;
    private final APIContext paypalApiContext;

    @Value("${paypal.success-url}")
    private String successUrl;

    @Value("${paypal.cancel-url}")
    private String cancelUrl;


    @Value("${sepay.api-key:}")
    private String sepayApiKey;


    @Value("${sepay.qr.account:}")
    private String sepayAccount;

    @Value("${sepay.qr.bank:}")
    private String sepayBank;

    @Value("${sepay.qr.template:compact}")
    private String sepayQrTemplate;

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Processing payment request for booking ID: {}", request.getBookingId());

        Booking booking = getValidBooking(request.getBookingId());
        Payment payment = getOrCreatePayment(booking, request.getPaymentMethod());

        return processPayment(payment, booking);
    }

    @Override
    @Transactional
    public PaymentResponse executePayPalPayment(String paymentId, String payerId, Long bookingId) {
        log.info("Executing PayPal payment - PaymentID: {}, BookingID: {}", paymentId, bookingId);

        Payment payment = paymentRepository.findByTransactionIdAndBooking_BookingId(paymentId, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with ID: " + paymentId + " for booking: " + bookingId
                ));

        validatePaymentForExecution(payment);

        try {
            PaymentExecution paymentExecution = new PaymentExecution();
            paymentExecution.setPayerId(payerId);

            com.paypal.api.payments.Payment paypalPayment =
                    new com.paypal.api.payments.Payment().setId(paymentId);

            com.paypal.api.payments.Payment executedPayment =
                    paypalPayment.execute(paypalApiContext, paymentExecution);

            return processSuccessfulPayment(payment, payerId, executedPayment);

        } catch (PayPalRESTException ex) {
            handlePaymentFailure(payment);
            throw new PaymentProcessingException("Failed to process PayPal payment", ex);
        }
    }

    @Override
    public Optional<PaymentResponse> findById(Long id) {
        log.info("Finding payment by ID: {}", id);
        return paymentRepository.findById(id).map(paymentMapper::toResponseDTO);
    }

    @Override
    public PageResponse<PaymentResponse> findAll(Pageable pageable) {
        log.info("Finding all payments with pagination: {}", pageable);
        Page<Payment> page = paymentRepository.findAll(pageable);
        return new PageResponse<>(page.map(paymentMapper::toResponseDTO));
    }

    @Override
    public List<PaymentResponse> findByBookingId(Long bookingId) {
        log.info("Finding payments for booking id: {}", bookingId);
        Optional<Payment> payments = paymentRepository.findByBooking_BookingId(bookingId);
        return payments.stream().map(paymentMapper::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting payment with ID: {}", id);
        if (!paymentRepository.existsById(id)) {
            log.warn("Payment not found for delete: {}", id);
            throw new ResourceNotFoundException("Payment not found with id " + id);
        }
        paymentRepository.deleteById(id);
        log.info("Payment deleted: {}", id);
    }

    private Booking getValidBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + bookingId
                ));
    }

    private Payment getOrCreatePayment(Booking booking, PaymentMethod paymentMethod) {
        return paymentRepository.findByBooking_BookingId(booking.getBookingId())
                .map(existingPayment -> handleExistingPayment(existingPayment, paymentMethod))
                .orElseGet(() -> createNewPayment(booking, paymentMethod));
    }

    private Payment handleExistingPayment(Payment existingPayment, PaymentMethod newPaymentMethod) {
        switch (existingPayment.getStatus()) {
            case COMPLETED:
                throw new PaymentProcessingException("Payment for this booking is already completed");
            case PENDING:
                if (existingPayment.getPaymentMethod() != newPaymentMethod) {
                    existingPayment.setPaymentMethod(newPaymentMethod);
                    return paymentRepository.save(existingPayment);
                }
                return existingPayment;
            case FAILED:
            case REFUNDED:
                existingPayment.setStatus(PaymentStatus.PENDING);
                existingPayment.setPaymentMethod(newPaymentMethod);
                return paymentRepository.save(existingPayment);
            default:
                throw new PaymentProcessingException(
                        "Cannot process payment with status: " + existingPayment.getStatus()
                );
        }
    }

    private Payment createNewPayment(Booking booking, PaymentMethod paymentMethod) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentDate(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    private PaymentResponse processPayment(Payment payment, Booking booking) {
        try {
            switch (payment.getPaymentMethod()) {
                case PAYPAL:
                    return processPayPalPayment(payment, booking);
                case BANK_TRANSFER:
                    return processSepayPayment(payment, booking);
                default:
                    return processRegularPayment(booking, payment);
            }
        } catch (Exception e) {
            log.error("Payment processing failed: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    private PaymentResponse processRegularPayment(Booking booking, Payment payment) {
        // Implementation for other payment methods
        throw new UnsupportedOperationException("Regular payment processing not implemented");
    }
    @Transactional
    @Override
    public boolean checkSepayTransaction(String bookingCode) {
        try {
            String url = "https://my.sepay.vn/userapi/transactions/list";
            String apiKey = "Bearer " + sepayApiKey; // cấu hình trong application.yml

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> transactions =
                        (List<Map<String, Object>>) response.getBody().get("transactions");

                for (Map<String, Object> tx : transactions) {
                    String content = tx.get("transaction_content").toString();
                    if (content.contains(bookingCode)) {
                        Double amount = Double.parseDouble(tx.get("amount_in").toString());
                        log.info("✅ Found transaction for booking {} with amount {}", bookingCode, amount);

                        updateSepayPaymentStatus("PAYBOOKING" + bookingCode, "SUCCESS", amount);
                        return true;
                    }
                }
            }

            log.info(" No transaction found for booking {}", bookingCode);
            return false;
        } catch (Exception e) {
            log.error("Error checking SePay transaction: {}", e.getMessage(), e);
            return false;
        }
    }


    private PaymentResponse processPayPalPayment(Payment payment, Booking booking) throws PayPalRESTException {
        if (payment.getTransactionId() != null && payment.getStatus() == PaymentStatus.PENDING) {
            return buildPaymentResponse(payment);
        }

        com.paypal.api.payments.Payment paypalPayment = createPayPalPayment(booking);

        payment.setTransactionId(paypalPayment.getId());
        payment.setPaypalApprovalUrl(getApprovalUrl(paypalPayment));
        paymentRepository.save(payment);

        return buildPaymentResponse(payment);
    }

    /**
     * SEPAY: create QR url via qr.sepay.vn (no external API call required)
     */
    private PaymentResponse processSepayPayment(Payment payment, Booking booking) {
        log.info("Creating SePay QR for booking {}", booking.getBookingId());

        try {


            // build description (use order code or booking code)
            String orderCode = String.format("PAYBOOKING%s", booking.getBookingCode());
            String description = orderCode;
            String encodedDescription = URLEncoder.encode(description, StandardCharsets.UTF_8);

            // amount as integer (VND) - ensure no decimals
            BigDecimal amount = booking.getTotalAmount().setScale(0, RoundingMode.HALF_UP);
            String amountStr = amount.toPlainString();

            // account & bank from config (ensure you set these in application.yml)
            if (sepayAccount == null || sepayAccount.isBlank() || sepayBank == null || sepayBank.isBlank()) {
                log.warn("SePay account/bank not configured (sepay.qr.account/sepay.qr.bank). Falling back to payment without QR.");
            }

            // Build QR url (per docs)
            String qrUrl = String.format(
                    "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s&template=%s&download=false",
                    URLEncoder.encode(Optional.ofNullable(sepayAccount).orElse(""), StandardCharsets.UTF_8),
                    URLEncoder.encode(Optional.ofNullable(sepayBank).orElse(""), StandardCharsets.UTF_8),
                    URLEncoder.encode(amountStr, StandardCharsets.UTF_8),
                    encodedDescription,
                    URLEncoder.encode(Optional.ofNullable(sepayQrTemplate).orElse("compact"), StandardCharsets.UTF_8)
            );

            // Save transaction/order code & QR url into Payment
            payment.setTransactionId(orderCode);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentDate(LocalDateTime.now());
            // reuse paypalApprovalUrl field to store the checkout/qr url
            payment.setPaypalApprovalUrl(qrUrl);

            paymentRepository.save(payment);

            // build response
            PaymentResponse response = paymentMapper.toResponseDTO(payment);
            response.setPaypalApprovalUrl(qrUrl); // return qr url to frontend
            return response;

        } catch (Exception e) {
            log.error("Error creating SePay QR: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to create SePay payment", e);
        }
    }
    private void updateBookingStatus(Booking booking) {
        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
            updatePassengerSeats(booking);
            bookingRepository.save(booking);
            log.info("Updated booking {} status to CONFIRMED", booking.getBookingId());
        }
    }
    private com.paypal.api.payments.Payment createPayPalPayment(Booking booking) throws PayPalRESTException {
        // Ensure totalAmount is not null and properly formatted
        BigDecimal totalAmount = booking.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentProcessingException("Invalid booking total amount: " + totalAmount);
        }

        // Format to exactly 2 decimal places as required by PayPal
        BigDecimal scaledAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        String formattedTotal = scaledAmount.toString();

        Amount amount = new Amount()
                .setCurrency("USD")
                .setTotal(formattedTotal);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription(String.format("Payment for booking #%s", booking.getBookingCode()));

        RedirectUrls redirectUrls = new RedirectUrls()
                .setCancelUrl(String.format("%s?bookingId=%d", cancelUrl, booking.getBookingId()))
                .setReturnUrl(String.format("%s?bookingId=%d", successUrl, booking.getBookingId()));

        com.paypal.api.payments.Payment paypalPayment = new com.paypal.api.payments.Payment()
                .setIntent("sale")
                .setPayer(new Payer().setPaymentMethod("paypal"))
                .setTransactions(Collections.singletonList(transaction))
                .setRedirectUrls(redirectUrls);

        return paypalPayment.create(paypalApiContext);
    }

    private String getApprovalUrl(com.paypal.api.payments.Payment paypalPayment) {
        return paypalPayment.getLinks().stream()
                .filter(link -> "approval_url".equals(link.getRel()))
                .findFirst()
                .map(Links::getHref)
                .orElseThrow(() -> new PaymentProcessingException("No approval URL found in PayPal response"));
    }

    private PaymentResponse buildPaymentResponse(Payment payment) {
        PaymentResponse response = paymentMapper.toResponseDTO(payment);
        if (payment.getPaymentMethod() == PaymentMethod.PAYPAL) {
            response.setPaypalApprovalUrl(payment.getPaypalApprovalUrl());
        } else if (payment.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            // Return the QR/check-out URL for BANK_TRANSFER
            response.setPaypalApprovalUrl(payment.getPaypalApprovalUrl());
        }
        return response;
    }

    private void validatePaymentForExecution(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentProcessingException(
                    String.format("Cannot execute payment with status: %s", payment.getStatus())
            );
        }
    }

    private PaymentResponse processSuccessfulPayment(Payment payment, String payerId,
                                                     com.paypal.api.payments.Payment executedPayment) {

        if (!"approved".equalsIgnoreCase(executedPayment.getState())) {
            handlePaymentFailure(payment);
            throw new PaymentProcessingException("Payment not approved. Status: " + executedPayment.getState());
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPayerId(payerId);
        payment.setPaymentDate(LocalDateTime.now());

        paymentRepository.save(payment);

        updateBookingStatus(payment.getBooking());

        log.info("Payment {} completed successfully", payment.getPaymentId());
        return paymentMapper.toResponseDTO(payment);
    }

    @Transactional
    @Override
    public void updateSepayPaymentStatus(String orderCode, String status, Double amount) {
        log.info("Updating SePay payment with order_code: {}, status: {}, amount: {}", orderCode, status, amount);

        Optional<Payment> opt = paymentRepository.findByTransactionId(orderCode);
        if (opt.isEmpty()) {
            log.warn("No payment found for orderCode {}", orderCode);
            return;
        }

        Payment payment = opt.get();

        // Optional: verify amount (if amount provided)
        if (amount != null) {
            try {
                double expected = payment.getAmount() != null ? payment.getAmount().doubleValue() : 0.0;
                if (Double.compare(expected, amount) != 0) {
                    log.warn("Amount mismatch for payment {}: expected {}, received {}", payment.getPaymentId(), expected, amount);
                    // You may choose to still accept or ignore — here we just log and continue
                }
            } catch (Exception ex) {
                log.warn("Failed to compare amounts for payment {}", payment.getPaymentId(), ex);
            }
        }

        if ("PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status) || "IN".equalsIgnoreCase(status)) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);
            updateBookingStatus(payment.getBooking());
            log.info("Payment {} marked as COMPLETED via SePay webhook", payment.getPaymentId());
        } else if ("FAILED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status) || "OUT".equalsIgnoreCase(status)) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Payment {} marked as FAILED via SePay webhook", payment.getPaymentId());
        } else {
            log.info("Ignoring SePay webhook with unrecognized status: {}", status);
        }
    }

    private void handlePaymentFailure(Payment payment ) {
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        log.error("Payment failed: {}", payment.getPaymentId());
    }

    @Transactional
    protected void updatePassengerSeats(Booking booking) {
        // Cập nhật trạng thái ghế từ PENDING_PAYMENT thành OCCUPIED khi thanh toán thành công
        for (Passenger passenger : booking.getPassengers()) {
            if (passenger.getSeat() != null && passenger.getSeat().getStatus() == SeatStatus.PENDING_PAYMENT) {
                passenger.getSeat().setStatus(SeatStatus.OCCUPIED);
                seatRepository.save(passenger.getSeat());
                log.debug("Updated seat {} status to OCCUPIED for passenger {}",
                        passenger.getSeat().getSeatNumber(), passenger.getFirstName());
            }
        }
    }

    @Transactional
    public void processRefundForCancelledBooking(Booking booking, String reason) {
        log.info("Processing refund for cancelled booking {}: {}", booking.getBookingId(), reason);

        if (booking.getPayment() == null) {
            log.info("No payment found for booking {}, skipping refund", booking.getBookingId());
            return;
        }

        Payment payment = booking.getPayment();

        // Chỉ xử lý refund cho payment đã hoàn thành
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            try {
                // Đánh dấu payment là REFUNDED
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);

                // TODO: Implement actual refund logic với PayPal/VNPay/etc.
                // Hiện tại chỉ log và đánh dấu đã refund
                log.info("Marked payment {} as refunded for booking {}", payment.getPaymentId(), booking.getBookingId());

                // Gửi email thông báo refund
                if (booking.getUserId() != null) {
                    sendRefundNotificationEmail(booking, reason);
                }

            } catch (Exception e) {
                log.error("Failed to process refund for booking {}: {}", booking.getBookingId(), e.getMessage(), e);
                // Có thể throw exception hoặc xử lý graceful
            }
        } else {
            log.info("Payment {} for booking {} is not completed (status: {}), no refund needed",
                    payment.getPaymentId(), booking.getBookingId(), payment.getStatus());
        }
    }

    private void sendRefundNotificationEmail(Booking booking, String reason) {
        try {
            String email = booking.getUserId().getEmail();
            String subject = "Thông báo: Hoàn tiền booking đã hủy";
            String body = String.format(
                    "<h3>Xin chào %s</h3>" +
                            "<p>Mã đặt vé: <strong>%s</strong></p>" +
                            "<p>Booking của bạn đã được hoàn tiền vì: <strong>%s</strong></p>" +
                            "<p>Số tiền đã hoàn: $%.2f</p>" +
                            "<p>Quy trình hoàn tiền có thể mất 3-5 ngày làm việc tùy thuộc vào phương thức thanh toán.</p>" +
                            "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.</p>",
                    booking.getUserId().getLastName(),
                    booking.getBookingCode(),
                    reason,
                    booking.getTotalAmount()
            );

            emailService.sendEmail(email, subject, body);
            log.info("Refund notification email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send refund notification email for booking {}: {}", booking.getBookingId(), e.getMessage());
        }
    }
}
