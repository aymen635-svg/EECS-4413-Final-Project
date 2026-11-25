# EECS 4413 Final Project - Smart Laundromat

Spring Boot backend, HTML/CSS/JS frontend.

## Running it

Open the folder with pom.xml in VS Code or IntelliJ and run LaundromatApplication, then go to http://localhost:8080

Admin account: admin@laundromat.com / admin12345

Using H2 for now so nobody has to install a database. Data resets every restart. You can see the tables at http://localhost:8080/h2-console (URL: jdbc:h2:mem:laundromat, user: sa, no password)

## Folders

Same setup as the lectures: controller, service, repository, model under src/main/java/com/laundromat. Put your stuff in those. HTML/CSS/JS goes in src/main/resources/static.

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