
package com.laundromat.repository;

import com.laundromat.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    // Find a machine by its unique number
    Optional<Machine> findByMachineNumber(String machineNumber);

    // Find machines by type (WASHER or DRYER)
    List<Machine> findByMachineType(String machineType);

    // Find all active machines
    List<Machine> findByActiveTrue();

    // Find active machines by type and status
    List<Machine> findByMachineTypeAndStatusAndActiveTrue(
            String machineType,
            String status);

    // Check if a machine number already exists
    boolean existsByMachineNumber(String machineNumber);
}
