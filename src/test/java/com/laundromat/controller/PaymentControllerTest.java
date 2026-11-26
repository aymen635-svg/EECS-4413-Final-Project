package com.laundromat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laundromat.model.Machine;
import com.laundromat.model.Reservation;
import com.laundromat.model.User;
import com.laundromat.repository.MachineRepository;
import com.laundromat.repository.PaymentRepository;
import com.laundromat.repository.ReservationRepository;
import com.laundromat.repository.UserRepository;
import com.laundromat.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Payment API tests. Reservations are created directly through the repositories
 * so these tests do not depend on the booking API. Emails and machine numbers are
 * unique per test so the tests can be re-run against a persistent database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository userRepository;
    @Autowired MachineRepository machineRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired PaymentService paymentService;

    record Customer(MockHttpSession session, User user) {}

    // ---------- helpers ----------

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private Customer newCustomer() throws Exception {
        String email = "pay-" + unique() + "@test.com";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Payer\",\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
        return new Customer(login(email, "password123"), userRepository.findByEmail(email).orElseThrow());
    }

    private Reservation newReservation(User user, String machineType, int minutes) {
        Machine machine = machineRepository.save(new Machine("PT-" + unique(), machineType));
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        return reservationRepository.save(new Reservation(user, machine, start, start.plusMinutes(minutes)));
    }

    private ResultActions pay(MockHttpSession session, Long reservationId, String token) throws Exception {
        return mvc.perform(post("/api/payments").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservationId\":" + reservationId + ",\"paymentToken\":\"" + token + "\"}"));
    }

    private String reservationStatus(Long id) {
        return reservationRepository.findById(id).orElseThrow().getStatus();
    }

    // ---------- normal use ----------

    @Test
    void successfulPaymentConfirmsReservationWithServerCalculatedAmount() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "WASHER", 60); // 60 min at 5.00/h

        pay(c.session(), r.getId(), "tok_visa")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.amount").value(5.00))
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.paymentMethod").value("SIMULATED"))
                .andExpect(jsonPath("$.reservationStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.transactionRef").isNotEmpty());

        assertEquals("CONFIRMED", reservationStatus(r.getId()));
    }

    @Test
    void dryerIsPricedPerMinute() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "DRYER", 45); // 45 min at 4.00/h = 3.00

        mvc.perform(get("/api/payments/quote").param("reservationId", r.getId().toString()).session(c.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(3.00))
                .andExpect(jsonPath("$.payBefore").isNotEmpty());
    }

    @Test
    void clientCannotChooseTheAmount() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "WASHER", 60);

        mvc.perform(post("/api/payments").session(c.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":" + r.getId() + ",\"paymentToken\":\"tok_visa\",\"amount\":0.01}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(5.00));
    }

    // ---------- payment failures ----------

    @Test
    void declinedPaymentIsRecordedAndCustomerCanRetry() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "WASHER", 30);

        pay(c.session(), r.getId(), "tok_chargeDeclinedInsufficientFunds")
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Your card has insufficient funds."));

        assertEquals("PENDING_PAYMENT", reservationStatus(r.getId()));

        pay(c.session(), r.getId(), "tok_mastercard")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCEEDED"));

        mvc.perform(get("/api/reservations/" + r.getId() + "/payments").session(c.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].paymentStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$[1].paymentStatus").value("FAILED"));
    }

    @Test
    void cannotPayTwiceForTheSameReservation() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "WASHER", 30);

        pay(c.session(), r.getId(), "tok_visa").andExpect(status().isCreated());
        pay(c.session(), r.getId(), "tok_visa")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Reservation is already paid"));

        assertEquals(1, paymentRepository.findByReservationId(r.getId()).size());
    }

    @Test
    void cannotPayForCancelledReservation() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "WASHER", 30);
        r.setStatus("CANCELLED");
        reservationRepository.save(r);

        pay(c.session(), r.getId(), "tok_visa").andExpect(status().isConflict());
        assertTrue(paymentRepository.findByReservationId(r.getId()).isEmpty());
    }

    @Test
    void latePaymentExpiresReservationInsteadOfConfirmingIt() throws Exception {
        Customer c = newCustomer();
        Machine machine = machineRepository.save(new Machine("PT-" + unique(), "WASHER"));
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Reservation r = new Reservation(c.user(), machine, start, start.plusMinutes(30));
        ReflectionTestUtils.setField(r, "createdAt", LocalDateTime.now().minusMinutes(20)); // hold is 15 min
        r = reservationRepository.save(r);

        pay(c.session(), r.getId(), "tok_visa")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "The payment window for this reservation has expired. Please book again"));

        assertEquals("EXPIRED", reservationStatus(r.getId()));
        assertTrue(paymentRepository.findByReservationId(r.getId()).isEmpty());
    }

    // ---------- concurrency and idempotency ----------

    @Test
    void concurrentPaymentsOnlyOneSucceeds() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "WASHER", 30);

        int attempts = 5;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return pay(c.session(), r.getId(), "tok_visa").andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();

        int created = 0, conflicts = 0;
        for (Future<Integer> f : results) {
            int code = f.get(30, TimeUnit.SECONDS);
            if (code == 201) created++;
            if (code == 409) conflicts++;
        }
        pool.shutdown();

        assertEquals(1, created);
        assertEquals(attempts - 1, conflicts);
        assertEquals(1, paymentRepository.findByReservationId(r.getId()).size());
    }

    @Test
    void retryWithSameIdempotencyKeyReturnsOriginalPayment() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "WASHER", 30);
        String key = UUID.randomUUID().toString();
        String body = "{\"reservationId\":" + r.getId() + ",\"paymentToken\":\"tok_visa\"}";

        String first = mvc.perform(post("/api/payments").session(c.session()).header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andReturn().getResponse().getContentAsString();
        long firstId = json.readTree(first).get("id").asLong();

        mvc.perform(post("/api/payments").session(c.session()).header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(firstId));

        assertEquals(1, paymentRepository.findByReservationId(r.getId()).size());
    }

    @Test
    void idempotencyKeyCannotBeReusedForAnotherReservation() throws Exception {
        Customer c = newCustomer();
        Reservation r1 = newReservation(c.user(), "WASHER", 30);
        Reservation r2 = newReservation(c.user(), "WASHER", 30);
        String key = UUID.randomUUID().toString();

        mvc.perform(post("/api/payments").session(c.session()).header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":" + r1.getId() + ",\"paymentToken\":\"tok_visa\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/payments").session(c.session()).header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":" + r2.getId() + ",\"paymentToken\":\"tok_visa\"}"))
                .andExpect(status().isConflict());

        assertEquals("PENDING_PAYMENT", reservationStatus(r2.getId()));
    }

    // ---------- invalid input ----------

    @Test
    void realLookingCardNumbersAreRejected() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "WASHER", 30);

        pay(c.session(), r.getId(), "4242 4242 4242 4242")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Card numbers are not accepted. Use a test payment token such as tok_visa"));
        assertTrue(paymentRepository.findByReservationId(r.getId()).isEmpty());
    }

    @Test
    void invalidRequestsReturn400() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "WASHER", 30);

        pay(c.session(), r.getId(), "tok_unknown").andExpect(status().isBadRequest());

        mvc.perform(post("/api/payments").session(c.session())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentToken\":\"tok_visa\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("reservationId is required"));

        mvc.perform(post("/api/payments").session(c.session())
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/payments/not-a-number").session(c.session()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownReservationOrPaymentReturns404() throws Exception {
        Customer c = newCustomer();
        pay(c.session(), 999_999_999L, "tok_visa").andExpect(status().isNotFound());
        mvc.perform(get("/api/payments/999999999").session(c.session())).andExpect(status().isNotFound());
    }

    // ---------- authentication and authorization ----------

    @Test
    void paymentEndpointsRequireSignIn() throws Exception {
        mvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":1,\"paymentToken\":\"tok_visa\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/payments")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotPayForOrViewAnotherCustomersReservation() throws Exception {
        Customer owner = newCustomer();
        Customer other = newCustomer();
        Reservation r = newReservation(owner.user(), "WASHER", 30);

        pay(other.session(), r.getId(), "tok_visa").andExpect(status().isForbidden());
        assertEquals("PENDING_PAYMENT", reservationStatus(r.getId()));

        String body = pay(owner.session(), r.getId(), "tok_visa")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long paymentId = json.readTree(body).get("id").asLong();

        mvc.perform(get("/api/payments/" + paymentId).session(other.session()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reservations/" + r.getId() + "/payments").session(other.session()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/payments/quote").param("reservationId", r.getId().toString()).session(other.session()))
                .andExpect(status().isForbidden());

        // "My payments" only contains the caller's own payments
        mvc.perform(get("/api/payments").session(other.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/payments").session(owner.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void adminCanInspectPaymentsButCustomerCannot() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "WASHER", 30);
        String body = pay(c.session(), r.getId(), "tok_chargeDeclined")
                .andExpect(status().isPaymentRequired())
                .andReturn().getResponse().getContentAsString();
        long paymentId = json.readTree(body).get("id").asLong();

        mvc.perform(get("/api/admin/payments").session(c.session())).andExpect(status().isForbidden());

        MockHttpSession admin = login("admin@laundromat.com", "admin12345");

        String failed = mvc.perform(get("/api/admin/payments").param("status", "failed").session(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        boolean found = false;
        for (JsonNode p : json.readTree(failed)) {
            assertEquals("FAILED", p.get("paymentStatus").asText());
            if (p.get("id").asLong() == paymentId) {
                found = true;
                assertEquals(c.user().getEmail(), p.get("customerEmail").asText());
            }
        }
        assertTrue(found);

        mvc.perform(get("/api/payments/" + paymentId).session(admin)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/payments").param("status", "BOGUS").session(admin))
                .andExpect(status().isBadRequest());
    }

    // ---------- refunds (used by booking when a paid reservation is cancelled) ----------

    @Test
    void refundMarksSuccessfulPaymentRefunded() throws Exception {
        Customer c = newCustomer();
        Reservation r = newReservation(c.user(), "WASHER", 30);
        assertFalse(paymentService.refundReservation(r.getId()));

        pay(c.session(), r.getId(), "tok_visa").andExpect(status().isCreated());
        assertTrue(paymentService.refundReservation(r.getId()));

        assertEquals("REFUNDED", paymentRepository.findByReservationId(r.getId()).get(0).getPaymentStatus());
    }
}
