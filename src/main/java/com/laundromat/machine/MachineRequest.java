package com.laundromat.smart_laundromat.machine;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MachineRequest(

    @NotBlank
    @Size(max = 30)
    String machineNumber,

    @NotNull
    Machine.MachineType machineType,

    @NotNull
    Machine.MachineStatus status,

    @NotNull
    Boolean isActive

) {}
