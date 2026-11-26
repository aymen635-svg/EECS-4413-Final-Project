
package com.laundromat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod = "SIMULATED";

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "PENDING";

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    // Client-supplied key so a retried request does not charge twice
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    // Reference returned by the (simulated) payment gateway, e.g. "sim_ch_..."
    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    // Why the gateway declined the payment (null when it succeeded)
    @Column(name = "failure_reason")
    private String failureReason;

    public Payment() {
    }

    public Payment(
            Reservation reservation,
            BigDecimal amount,
            String paymentMethod) {

        this.reservation = reservation;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = "PENDING";
        this.paymentDate = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
