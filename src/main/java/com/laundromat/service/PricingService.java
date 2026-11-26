package com.laundromat.service;

import com.laundromat.model.Reservation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Calculates what a reservation costs. Prices are always computed on the server
 * so a client can never choose its own amount.
 */
@Service
public class PricingService {

    private final BigDecimal washerPerHour;
    private final BigDecimal dryerPerHour;

    public PricingService(@Value("${app.pricing.washer-per-hour:5.00}") BigDecimal washerPerHour,
                          @Value("${app.pricing.dryer-per-hour:4.00}") BigDecimal dryerPerHour) {
        this.washerPerHour = washerPerHour;
        this.dryerPerHour = dryerPerHour;
    }

    public BigDecimal priceFor(Reservation reservation) {
        long minutes = Duration.between(reservation.getStartTime(), reservation.getEndTime()).toMinutes();
        if (minutes <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation has an invalid time slot");
        }

        BigDecimal hourlyRate = hourlyRate(reservation.getMachine().getMachineType());
        return hourlyRate
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal hourlyRate(String machineType) {
        if ("WASHER".equals(machineType)) {
            return washerPerHour;
        }
        if ("DRYER".equals(machineType)) {
            return dryerPerHour;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Unknown machine type");
    }
}
