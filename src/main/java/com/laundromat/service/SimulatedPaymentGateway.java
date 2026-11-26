package com.laundromat.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Simulated payment provider. Customers pick a test token (named after Stripe's
 * test tokens) instead of entering card details, so no card data ever reaches
 * the server. The outcome is decided here on the server, not by a client flag.
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    // token -> decline reason (null means the charge succeeds)
    private static final Map<String, String> TEST_TOKENS = Map.of(
            "tok_visa", "",
            "tok_mastercard", "",
            "tok_chargeDeclined", "Your card was declined.",
            "tok_chargeDeclinedInsufficientFunds", "Your card has insufficient funds.",
            "tok_chargeDeclinedExpiredCard", "Your card has expired.");

    @Override
    public String method() {
        return "SIMULATED";
    }

    @Override
    public boolean supports(String paymentToken) {
        return paymentToken != null && TEST_TOKENS.containsKey(paymentToken);
    }

    @Override
    public Result charge(BigDecimal amount, String currency, String paymentToken, String idempotencyKey) {
        if (!supports(paymentToken)) {
            throw new IllegalArgumentException("Unsupported test payment token");
        }
        String transactionRef = "sim_ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String declineReason = TEST_TOKENS.get(paymentToken);
        if (declineReason.isEmpty()) {
            return Result.success(transactionRef);
        }
        return Result.declined(transactionRef, declineReason);
    }
}
