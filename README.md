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

## Testing QR check-in

Coordinators show an event's check-in QR (Coordinator → Events → **Check-in QR**); volunteers scan it with their phone to check in, and again to check out. For real use, set `CHECKIN_SECRET` and serve the site over HTTPS on its real address. To try it locally, two **dev-only** Spring profiles help — never enable them on a real server:

- **`dev`** — creates demo data on startup (`DemoDataInitializer`, only adds what is missing): `demo_coordinator` supervises *Demo Initiative*, which has a *Demo Event* running all day today; `demo_volunteer` is an approved member and `demo_pending` has a pending request. Password for all three: `demo12345` (`DEMO_PASSWORD`). It also shows the QR's link under the code, with **Copy**/**Open** buttons, so you can test **without a phone**: open the link in a private window logged in as `demo_volunteer`. Set `DEMO_EVENT_LATITUDE`/`DEMO_EVENT_LONGITUDE` to also get an event that checks the phone's location.
- **`https`** — serves `https://<host>:8443` with a self-signed certificate, so a **phone** on your Wi-Fi can check in to events with coordinates (phone browsers only share location over HTTPS). Create the certificate once (git-ignored), listing your PC's Wi-Fi address from `ipconfig`:

  ```bash
  mkdir -p dev-certs
  keytool -genkeypair -alias dev -keyalg RSA -keysize 2048 -validity 825 -storetype PKCS12 \
    -keystore dev-certs/dev-keystore.p12 -storepass changeit -dname "CN=volunteerportal-dev" \
    -ext "SAN=dns:localhost,ip:127.0.0.1,ip:192.168.1.23"
  ```

Run with both, then open the coordinator's QR page **using the Wi-Fi address** (the QR contains whatever address the page was opened on — `localhost` won't work on a phone):

```bash
SPRING_PROFILES_ACTIVE=dev,https ./mvnw spring-boot:run      # PowerShell: $env:SPRING_PROFILES_ACTIVE='dev,https'
```

The phone warns about the self-signed certificate once; continue anyway (or install `dev-certs/dev-cert.crt`, exported with `keytool -exportcert -rfc`, as a trusted certificate). If the phone can't connect, allow Java through Windows Firewall on private networks.

## Mobile app API

JSON API for the volunteer mobile app under `/api`. Log in once for a **bearer token** (valid 30 days, `app.api.token-validity`), then send it on every request as `Authorization: Bearer <token>`. Tokens are random and stored only as a SHA-256 hash (`api_token` table); logging out deletes the token, so it stops working immediately. The API ignores the website's session cookie (it has its own stateless security chain, `ApiSecurityConfig`), so CSRF tokens aren't needed. Messages come back in the phone's language from `Accept-Language` (`en` or `ar`). Errors are JSON: `{"error": "...", "message": "..."}`.

| Method & path | Body | Returns |
|---|---|---|
| `POST /api/auth/login` | `{"username", "password", "deviceName"?}` | `{"token", "expiresAt", "user"}`, or `401 invalid_credentials` |
| `POST /api/auth/logout` | — | `204`; the token is revoked |
| `GET /api/me` | — | `{"id", "username", "email", "roles", "grade", "points"}` |
| `GET /api/initiatives` | — | open initiatives with `membership`: `NONE` / `PENDING` / `APPROVED` / `REJECTED` |
| `GET /api/events` | — | upcoming events of initiatives I'm an approved member of, with `requiresLocation` and `myStatus`: `NOT_CHECKED_IN` / `CHECKED_IN` / `CHECKED_OUT` |
| `POST /api/checkin` | `{"qr": "<scanned text>", "latitude"?, "longitude"?}` | `{"result", "success", "eventId", "event", "message"}` — checks in, or out if already in; same rules as the web check-in. `400 invalid_qr` if it isn't an event check-in QR |
| `GET /api/attendance` | — | my check-ins/outs, newest first |
| `GET /api/notifications` | — | my notifications, newest first, in the phone's language |
| `POST /api/notifications/{id}/read`, `POST /api/notifications/read-all` | — | `204` |

`result` values: `CHECKED_IN`, `CHECKED_OUT`, `ALREADY_DONE`, `INVALID_CODE`, `NOT_MEMBER`, `EVENT_CLOSED`, `LOCATION_REQUIRED`, `TOO_FAR`. The app sends the QR text as scanned; the server reads the event id and code from the check-in link.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"demo_volunteer","password":"demo12345"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl -s http://localhost:8080/api/events -H "Authorization: Bearer $TOKEN"
```

## Build / test

```bash
./mvnw clean verify
```

Requires the database above to be reachable. The suite includes:
- A context-load smoke test (`VolunteerPortalApplicationTests`)
- Repository tests (`@DataJpaTest`, run against the real configured MySQL DB via `@AutoConfigureTestDatabase(replace = NONE)` — there's no embedded test DB since the schema/Flyway migrations are MySQL-specific; each test rolls back its own transaction)
- Service unit tests (Mockito, no DB) covering registration, join-request approve/reject/find-managed-initiatives, the per-question-type answer logic in `VolunteerInitiativeServiceImpl.join()`/`withdraw()`, config key-uniqueness checks, notification creation/ownership-checked mark-read/mark-all-read, the report aggregation logic in `ReportServiceImpl` (participation counts by status, attendance counts by check-in/out, leaderboard sorting and its no-profile-yet case), the CSV rendering (header, per-status rows, comma/quote escaping) in `InitiativeExportServiceImpl`, and the copy semantics (name suffix, disabled by default, question cloning, question count) in `InitiativeDuplicateServiceImpl`
- Web-layer tests (`@WebMvcTest`), covering **every controller in the app**:
  - `AuthController` — registration validation (duplicate username, password mismatch, happy path)
  - `CoordinatorController` — supervisor-scoped authorization (a coordinator can only manage initiatives they supervise; an admin can manage any) and the check that a join request being approved/rejected actually belongs to the initiative in the URL. Uses `SecurityMockMvcRequestPostProcessors.user(UserDetails)` to inject a real `UserPrincipal`, since `@WithMockUser`'s generic principal doesn't satisfy code that dereferences it
  - `NotificationController` — same real-`UserPrincipal` technique, asserting list/mark-read/mark-all-read are scoped to the current user's id
  - `InitiativeController` / `OfficeController` / `EventController` / `AttendController` / `QuestionLibCatController` / `QuestionLibController` / `GradeController` — the canonical list/create/edit/update/delete CRUD pattern, plus `@Valid` field-error rendering
  - `InitiativeQuestionController` — that CRUD pattern plus the "copy from library" pre-fill (`?fromLibrary=<id>`)
  - `ConfigSetController` — the duplicate-key rejection path (surfaces as a form error, not a raw SQL constraint violation)
  - `VolunteerAdminController` — grade/points update, including when the volunteer has no profile row yet
  - `ReportsController` — each report view renders the rows returned by the (mocked) `ReportService`
  - `InitiativeExportController` — the CSV download's `Content-Type`, `Content-Disposition` filename, and body come from the (mocked) `InitiativeExportService`
  - `InitiativeDuplicateController` — redirects to the edit page of the newly created (mocked) copy
- A full-context `MockMvc` test asserting the `/admin/**` and `/coordinator/**` access-control rules from `SecurityConfig` (anonymous → redirect to login, wrong role → 403)
- `LazyAssociationRenderingTest` (full context, real repositories, no test-level `@Transactional`) — regression tests for a `LazyInitializationException` bug where a view rendered a lazy `@ManyToOne` association's name/username after the request's Hibernate session had already closed (`open-in-view` is disabled). Unlike the `@WebMvcTest`s above, which mock the service layer, this persists real data with the association populated and hits the actual page, so it exercises Hibernate's real session lifecycle — covering the offices list, initiatives list, the volunteer-facing initiative detail page, the admin volunteers list, `/profile`, the coordinator's join-requests list, the attendance list, and the event attendance report

## What's implemented

- **Auth**: registration, login, logout, BCrypt password hashing, role-based access control (`users` / `roles` / `user_roles`), roles seeded as `ADMIN`, `COORDINATOR`, `VOLUNTEER`
- **Admin** (`/admin/**`, `ADMIN` role):
  - Offices CRUD (`/admin/offices`) — an office has many initiatives
  - Initiatives CRUD (`/admin/initiatives`)
  - Initiative questions CRUD, nested per initiative (`/admin/initiatives/{id}/questions`) — true/false, single-choice, multi-choice, and free-text question types
  - Events CRUD, nested per initiative (`/admin/initiatives/{id}/events`)
  - Attendance (check-in/check-out) CRUD, nested per event (`/admin/initiatives/{id}/events/{eventId}/attendance`)
  - Question library CRUD — categories and reusable questions (`/admin/question-library`), wired into initiative-question creation via a "copy from library" picker (one-time copy, no persistent link back to the library entry)
  - Grades CRUD (`/admin/grades`)
  - Volunteer grade/points management (`/admin/volunteers`) — assign a grade and set points on any volunteer's profile, lazily creating the profile row if the volunteer hasn't visited `/profile` yet
  - Config CRUD (`/admin/config`) for the `configset` key/value table, with a duplicate-key check surfaced as a form error
  - Reports (`/admin/reports`) — read-only: initiative participation (approved/pending/rejected join-request counts per initiative), event attendance (check-in/check-out counts per event), and a volunteer leaderboard sorted by points
  - Initiative volunteer export (`/admin/initiatives/{id}/export`) — downloads a CSV of every join request for an initiative (username, email, status, request/response dates, answer count)
  - Initiative duplication (`/admin/initiatives/{id}/duplicate`) — creates a disabled copy of an initiative (name suffixed "(Copy)") along with copies of all its questions, so a recurring initiative doesn't need to be rebuilt from scratch; join requests, events, and attendance are not carried over
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
