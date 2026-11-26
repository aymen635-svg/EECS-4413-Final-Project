
# Smart Laundromat - Database Setup

## EECS 4413 Milestone 2

The application uses PostgreSQL with Spring Data JPA and Hibernate for persistent data storage.

## Database Tables

- **users:** Customer and administrator accounts
- **machines:** Washing machines and dryers
- **reservations:** Machine booking information
- **payments:** Simulated payment records

## Requirements

- PostgreSQL
- Java 17 or newer
- Maven

## 1. Create the Database

Connect to PostgreSQL as an administrator and create a database named `laundromat` with a user named `laundromat_app`.

Each developer should use their own local database password.

If the database already exists, skip this step.

## 2. Configure Spring Boot

The application is already configured to use PostgreSQL through `application.properties`.

Set the `DB_PASSWORD` environment variable using your local PostgreSQL password.

The application uses the default PostgreSQL settings unless environment variables override them.

## 3. Start the Application

From the project folder, run:

```bash
mvn spring-boot:run
```

Hibernate automatically creates or updates the required database tables.

The `DataSeeder` initializes an administrator account and sample washing machines and dryers.

Existing database records are preserved when the application restarts.

## 4. Apply Database Constraints

After starting the application for the first time, apply the constraints script located at:

`database/constraints-postgresql.sql`

From the project root, run:

```bash
psql -h localhost -p 5432 -U laundromat_app -d laundromat -W -v ON_ERROR_STOP=1 -f database/constraints-postgresql.sql
```

The script adds database validation rules and prevents conflicting machine reservations.

It can be executed again without duplicating existing constraints.

## 5. Verify the Database

Connect to PostgreSQL and check that the following tables exist:

- users
- machines
- reservations
- payments

The application should also initialize four washing machines and three dryers.

## Database Rules

- Machine numbers must be unique.
- Reservations must have valid start and end times.
- Active reservations cannot overlap for the same machine.
- Payments must have positive amounts.
- Only one successful payment is permitted per reservation.
- Foreign keys maintain relationships between records.

## Testing

Database integration tests cover machine, reservation, and payment persistence.

Run the tests from the project root:

```bash
mvn -Dtest=DatabaseIntegrationTest test
```

Additional testing verified reservation conflict prevention, database constraints, and persistence after application restarts.

## Security

- User passwords are stored as hashes.
- Database credentials are provided through environment variables.
- Real payment-card information is not stored.
- Local database files and credentials should not be committed to GitHub.
