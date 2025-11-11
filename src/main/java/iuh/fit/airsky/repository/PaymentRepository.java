package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    @Override
    @EntityGraph(attributePaths = {"booking"})
    Page<Payment> findAll(Pageable pageable); 
    Optional<Payment> findByBooking_BookingId(Long bookingId);

    boolean existsByBooking_BookingId(Long bookingId);

    /**
     * Tìm tất cả các thanh toán của một booking, sắp xếp theo ngày thanh toán giảm dần
     */
    @EntityGraph(attributePaths = {"booking"})
    List<Payment> findByBooking_BookingIdOrderByPaymentDateDesc(Long bookingId);

    Optional<Payment> findByTransactionIdAndBooking_BookingId(String transactionId, Long bookingId);

    Optional<Payment> findByBooking_BookingCode(String bookingCode);

    Optional<Payment> findByTransactionId(String orderCode);
}