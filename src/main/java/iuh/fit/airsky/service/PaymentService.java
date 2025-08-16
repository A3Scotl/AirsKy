package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.response.PaymentResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest request);
    PaymentResponse updatePayment(Long id, PaymentRequest request);
    Optional<PaymentResponse> findById(Long id);
    PageResponse<PaymentResponse> findAll(Pageable pageable);
    void delete(Long id);
}