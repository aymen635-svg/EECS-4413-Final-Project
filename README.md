# Smart Laundromat

Spring Boot backend + HTML/CSS/JS frontend.

## Run it

1. Open this folder in IntelliJ (File > Open, pick the folder with `pom.xml`)
2. Run `LaundromatApplication`
3. Go to http://localhost:8080

Admin login: `admin@laundromat.com` / `admin12345`

Database is H2 (in memory, resets on restart). View tables at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:laundromat`, user `sa`, no password).

Run tests: right-click `src/test/java` > Run All Tests

## Structure

```
src/main/java/com/laundromat
├── controller   REST endpoints
├── service      business logic
├── repository   database access
├── model        entities (tables)
├── dto          request bodies
└── config       login/role checks, error format, admin seed
src/main/resources/static   HTML, CSS, JS
```

## API so far

| Method | Endpoint | Who | Result |
|---|---|---|---|
| POST | /api/auth/register | anyone | 201, 400 bad input, 409 email taken |
| POST | /api/auth/login | anyone | 200, 401 wrong email/password |
| POST | /api/auth/logout | signed in | 204 |
| GET | /api/users/me | signed in | 200, 401 not signed in |

## Rules for everyone

- Start every endpoint with `/api/`
- Signed-in check is automatic for all `/api/` endpoints
- Admin only: anything under `/api/admin/`, and POST/PUT/PATCH/DELETE on `/api/machines`
- Anyone (no login): GET `/api/machines`
- Get the logged-in user in a controller:

```java
public ... myEndpoint(HttpSession session) {
    Long userId = (Long) session.getAttribute("userId");
    String role = (String) session.getAttribute("role");   // "CUSTOMER" or "ADMIN"
}
```

- Errors: `throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Machine not found");` returns `{"message": "Machine not found"}` with that status
