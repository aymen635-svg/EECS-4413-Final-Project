package com.laundromat.service;

import java.math.BigDecimal;

/**
 * Adapter between the payment module and whatever actually charges the customer.
 * The app uses {@link SimulatedPaymentGateway}; a Stripe test-mode adapter could
 * implement this same interface without changing PaymentService.
 */
public interface PaymentGateway {

    /** Value stored in payments.payment_method (must match chk_payment_method). */
    String method();

    /** True if the token is one this gateway accepts. Never log the token itself. */
    boolean supports(String paymentToken);

    Result charge(BigDecimal amount, String currency, String paymentToken, String idempotencyKey);

    record Result(boolean succeeded, String transactionRef, String failureReason) {

        public static Result success(String transactionRef) {
            return new Result(true, transactionRef, null);
        }

        public static Result declined(String transactionRef, String reason) {
            return new Result(false, transactionRef, reason);
        }
    }
}
