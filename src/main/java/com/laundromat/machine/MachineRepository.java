package com.laundromat.smart_laundromat.machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    boolean existsByMachineNumber(String machineNumber);
    List<Machine> findByActiveTrue();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Machine m WHERE m.machineId = :id")
    Optional<Machine> findByIdForUpdate(@Param("id") Long id);
}
