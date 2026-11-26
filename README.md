# EECS 4413 Final Project - Smart Laundromat

Spring Boot backend, HTML/CSS/JS frontend, PostgreSQL database.

## Running it

Open the folder with pom.xml in VS Code or IntelliJ and run LaundromatApplication, then go to http://localhost:8080

Admin account: admin@laundromat.com / admin12345

Using PostgreSQL for persistent storage, so data is saved even after restarting the application.

Database setup instructions are in database/README.md . Make sure the database is configured before running the application.

## Folders

Same setup as the lectures: controller, service, repository, model under src/main/java/com/laundromat. Put your stuff in those. HTML/CSS/JS goes in src/main/resources/static.

Database scripts and documentation are in the database folder.

## Auth (done)

- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/logout
- GET /api/users/me

If you're adding endpoints:
- start them with /api/ and login is checked automatically
- anything under /api/admin/ and any non-GET on /api/machines is admin only
- logged in user: `(Long) session.getAttribute("userId")` and `(String) session.getAttribute("role")`
- errors: `throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Machine not found");`

## Payments (done)

- POST /api/payments (pay with a test token, e.g. `tok_visa` / `tok_chargeDeclined`)
- GET /api/payments, GET /api/payments/{id}, GET /api/payments/quote?reservationId=
- GET /api/reservations/{id}/payments
- GET /api/admin/payments?status=

Pages: payments.html?reservationId=ID, admin-payments.html. Full docs and notes for booking/notifications in docs/payments.md.

## Database

Using Spring Data JPA with PostgreSQL for users, machines, reservations, and payments.

Database constraints handle data validation and prevent overlapping reservations.

See database/README.md for details.

## Testing

Run tests using:

    mvn test
