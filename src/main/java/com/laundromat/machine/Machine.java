package com.laundromat.smart_laundromat.machine;

import jakarta.persistence.*;

@Entity
@Table(name = "machines")
public class Machine {

    public enum MachineStatus {
        AVAILABLE,
        IN_USE,
        OUT_OF_SERVICE,
        MAINTENANCE
    }

    public enum MachineType {
        WASHER,
        DRYER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long machineId;

    @Column(nullable = false, unique = true, length = 30)
    private String machineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MachineType machineType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MachineStatus status = MachineStatus.AVAILABLE;

    @Column(nullable = false)
    private boolean active = true;

    protected Machine() {}

    public Machine(
            String machineNumber,
            MachineType machineType,
            MachineStatus status,
            boolean active) {
        this.machineNumber = machineNumber;
        this.machineType = machineType;
        this.status = status;
        this.active = active;
    }

    public Long getMachineId() {
        return machineId;
    }

    public String getMachineNumber() {
        return machineNumber;
    }

    public void setMachineNumber(String machineNumber) {
        this.machineNumber = machineNumber;
    }

    public MachineType getMachineType() {
        return machineType;
    }

    public void setMachineType(MachineType machineType) {
        this.machineType = machineType;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public void setStatus(MachineStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}