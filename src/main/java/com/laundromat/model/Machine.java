
package com.laundromat.model;

import jakarta.persistence.*;

@Entity
@Table(name = "machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "machine_id")
    private Long id;

    @Column(name = "machine_number", nullable = false, unique = true)
    private String machineNumber;

    @Column(name = "machine_type", nullable = false)
    private String machineType;

    @Column(nullable = false)
    private String status = "AVAILABLE";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Machine() {
    }

    public Machine(String machineNumber, String machineType) {
        this.machineNumber = machineNumber;
        this.machineType = machineType;
    }

    public Long getId() {
        return id;
    }

    public String getMachineNumber() {
        return machineNumber;
    }

    public String getMachineType() {
        return machineType;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }

    public void setMachineNumber(String machineNumber) {
        this.machineNumber = machineNumber;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
