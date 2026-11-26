package com.laundromat.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Amount due for a reservation, shown to the customer before they pay. */
public record PaymentQuote(
        Long reservationId,
        String reservationStatus,
        String machineNumber,
        String machineType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal amount,
        String currency,
        LocalDateTime payBefore) {
}
