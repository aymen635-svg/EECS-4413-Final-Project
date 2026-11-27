package com.laundromat.smart_laundromat.reservation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDateTime;

public record ReservationRequest(@NotNull Long machineId,@NotNull @Future LocalDateTime startTime,@NotNull @Future LocalDateTime endTime) {

    @AssertTrue(message = "endTime must be after startTime")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
