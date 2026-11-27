
package com.laundromat.smart_laundromat.reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    // Get customer's reservations with Machine loaded.
    @EntityGraph(attributePaths = {"machine"})
    List<Reservation> findByUserIdOrderByStartTimeDesc(Long userId);

    // Get one reservation with Machine loaded.
    @Query("""
        SELECT r
        FROM Reservation r
        JOIN FETCH r.machine
        WHERE r.reservationId = :id
    """)
    Optional<Reservation> findByIdWithMachine(@Param("id") Long id);

      // Find overlapping reservations across all machines.
      List<Reservation>
      findByStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
              List<Reservation.ReservationStatus> statuses,
              LocalDateTime end,
              LocalDateTime start
      );

    // Find overlapping reservations for a specific machine.
    List<Reservation>
    findByMachine_MachineIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            Long machineId,
            List<Reservation.ReservationStatus> statuses,
            LocalDateTime end,
            LocalDateTime start
    );


}
