# Payment Simulation (UC4)

Simulated payments for reservations. No real card data is ever collected or stored:
the customer picks a **test token** (named after Stripe's test tokens) and the server
decides the outcome.

## How it works

1. Booking creates a reservation with status `PENDING_PAYMENT`. The slot is held for
   `app.payment.hold-minutes` (default 15), or until the start time if that is sooner.
2. The customer calls `POST /api/payments` with the reservation id and a test token.
3. `PaymentService`:
   - validates the input (rejects anything that looks like a card number),
   - locks the reservation row (`SELECT ... FOR UPDATE`) so concurrent payments for
     the same reservation are serialized,
   - checks ownership, the idempotency key and the reservation status/hold,
   - calculates the amount on the server (`PricingService`: per-minute price by machine type),
   - charges through the `PaymentGateway` adapter (`SimulatedPaymentGateway`),
   - records the attempt. On success the payment is `SUCCEEDED`, the reservation becomes
     `CONFIRMED` and a `PaymentCompletedEvent` is published. On decline the payment is
     `FAILED` with a reason and the reservation stays `PENDING_PAYMENT` so the customer can retry.
4. If the hold has expired, the reservation is set to `EXPIRED` (freeing the slot) and
   the payment is rejected. A late payment never confirms an expired reservation.

The unique index `uq_successful_payment_per_reservation` (see `database/constraints-postgresql.sql`)
is a database-level backstop against two successful payments for one reservation.

### Test tokens

| Token | Result |
|---|---|
| `tok_visa` | Succeeds |
| `tok_mastercard` | Succeeds |
| `tok_chargeDeclined` | Declined: "Your card was declined." |
| `tok_chargeDeclinedInsufficientFunds` | Declined: "Your card has insufficient funds." |
| `tok_chargeDeclinedExpiredCard` | Declined: "Your card has expired." |

### Pricing (`application.properties`)

| Property | Default |
|---|---|
| `app.pricing.washer-per-hour` | 5.00 |
| `app.pricing.dryer-per-hour` | 4.00 |
| `app.payment.currency` | CAD |
| `app.payment.hold-minutes` | 15 |

Amount = hourly rate × reservation minutes / 60, rounded to cents.

## API

All endpoints require sign-in (401 otherwise). Errors return `{"message": "..."}`.

### `POST /api/payments`: pay for a reservation

Auth: customer who owns the reservation.

Headers: `Idempotency-Key` (optional, 1 to 100 chars). Retrying with the same key returns the
original result instead of charging again, with header `Idempotent-Replayed: true`.

Request:
```json
{ "reservationId": 12, "paymentToken": "tok_visa" }
```
Any `amount` in the body is ignored. The server calculates it.

Response (`PaymentResponse`):
```json
{
  "id": 7, "reservationId": 12, "reservationStatus": "CONFIRMED",
  "machineNumber": "W001", "customerEmail": "jane@test.com",
  "amount": 5.00, "currency": "CAD", "paymentMethod": "SIMULATED",
  "paymentStatus": "SUCCEEDED", "paymentDate": "2026-11-26T10:15:00",
  "transactionRef": "sim_ch_3f9a...", "failureReason": null
}
```

| Status | Meaning |
|---|---|
| 201 | Payment succeeded, reservation confirmed (`Location: /api/payments/{id}`) |
| 402 | Payment declined. Attempt recorded, body has `failureReason`. Customer may retry |
| 400 | Missing `reservationId`/`paymentToken`, unknown token, card number sent, bad JSON, bad key |
| 403 | Reservation belongs to another customer |
| 404 | Reservation not found |
| 409 | Already paid, cancelled/expired, hold expired, or idempotency key reused for another reservation |

### `GET /api/payments`
The signed-in customer's own payments, newest first. 200 → `PaymentResponse[]`.

### `GET /api/payments/{paymentId}`
One payment. Owner or admin. 200 / 403 / 404.

### `GET /api/payments/quote?reservationId={id}`
Amount due before paying. Owner or admin. 200 / 400 / 403 / 404.
```json
{ "reservationId": 12, "reservationStatus": "PENDING_PAYMENT", "machineNumber": "W001",
  "machineType": "WASHER", "startTime": "...", "endTime": "...",
  "amount": 5.00, "currency": "CAD", "payBefore": "2026-11-26T10:30:00" }
```

### `GET /api/reservations/{reservationId}/payments`
All payment attempts for a reservation, newest first. Owner or admin. 200 / 403 / 404.

### `GET /api/admin/payments?status={status}`
Admin only (403 for customers). Optional `status`: `PENDING`, `SUCCEEDED`, `FAILED`, `REFUNDED`
(case-insensitive, 400 otherwise).

## For other modules

- **Booking:** link customers to `payments.html?reservationId={id}` after creating a reservation.
  When a `CONFIRMED` reservation is cancelled, call `paymentService.refundReservation(id)`
  (marks the payment `REFUNDED`). Reservations must start as `PENDING_PAYMENT` (the entity default).
- **Notifications:** listen for `com.laundromat.event.PaymentCompletedEvent` with
  `@TransactionalEventListener` so it only fires after the payment commits.
- **Analytics:** count completed payments with `paymentRepository.findByPaymentStatus("SUCCEEDED")`.
- **Stripe test mode (optional):** implement `PaymentGateway` with a Stripe adapter. `PaymentService` doesn't change.

## Database changes

Three nullable columns were added to `payments` (Hibernate `ddl-auto=update` adds them):
`idempotency_key` (unique), `transaction_ref`, `failure_reason`.

## Frontend

- `payments.html?reservationId={id}`: shows the amount due, lets the customer choose a
  test token and pay, and shows their payment history. `payments.html` alone shows just the history.
- `admin-payments.html`: admin table of all payments with a status filter.

## Tests

`src/test/java/com/laundromat/controller/PaymentControllerTest.java` (17 tests) covers success,
server-side pricing, declined payments and retry, double payment, cancelled and expired
reservations, concurrent payments (5 parallel requests → exactly one succeeds), idempotency,
card-number rejection, invalid input, 401/403/404, admin access and refunds.
