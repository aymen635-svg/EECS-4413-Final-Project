
package com.laundromat.smart_laundromat.reservation;
import com.laundromat.smart_laundromat.machine.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservations;
    private final MachineRepository machines;
    private final CurrentUserProvider currentUser;

    private static final List<Reservation.ReservationStatus> BLOCKING = List.of(Reservation.ReservationStatus.PENDING_PAYMENT, Reservation.ReservationStatus.CONFIRMED);

    public ReservationService(ReservationRepository reservations, MachineRepository machines, CurrentUserProvider currentUser) {
        this.reservations = reservations;
        this.machines = machines;
        this.currentUser = currentUser;
    }

    // Create a reservation
    @Transactional
    public Reservation create(ReservationRequest request) {

        Long userId = currentUser.currentUserId();

        if (userId == null) {
            throw new SecurityException("Login required");
        }

        Machine machine = machines.findByIdForUpdate(request.machineId())
            .orElseThrow(() -> new IllegalArgumentException("Machine not found"));

        if (!machine.isActive() || machine.getStatus() == Machine.MachineStatus.OUT_OF_SERVICE|| machine.getStatus() == Machine.MachineStatus.IN_USE) {
             throw new IllegalStateException("Machine is not bookable");
        }

        List<Reservation> conflicts = reservations.findByMachine_MachineIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                machine.getMachineId(),
                BLOCKING,
                request.endTime(),
                request.startTime()
            );

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                "Machine is already reserved for that time"
            );
        }

        Reservation reservation = new Reservation(userId, machine,request.startTime(), request.endTime());

        return reservations.save(reservation);
    }

    // Retrieve reservations belonging to logged-in customer
    public List<Reservation> mine() {

        Long userId = currentUser.currentUserId();

        if (userId == null) {
            throw new SecurityException("Login required");
        }

        return reservations.findByUserIdOrderByStartTimeDesc(userId);
    }

    // Retrieve reservation if user owns it or is admin
    public Reservation getOwned(Long id) {

        Reservation reservation = find(id);

        if (!currentUser.isAdmin() && !reservation.getUserId().equals(currentUser.currentUserId())) {

            throw new SecurityException("You cannot access this reservation");
        }

        return reservation;
    }

    // Cancel reservation
    @Transactional
    public Reservation cancel(Long id) {

        Reservation reservation = getOwned(id);

        reservation.cancel();

        return reservations.save(reservation);
    }

    // Find reservation with Machine loaded
    private Reservation find(Long id) {
      return reservations.findByIdWithMachine(id).orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
    }
}
