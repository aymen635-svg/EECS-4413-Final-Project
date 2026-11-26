package com.laundromat.dto;

/**
 * Body of POST /api/payments. The amount is NOT part of the request; the server
 * calculates it. paymentToken is a simulated test token such as "tok_visa".
 */
public record PaymentRequest(Long reservationId, String paymentToken) {
}
