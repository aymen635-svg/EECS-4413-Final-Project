-- Smart Laundromat
-- PostgreSQL Database Constraints
CREATE EXTENSION IF NOT EXISTS btree_gist;
DO $$ BEGIN -- Machine Type
IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_machine_type'
        AND conrelid = 'machines'::regclass
) THEN
ALTER TABLE machines
ADD CONSTRAINT chk_machine_type CHECK (machine_type IN ('WASHER', 'DRYER'));
END IF;
-- Machine Status
IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_machine_status'
        AND conrelid = 'machines'::regclass
) THEN
ALTER TABLE machines
ADD CONSTRAINT chk_machine_status CHECK (
        status IN (
            'AVAILABLE',
            'RESERVED',
            'IN_USE',
            'OUT_OF_SERVICE'
        )
    );
END IF;
-- Reservation Time
IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_reservation_time'
        AND conrelid = 'reservations'::regclass
) THEN
ALTER TABLE reservations
ADD CONSTRAINT chk_reservation_time CHECK (end_time > start_time);
END IF;
-- Reservation Status
IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_reservation_status'
        AND conrelid = 'reservations'::regclass
) THEN
ALTER TABLE reservations
ADD CONSTRAINT chk_reservation_status CHECK (
        status IN (
            'PENDING_PAYMENT',
            'CONFIRMED',
            'CANCELLED',
            'COMPLETED',
            'EXPIRED'
        )
    );
END IF;
-- Prevent Overlapping Reservations
IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'no_overlapping_reservations'
        AND conrelid = 'reservations'::regclass
) THEN
ALTER TABLE reservations
ADD CONSTRAINT no_overlapping_reservations EXCLUDE USING gist (
        machine_id WITH =,
        tsrange(start_time, end_time, '[)') WITH &&
    )
WHERE (status IN ('PENDING_PAYMENT', 'CONFIRMED'));
END IF;
-- Payment Amount
IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_payment_amount'
        AND conrelid = 'payments'::regclass
) THEN
ALTER TABLE payments
ADD CONSTRAINT chk_payment_amount CHECK (amount > 0);
END IF;
-- Payment Status
IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_payment_status'
        AND conrelid = 'payments'::regclass
) THEN
ALTER TABLE payments
ADD CONSTRAINT chk_payment_status CHECK (
        payment_status IN (
            'PENDING',
            'SUCCEEDED',
            'FAILED',
            'REFUNDED'
        )
    );
END IF;
-- Payment Method
IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_payment_method'
        AND conrelid = 'payments'::regclass
) THEN
ALTER TABLE payments
ADD CONSTRAINT chk_payment_method CHECK (payment_method IN ('SIMULATED', 'STRIPE_TEST'));
END IF;
END $$;
-- Prevent Duplicate Successful Payments
CREATE UNIQUE INDEX IF NOT EXISTS uq_successful_payment_per_reservation ON payments (reservation_id)
WHERE payment_status = 'SUCCEEDED';
