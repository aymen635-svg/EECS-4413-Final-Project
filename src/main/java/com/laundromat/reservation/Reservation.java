package com.laundromat.smart_laundromat.reservation;
import com.laundromat.smart_laundromat.machine.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    public enum ReservationStatus {
        PENDING_PAYMENT,
        CONFIRMED,
        CANCELLED,
        COMPLETED,
        EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    protected Reservation() {}

    public Reservation(Long userId, Machine machine, LocalDateTime startTime,LocalDateTime endTime) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (machine == null) {
            throw new IllegalArgumentException("Machine cannot be null");
        }

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Reservation times cannot be null");
        }

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        this.userId = userId;
        this.machine = machine;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = ReservationStatus.PENDING_PAYMENT;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void confirm() {
        if (this.status != ReservationStatus.PENDING_PAYMENT) {
            throw new IllegalStateException(
                    "Only pending reservations can be confirmed");
        }

        this.status = ReservationStatus.CONFIRMED;
    }

    /*public void cancel() {
        if (this.status == ReservationStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed reservations cannot be cancelled");
        }

        this.status = ReservationStatus.CANCELLED;
    }*/
    public void cancel() {
        if (this.status != ReservationStatus.PENDING_PAYMENT
                && this.status != ReservationStatus.CONFIRMED) {
    
            throw new IllegalStateException(
                "Reservation cannot be cancelled"
            );
        }
    
        this.status = ReservationStatus.CANCELLED;
    }

    public void complete() {
        if (this.status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only confirmed reservations can be completed");
        }

        this.status = ReservationStatus.COMPLETED;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Long getUserId() {
        return userId;
    }

    public Machine getMachine() {
        return machine;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

}
