
package com.laundromat.repository;

import com.laundromat.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    // Get all payment attempts for a reservation
    List<Payment> findByReservationId(Long reservationId);

    // Get all payment attempts for a reservation, newest first
    List<Payment> findByReservationIdOrderByIdDesc(Long reservationId);

    // Get payments with a specific status
    List<Payment> findByPaymentStatus(String paymentStatus);

    // Check if a reservation has a successful payment
    boolean existsByReservationIdAndPaymentStatus(
            Long reservationId,
            String paymentStatus);

    // Find the successful payment for a reservation
    Optional<Payment> findByReservationIdAndPaymentStatus(
            Long reservationId,
            String paymentStatus);

    // Look up an earlier attempt sent with the same idempotency key
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    // All payments made by one customer, newest first
    List<Payment> findByReservationUserIdOrderByIdDesc(Long userId);

    // Admin listings, newest first
    List<Payment> findAllByOrderByIdDesc();

    List<Payment> findByPaymentStatusOrderByIdDesc(String paymentStatus);
}
