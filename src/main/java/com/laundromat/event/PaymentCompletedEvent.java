package com.laundromat.event;

import java.math.BigDecimal;

/**
 * Published after a payment succeeds and its reservation is confirmed.
 * Listen with @TransactionalEventListener (e.g. in the notification module) so the
 * handler only runs once the payment has actually been committed.
 */
public record PaymentCompletedEvent(Long paymentId, Long reservationId, Long userId, BigDecimal amount) {
}
