package com.laundromat.dto;

import com.laundromat.model.Payment;
import com.laundromat.model.Reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * What the API returns for a payment. Built from the entity inside a transaction
 * so lazy relations are loaded, and so no internal fields leak by accident.
 */
public record PaymentResponse(
        Long id,
        Long reservationId,
        String reservationStatus,
        String machineNumber,
        String customerEmail,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String paymentStatus,
        LocalDateTime paymentDate,
        String transactionRef,
        String failureReason) {

    public static PaymentResponse from(Payment payment, String currency) {
        Reservation reservation = payment.getReservation();
        return new PaymentResponse(
                payment.getId(),
                reservation.getId(),
                reservation.getStatus(),
                reservation.getMachine().getMachineNumber(),
                reservation.getUser().getEmail(),
                payment.getAmount(),
                currency,
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getPaymentDate(),
                payment.getTransactionRef(),
                payment.getFailureReason());
    }
}
