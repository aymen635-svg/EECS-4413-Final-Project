
package com.laundromat.controller;

import com.laundromat.model.Machine;
import com.laundromat.model.Reservation;
import com.laundromat.model.User;
import com.laundromat.model.Payment;

import com.laundromat.repository.MachineRepository;
import com.laundromat.repository.ReservationRepository;
import com.laundromat.repository.UserRepository;
import com.laundromat.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DatabaseIntegrationTest {

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // Test 1: Machine persistence
    @Test
    void testMachinePersistence() {

        Machine machine = new Machine("TEST-JPA-001", "WASHER");

        Machine saved = machineRepository.saveAndFlush(machine);

        assertNotNull(saved.getId());

        Machine found = machineRepository
                .findById(saved.getId())
                .orElseThrow();

        assertEquals("TEST-JPA-001", found.getMachineNumber());
        assertEquals("AVAILABLE", found.getStatus());
    }

    // Test 2: Reservation persistence and overlap detection
    @Test
    void testReservationPersistenceAndOverlap() {

        User user = userRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        Machine machine = machineRepository.saveAndFlush(
                new Machine("TEST-RES-001", "WASHER"));

        LocalDateTime start = LocalDateTime.of(2026, 12, 20, 10, 0);

        LocalDateTime end = start.plusHours(2);

        Reservation reservation = reservationRepository.saveAndFlush(
                new Reservation(user, machine, start, end));

        assertNotNull(reservation.getId());

        Reservation saved = reservationRepository
                .findById(reservation.getId())
                .orElseThrow();

        assertEquals(machine.getId(), saved.getMachine().getId());
        assertEquals("PENDING_PAYMENT", saved.getStatus());

        boolean overlapping = reservationRepository.existsOverlappingReservation(
                machine.getId(),
                start.plusHours(1),
                end.plusHours(1));

        assertTrue(overlapping);

        boolean nonOverlapping = reservationRepository.existsOverlappingReservation(
                machine.getId(),
                end,
                end.plusHours(1));

        assertFalse(nonOverlapping);
    }

    // Test 3: Payment persistence
    @Test
    void testPaymentPersistence() {

        // Retrieve an existing user
        User user = userRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        // Create a test machine
        Machine machine = machineRepository.saveAndFlush(
                new Machine("TEST-PAY-001", "WASHER"));

        // Create a reservation
        LocalDateTime start = LocalDateTime.of(2026, 12, 22, 10, 0);

        LocalDateTime end = start.plusHours(1);

        Reservation reservation = reservationRepository.saveAndFlush(
                new Reservation(user, machine, start, end));

        // Create a simulated payment
        Payment payment = new Payment(
                reservation,
                new BigDecimal("5.00"),
                "SIMULATED");

        payment.setPaymentStatus("SUCCEEDED");

        // Save the payment
        Payment saved = paymentRepository.saveAndFlush(payment);

        assertNotNull(saved.getId());

        // Retrieve the payment from PostgreSQL
        Payment found = paymentRepository
                .findById(saved.getId())
                .orElseThrow();

        // Verify the payment details
        assertEquals(
                0,
                new BigDecimal("5.00").compareTo(found.getAmount()));

        assertEquals("SUCCEEDED", found.getPaymentStatus());

        assertEquals(
                reservation.getId(),
                found.getReservation().getId());

        // Verify successful payment lookup
        assertTrue(
                paymentRepository.existsByReservationIdAndPaymentStatus(
                        reservation.getId(),
                        "SUCCEEDED"));
    }

}
