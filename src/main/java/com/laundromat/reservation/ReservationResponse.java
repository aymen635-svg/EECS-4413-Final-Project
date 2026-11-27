package com.laundromat.smart_laundromat.reservation;
import java.time.LocalDateTime;

public record ReservationResponse(
    Long reservationId,
    Long userId,
    Long machineId,
    String machineNumber,
    LocalDateTime startTime,
    LocalDateTime endTime,
    LocalDateTime createdAt,
    Reservation.ReservationStatus status
) {

    public ReservationResponse(Reservation r) {
        this(
            r.getReservationId(),
            r.getUserId(),
            r.getMachine().getMachineId(),
            r.getMachine().getMachineNumber(),
            r.getStartTime(),
            r.getEndTime(),
            r.getCreatedAt(),
            r.getStatus()
        );
    }
}