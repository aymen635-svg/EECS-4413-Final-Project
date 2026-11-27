package com.laundromat.smart_laundromat.availabilty;
import com.laundromat.smart_laundromat.machine.*;
import com.laundromat.smart_laundromat.reservation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    private final MachineRepository machines;
    private final ReservationRepository reservations;

    public AvailabilityService(MachineRepository machines, ReservationRepository reservations) {
        this.machines = machines;
        this.reservations = reservations;
    }

    public List<Machine> available(LocalDateTime start, LocalDateTime end,Machine.MachineType category) {
         if (start == null || end == null || !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }

        Set<Long> reservedMachineIds = reservations.findByStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                List.of(Reservation.ReservationStatus.PENDING_PAYMENT,Reservation.ReservationStatus.CONFIRMED),end,start)
            .stream().map(reservation ->reservation.getMachine().getMachineId()).collect(Collectors.toSet());

        return machines.findByActiveTrue().stream()
            .filter(machine ->
                machine.getStatus() != Machine.MachineStatus.OUT_OF_SERVICE)
            .filter(machine ->
                machine.getStatus() != Machine.MachineStatus.IN_USE)
            .filter(machine ->
                category == null || machine.getMachineType() == category)
            .filter(machine ->
                !reservedMachineIds.contains(machine.getMachineId()))
            .toList();
    }
}
