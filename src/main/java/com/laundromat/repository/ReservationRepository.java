
package com.laundromat.repository;

import com.laundromat.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    // Find all reservations belonging to a customer
    List<Reservation> findByUserId(Long userId);

    // Find reservations for a specific machine
    List<Reservation> findByMachineId(Long machineId);

    // Check whether a machine has an overlapping reservation
    @Query("""
                SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
                FROM Reservation r
                WHERE r.machine.id = :machineId
                AND r.status IN ('PENDING_PAYMENT', 'CONFIRMED')
                AND r.startTime < :endTime
                AND r.endTime > :startTime
            """)
    boolean existsOverlappingReservation(
            @Param("machineId") Long machineId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
