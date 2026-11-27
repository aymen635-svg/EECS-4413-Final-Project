package com.laundromat.smart_laundromat.machine;
import jakarta.validation.constraints.NotNull;

public record MachineStatusRequest(
    @NotNull Machine.MachineStatus status
) {}
