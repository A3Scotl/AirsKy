package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.response.PaymentResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.PaymentStatus;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.PaymentMapper;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Passenger;
import iuh.fit.airsky.model.Payment;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.repository.PaymentRepository;
import iuh.fit.airsky.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final BookingRepository bookingRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, PaymentMapper paymentMapper, BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Creating new payment for booking ID: {}", request.getBookingId());
        Payment payment = paymentMapper.toEntity(request);
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + request.getBookingId()));
        payment.setBooking(booking);

        // Confirm booking if payment is successful and within 15 minutes
        if (booking.getStatus() == BookingStatus.PENDING &&
                Duration.between(booking.getHoldTime(), LocalDateTime.now()).toMinutes() <= 15 &&
                request.getStatus() == PaymentStatus.SUCCESS) {
            booking.setStatus(BookingStatus.CONFIRMED);
            for (Passenger passenger : booking.getPassengers()) {
                if (passenger.getSeat() != null) {
                    passenger.getSeat().setStatus(SeatStatus.BOOKED);
                }
            }
            bookingRepository.save(booking);
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created with ID: {}", saved.getPaymentId());
        return paymentMapper.toResponseDTO(saved);
    }

    @Override
    public PaymentResponse updatePayment(Long id, PaymentRequest request) {
        log.info("Updating payment with ID: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id " + id));
        payment.setAmount(request.getAmount());
        payment.setPaymentDate(request.getPaymentDate());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(request.getStatus());
        Payment updated = paymentRepository.save(payment);
        log.info("Payment updated with ID: {}", updated.getPaymentId());
        return paymentMapper.toResponseDTO(updated);
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
    public void delete(Long id) {
        log.info("Deleting payment with ID: {}", id);
        if (!paymentRepository.existsById(id)) {
            log.warn("Payment not found for delete: {}", id);
            throw new ResourceNotFoundException("Payment not found with id " + id);
        }
        paymentRepository.deleteById(id);
        log.info("Payment deleted: {}", id);
    }
}