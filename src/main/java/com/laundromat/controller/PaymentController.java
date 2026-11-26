package com.laundromat.controller;

import com.laundromat.dto.PaymentQuote;
import com.laundromat.dto.PaymentRequest;
import com.laundromat.dto.PaymentResponse;
import com.laundromat.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Simulated payment API. Every path is under /api so AuthInterceptor requires
 * sign-in, and /api/admin/** is admin-only.
 */
@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Pay for a reservation.
     * 201 = payment succeeded, 402 = payment declined (attempt is still recorded),
     * replaying an Idempotency-Key returns the original result with header Idempotent-Replayed: true.
     */
    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> pay(@RequestBody(required = false) PaymentRequest request,
                                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                               HttpSession session) {
        PaymentService.PaymentOutcome outcome = paymentService.pay(userId(session), request, idempotencyKey);
        PaymentResponse payment = outcome.payment();

        HttpStatus status = outcome.succeeded() ? HttpStatus.CREATED : HttpStatus.PAYMENT_REQUIRED;
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status)
                .location(URI.create("/api/payments/" + payment.id()));
        if (outcome.replayed()) {
            response.header("Idempotent-Replayed", "true");
        }
        return response.body(payment);
    }

    /** Signed-in customer's own payment history. */
    @GetMapping("/payments")
    public List<PaymentResponse> myPayments(HttpSession session) {
        return paymentService.getMyPayments(userId(session));
    }

    /** Amount due for a reservation (calculated by the server). */
    @GetMapping("/payments/quote")
    public PaymentQuote quote(@RequestParam Long reservationId, HttpSession session) {
        return paymentService.quote(reservationId, userId(session), role(session));
    }

    @GetMapping("/payments/{paymentId}")
    public PaymentResponse getPayment(@PathVariable Long paymentId, HttpSession session) {
        return paymentService.getPayment(paymentId, userId(session), role(session));
    }

    /** All payment attempts for one reservation (owner or admin). */
    @GetMapping("/reservations/{reservationId}/payments")
    public List<PaymentResponse> reservationPayments(@PathVariable Long reservationId, HttpSession session) {
        return paymentService.getPaymentsForReservation(reservationId, userId(session), role(session));
    }

    /** Admin: inspect payments, optionally ?status=SUCCEEDED|FAILED|PENDING|REFUNDED. */
    @GetMapping("/admin/payments")
    public List<PaymentResponse> allPayments(@RequestParam(required = false) String status) {
        return paymentService.getAllPayments(status);
    }

    private static Long userId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    private static String role(HttpSession session) {
        return (String) session.getAttribute("role");
    }
}
