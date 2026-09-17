# Volunteer Portal

Spring Boot 4.1.1 / Java 21 / Thymeleaf / Bootstrap / MySQL volunteer management portal.

## Stack

- Spring Boot 4.1.1 (Spring Framework 7, Spring Security 7)
- Java 21
- Thymeleaf + Bootstrap 5 (via WebJars)
- MySQL 8, schema managed by Flyway
- Spring Data JPA / Hibernate 7

## Prerequisites

- JDK 21
- A running MySQL 8 server (no local `mysql` client required — the Maven wrapper handles the build)

## Database setup

Create the database and application user (adjust the password):

```sql
CREATE DATABASE volunteerportal CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'volunteerportal_app'@'%' IDENTIFIED BY 'changeme';
GRANT ALL PRIVILEGES ON volunteerportal.* TO 'volunteerportal_app'@'%';
FLUSH PRIVILEGES;
```

The app reads connection settings from environment variables (see `src/main/resources/application.yml`), with local-dev defaults:

| Variable | Default |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `volunteerportal` |
| `DB_USERNAME` | `volunteerportal_app` |
| `DB_PASSWORD` | `changeme` |
| `SEED_ADMIN` | `true` |
| `ADMIN_USERNAME` | `admin` |
| `ADMIN_DEFAULT_PASSWORD` | `admin123` |
| `ADMIN_EMAIL` | `admin@volunteerportal.local` |

Set them to match whatever you used above (or just use the defaults).

## Run

```bash
./mvnw spring-boot:run
```

On startup:
1. Flyway applies `src/main/resources/db/migration/V1__init_schema.sql` (full schema), `V2__seed_roles.sql` (default roles: `ADMIN`, `COORDINATOR`, `VOLUNTEER`), and `V3__add_notifications.sql` (the `notification` table).
2. `DataInitializer` seeds a default `admin` user with the `ADMIN` role (unless `SEED_ADMIN=false`) — **change its password immediately in any non-local environment.**

Visit `http://localhost:8080/register` to create a volunteer account (lands on `/home`, `/initiatives`, `/profile`), or log in as `admin`/`admin123` (default) to reach `/admin`.

## Build / test

```bash
./mvnw clean verify
```

Requires the database above to be reachable. The suite includes:
- A context-load smoke test (`VolunteerPortalApplicationTests`)
- Repository tests (`@DataJpaTest`, run against the real configured MySQL DB via `@AutoConfigureTestDatabase(replace = NONE)` — there's no embedded test DB since the schema/Flyway migrations are MySQL-specific; each test rolls back its own transaction)
- Service unit tests (Mockito, no DB) covering registration, join-request approve/reject/find-managed-initiatives, the per-question-type answer logic in `VolunteerInitiativeServiceImpl.join()`/`withdraw()`, and config key-uniqueness checks
- Web-layer tests: a `@WebMvcTest` for `AuthController`'s registration validation; a full-context `MockMvc` test asserting the `/admin/**` and `/coordinator/**` access-control rules (anonymous → redirect to login, wrong role → 403); and a `@WebMvcTest` for `CoordinatorController` covering the supervisor-scoped authorization (a coordinator can only manage initiatives they supervise; an admin can manage any) and the check that a join request being approved/rejected actually belongs to the initiative in the URL — these use `SecurityMockMvcRequestPostProcessors.user(UserDetails)` to inject a real `UserPrincipal`, since `@WithMockUser`'s generic principal doesn't satisfy code that dereferences it

## What's implemented

- **Auth**: registration, login, logout, BCrypt password hashing, role-based access control (`users` / `roles` / `user_roles`), roles seeded as `ADMIN`, `COORDINATOR`, `VOLUNTEER`
- **Admin** (`/admin/**`, `ADMIN` role):
  - Initiatives CRUD (`/admin/initiatives`)
  - Initiative questions CRUD, nested per initiative (`/admin/initiatives/{id}/questions`) — true/false, single-choice, multi-choice, and free-text question types
  - Offices CRUD (`/admin/offices`)
  - Events CRUD, nested per initiative (`/admin/initiatives/{id}/events`)
  - Attendance (check-in/check-out) CRUD, nested per event (`/admin/initiatives/{id}/events/{eventId}/attendance`)
  - Question library CRUD — categories and reusable questions (`/admin/question-library`), wired into initiative-question creation via a "copy from library" picker (one-time copy, no persistent link back to the library entry)
  - Grades CRUD (`/admin/grades`)
  - Volunteer grade/points management (`/admin/volunteers`) — assign a grade and set points on any volunteer's profile, lazily creating the profile row if the volunteer hasn't visited `/profile` yet
  - Config CRUD (`/admin/config`) for the `configset` key/value table, with a duplicate-key check surfaced as a form error
- **Coordinator** (`/coordinator/**`, `COORDINATOR` or `ADMIN` role):
  - View initiatives you supervise and approve/reject volunteer join requests
- **Volunteer-facing** (`/initiatives`, any authenticated user):
  - Browse enabled initiatives, view details, and submit a join request answering that initiative's questions
  - Withdraw your own join request while it's still pending (not yet reviewed by a coordinator)
- **Profile self-service** (`/profile`, any authenticated user):
  - View/edit your own volunteer profile (name, mobile, city, address); grade and points are shown read-only since they're set by an admin
- **In-app notifications** (`/notifications`, any authenticated user):
  - Notified on join-request approval/rejection and when an admin updates your grade/points, with a link back to the relevant page; navbar shows an unread-count badge on every page
- Full schema for the volunteer-management domain: `volunteer_profile`, `grade`, `office`, `initiative`, `initiative_question`, `question_lib` / `question_lib_cat`, `volunteer_initiative`, `volunteer_initiative_answer`, `event`, `attend`, `configset`, `notification`
- JPA entities + Spring Data repositories for every table above

## What's not implemented yet

- Real email delivery (notifications are in-app only, not emailed)
- Broader test coverage: the admin CRUD controllers (initiatives, offices, events, attendance, question library, grades, config) don't have dedicated tests yet — the suite establishes the repository/service-unit/web-layer patterns to extend
