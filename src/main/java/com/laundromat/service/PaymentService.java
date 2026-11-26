package com.laundromat.service;

import com.laundromat.dto.PaymentQuote;
import com.laundromat.dto.PaymentRequest;
import com.laundromat.dto.PaymentResponse;
import com.laundromat.event.PaymentCompletedEvent;
import com.laundromat.model.Payment;
import com.laundromat.model.Reservation;
import com.laundromat.repository.PaymentRepository;
import com.laundromat.repository.ReservationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Simulated payments for reservations (UC4).
 *
 * Rules:
 *  - Only the customer who owns a reservation can pay for it.
 *  - The amount is always calculated here (PricingService), never sent by the client.
 *  - A reservation is confirmed only when its payment succeeds; failed attempts are
 *    recorded and the customer may retry until the pending hold expires.
 *  - The reservation row is locked while paying, so two simultaneous requests cannot
 *    both succeed. The unique index uq_successful_payment_per_reservation is the backstop.
 *  - An optional Idempotency-Key makes retries of the same request safe.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public static final Set<String> PAYMENT_STATUSES = Set.of("PENDING", "SUCCEEDED", "FAILED", "REFUNDED");

    @PersistenceContext
    private EntityManager entityManager;

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PricingService pricingService;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher events;
    private final long holdMinutes;
    private final String currency;

    public PaymentService(PaymentRepository paymentRepository,
                          ReservationRepository reservationRepository,
                          PricingService pricingService,
                          PaymentGateway paymentGateway,
                          ApplicationEventPublisher events,
                          @Value("${app.payment.hold-minutes:15}") long holdMinutes,
                          @Value("${app.payment.currency:CAD}") String currency) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.pricingService = pricingService;
        this.paymentGateway = paymentGateway;
        this.events = events;
        this.holdMinutes = holdMinutes;
        this.currency = currency;
    }

    /** Result of a payment request; replayed = an earlier attempt with the same idempotency key. */
    public record PaymentOutcome(PaymentResponse payment, boolean replayed) {
        public boolean succeeded() {
            return "SUCCEEDED".equals(payment.paymentStatus());
        }
    }

    // noRollbackFor: when a hold has expired we mark the reservation EXPIRED and then
    // reject the request; that status change must still be saved. Every other
    // ResponseStatusException below is thrown before anything is written.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public PaymentOutcome pay(Long userId, PaymentRequest request, String idempotencyKey) {
        validate(request, idempotencyKey);

        // SELECT ... FOR UPDATE: concurrent payments for the same reservation wait here
        Reservation reservation = entityManager.find(
                Reservation.class, request.reservationId(), LockModeType.PESSIMISTIC_WRITE);
        if (reservation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        }
        if (!reservation.getUser().getId().equals(userId)) {
            log.warn("User {} tried to pay for reservation {} they do not own", userId, reservation.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only pay for your own reservations");
        }

        // Checked after taking the lock so a duplicate request sees the committed first attempt
        if (idempotencyKey != null) {
            Payment previous = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (previous != null) {
                if (!previous.getReservation().getId().equals(reservation.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency key was already used for a different reservation");
                }
                return new PaymentOutcome(PaymentResponse.from(previous, currency), true);
            }
        }

        ensurePayable(reservation);

        BigDecimal amount = pricingService.priceFor(reservation);
        PaymentGateway.Result result = paymentGateway.charge(amount, currency, request.paymentToken(), idempotencyKey);

        Payment payment = new Payment(reservation, amount, paymentGateway.method());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setTransactionRef(result.transactionRef());

        if (result.succeeded()) {
            payment.setPaymentStatus("SUCCEEDED");
            reservation.setStatus("CONFIRMED");
        } else {
            payment.setPaymentStatus("FAILED");
            payment.setFailureReason(result.failureReason());
        }
        // Flush now so a constraint violation surfaces here (mapped to 409), not at commit
        payment = paymentRepository.saveAndFlush(payment);

        if (result.succeeded()) {
            log.info("Payment {} succeeded for reservation {} ({} {})",
                    payment.getId(), reservation.getId(), amount, currency);
            events.publishEvent(new PaymentCompletedEvent(payment.getId(), reservation.getId(), userId, amount));
        } else {
            log.info("Payment {} declined for reservation {}: {}",
                    payment.getId(), reservation.getId(), result.failureReason());
        }
        return new PaymentOutcome(PaymentResponse.from(payment, currency), false);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId, Long userId, String role) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        checkCanView(payment.getReservation(), userId, role);
        return PaymentResponse.from(payment, currency);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForReservation(Long reservationId, Long userId, String role) {
        Reservation reservation = findReservation(reservationId);
        checkCanView(reservation, userId, role);
        return paymentRepository.findByReservationIdOrderByIdDesc(reservationId).stream()
                .map(p -> PaymentResponse.from(p, currency))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(Long userId) {
        return paymentRepository.findByReservationUserIdOrderByIdDesc(userId).stream()
                .map(p -> PaymentResponse.from(p, currency))
                .toList();
    }

    /** Admin view of all payments, optionally filtered by status. */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments(String status) {
        List<Payment> payments;
        if (status == null || status.isBlank()) {
            payments = paymentRepository.findAllByOrderByIdDesc();
        } else {
            String normalized = status.trim().toUpperCase();
            if (!PAYMENT_STATUSES.contains(normalized)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Status must be one of PENDING, SUCCEEDED, FAILED, REFUNDED");
            }
            payments = paymentRepository.findByPaymentStatusOrderByIdDesc(normalized);
        }
        return payments.stream().map(p -> PaymentResponse.from(p, currency)).toList();
    }

    /** Amount due for a reservation, so the UI can show it before the customer pays. */
    @Transactional(readOnly = true)
    public PaymentQuote quote(Long reservationId, Long userId, String role) {
        Reservation reservation = findReservation(reservationId);
        checkCanView(reservation, userId, role);
        boolean pending = "PENDING_PAYMENT".equals(reservation.getStatus());
        return new PaymentQuote(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getMachine().getMachineNumber(),
                reservation.getMachine().getMachineType(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                pricingService.priceFor(reservation),
                currency,
                pending ? payBefore(reservation) : null);
    }

    /**
     * For the booking module: call this when a confirmed reservation is cancelled.
     * Marks its successful payment as REFUNDED. Returns false if nothing was paid.
     */
    @Transactional
    public boolean refundReservation(Long reservationId) {
        Payment payment = paymentRepository.findByReservationIdAndPaymentStatus(reservationId, "SUCCEEDED")
                .orElse(null);
        if (payment == null) {
            return false;
        }
        payment.setPaymentStatus("REFUNDED");
        log.info("Payment {} refunded for reservation {}", payment.getId(), reservationId);
        return true;
    }

    /** Latest time a pending reservation can still be paid for. */
    public LocalDateTime payBefore(Reservation reservation) {
        LocalDateTime holdEnds = reservation.getCreatedAt().plusMinutes(holdMinutes);
        return holdEnds.isBefore(reservation.getStartTime()) ? holdEnds : reservation.getStartTime();
    }

    private void validate(PaymentRequest request, String idempotencyKey) {
        if (request == null || request.reservationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reservationId is required");
        }
        String token = request.paymentToken();
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentToken is required");
        }
        if (looksLikeCardNumber(token)) {
            // Deliberately not logged: the value could be a real card number
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Card numbers are not accepted. Use a test payment token such as tok_visa");
        }
        if (!paymentGateway.supports(token)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported test payment token");
        }
        if (idempotencyKey != null && (idempotencyKey.isBlank() || idempotencyKey.length() > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must be 1 to 100 characters");
        }
    }

    private static boolean looksLikeCardNumber(String value) {
        String digits = value.replaceAll("[\\s-]", "");
        return digits.matches("\\d{12,19}");
    }

    private void ensurePayable(Reservation reservation) {
        switch (reservation.getStatus()) {
            case "PENDING_PAYMENT" -> {
                if (LocalDateTime.now().isAfter(payBefore(reservation))) {
                    // Releases the slot (the overlap constraint only covers PENDING_PAYMENT/CONFIRMED)
                    reservation.setStatus("EXPIRED");
                    log.info("Reservation {} expired before payment", reservation.getId());
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "The payment window for this reservation has expired. Please book again");
                }
            }
            case "CONFIRMED", "COMPLETED" ->
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation is already paid");
            default ->
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Reservation cannot be paid because it is " + reservation.getStatus());
        }
    }

    private Reservation findReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    }

    private void checkCanView(Reservation reservation, Long userId, String role) {
        if (!"ADMIN".equals(role) && !reservation.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own payments");
        }
    }
}
