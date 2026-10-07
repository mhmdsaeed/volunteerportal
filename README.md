# Volunteer Portal

Spring Boot 4.1.1 / Java 21 / Thymeleaf / Bootstrap / MySQL volunteer management portal, in English and Arabic, with a Flutter mobile app for volunteers.

| Folder | What's in it |
|---|---|
| `src/` | The Spring Boot server: website and mobile JSON API |
| [`mobile/`](mobile/README.md) | Flutter app (Android/iPhone) for volunteers: events, initiatives (with the same Joined/Pending/Not approved/Not joined/All filter as the website; join one by answering its questions, or withdraw a pending request), QR check-in, history, notifications, profile with change password, and "Forgot your password?" on login; same look and English/Arabic as the website |
| [`deploy/`](deploy/README.md) | Production setup: Docker Compose with MySQL and Caddy (HTTPS), backups |

## Stack

- Spring Boot 4.1.1 (Spring Framework 7, Spring Security 7)
- Java 21
- Thymeleaf + Bootstrap 5 and Bootstrap Icons (via WebJars); right-to-left Bootstrap for Arabic
- Readex Pro font (Latin and Arabic, SIL Open Font License), self-hosted in `static/fonts/`
- MySQL 8, schema managed by Flyway
- Spring Data JPA / Hibernate 7
- ZXing core for the check-in QR codes (rendered as SVG)

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
| `CHECKIN_SECRET` | empty: a random secret is generated at startup (set it in any real deployment, the same on every instance) |
| `MAIL_HOST` | empty: email is off. Set it (with `MAIL_PORT` `587`, `MAIL_USERNAME`, `MAIL_PASSWORD` and `MAIL_FROM`, the sender address) to email "Forgot your password?" links; `MAIL_SMTP_AUTH` and `MAIL_STARTTLS` default to `true` (set both to `false` for a local test server such as Mailpit) |
| `SERVER_PORT` | `8080` |

Set them to match whatever you used above (or just use the defaults).

## Run

```bash
./mvnw spring-boot:run
```

On startup:
1. Flyway applies the migrations in `src/main/resources/db/migration/`:
   - `V1__init_schema.sql` — full schema
   - `V2__seed_roles.sql` — default roles: `ADMIN`, `COORDINATOR`, `VOLUNTEER`
   - `V3__add_notifications.sql` — the `notification` table
   - `V4__notification_message_key.sql` — message key + arguments on notifications, so they're shown in the viewer's language
   - `V5__backfill_notification_message_keys.sql` — fills in keys for notifications stored before V4
   - `V6__api_tokens.sql` — the `api_token` table for the mobile app's bearer tokens
2. `DataInitializer` seeds a default `admin` user with the `ADMIN` role (unless `SEED_ADMIN=false`) — **change its password immediately in any non-local environment** (My Profile → Change password). If a user with that name already exists it's left alone (with a warning in the log if it no longer has the `ADMIN` role), so leaving `SEED_ADMIN=true` is safe.

Visit `http://localhost:8080/register` to create a volunteer account (lands on `/home`, `/initiatives`, `/profile`), or log in as `admin`/`admin123` (default) to reach `/admin`. Add `?lang=ar` to any page (or use the switch at the bottom of the sidebar) for Arabic; the choice is remembered in a cookie.

## Testing QR check-in

Coordinators show an event's check-in QR (Coordinator → Events → **Check-in QR**); volunteers scan it with their phone to check in, and again to check out. Check-in only works from an hour before the event starts until it ends (see [QR self check-in](#whats-implemented)), so give a test event times around now. For real use, set `CHECKIN_SECRET` and serve the site over HTTPS on its real address. To try it locally, two **dev-only** Spring profiles help — never enable them on a real server:

- **`dev`** — creates demo data on startup (`DemoDataInitializer`, only adds what is missing): `demo_coordinator` supervises *Demo Initiative*, which has a *Demo Event* running all day today (00:00–23:59, reset at each start); `demo_volunteer` is an approved member and `demo_pending` has a pending request. Password for all three: `demo12345` (`DEMO_PASSWORD`). It also shows the QR's link under the code, with **Copy**/**Open** buttons, so you can test **without a phone**: open the link in a private window logged in as `demo_volunteer`. Set `DEMO_EVENT_LATITUDE`/`DEMO_EVENT_LONGITUDE` to also get an event that checks the phone's location. It also writes "Forgot your password?" links to the server log (`app.password-reset.log-links`), so the reset works without a mail server: look for `Password reset link for <user>`.
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

JSON API for the volunteer mobile app under `/api` (the Flutter app itself is in [`mobile/`](mobile/README.md)). Log in once for a **bearer token** (valid 30 days, `app.api.token-validity`), then send it on every request as `Authorization: Bearer <token>`. Tokens are random and stored only as a SHA-256 hash (`api_token` table); logging out deletes the token, so it stops working immediately. The API ignores the website's session cookie (it has its own stateless security chain, `ApiSecurityConfig`), so CSRF tokens aren't needed. Messages come back in the phone's language from `Accept-Language` (`en` or `ar`). Errors are JSON: `{"error": "...", "message": "..."}`.

| Method & path | Body | Returns |
|---|---|---|
| `POST /api/auth/login` | `{"username", "password", "deviceName"?}` | `{"token", "expiresAt", "user"}`, or `401 invalid_credentials` |
| `POST /api/auth/logout` | — | `204`; the token is revoked |
| `POST /api/auth/password` | `{"currentPassword", "newPassword"}` | `204`; this token stays valid, my other app logins are revoked and my website sessions end. `400 wrong_password` or `400 password_too_short` (under 8 characters) |
| `POST /api/auth/forgot-password` | `{"email"}` | `202` whether or not the email has an account (no token needed); the reset link is emailed in the `Accept-Language` language and opens the website's reset page. `409 reset_unavailable` when the server has no mail set up |
| `GET /api/me` | — | `{"id", "username", "email", "roles", "grade", "points"}` |
| `GET /api/initiatives` | — | open initiatives with `office` and `membership`: `NONE` / `PENDING` / `APPROVED` / `REJECTED` (the app filters on it) |
| `GET /api/initiatives/{id}` | — | one open initiative with my `membership` and its `questions`: `{"id", "text", "type", "choices"}`, `type` `YES_NO` / `ONE_CHOICE` / `MANY_CHOICES` / `TEXT`. `404` if it isn't open |
| `POST /api/initiatives/{id}/join` | `{"answers": {"<question id>": ["..."]}}` (optional) | the initiative with `membership` `PENDING`. Values are what the website's join form sends: `"1"`/`"0"` for yes/no, choice numbers from 1, or the text; unanswered questions are left out. Answers go to `volunteer_initiative_answer` and the initiative's managers are notified, as from the website. `409 already_requested` if I already asked, `404` if it isn't open |
| `POST /api/initiatives/{id}/withdraw` | — | `204`; my pending request and its answers are deleted. `409 already_reviewed` once a coordinator answered it, `404` if there is none |
| `GET /api/events` | — | upcoming events of initiatives I'm an approved member of, with `requiresLocation` and `myStatus`: `NOT_CHECKED_IN` / `CHECKED_IN` / `CHECKED_OUT` |
| `POST /api/checkin` | `{"qr": "<scanned text>", "latitude"?, "longitude"?}` | `{"result", "success", "eventId", "event", "message"}` — checks in, or out if already in; same rules as the web check-in. `400 invalid_qr` if it isn't an event check-in QR |
| `GET /api/attendance` | — | my check-ins/outs, newest first |
| `GET /api/notifications` | — | my notifications, newest first, in the phone's language |
| `POST /api/notifications/{id}/read`, `POST /api/notifications/read-all` | — | `204` |

Browsers may call the API only from the origins in `app.api.cors-allowed-origin-patterns` (empty by default; the `dev` profile allows `http://localhost:*` so the app can run with `flutter run -d web-server`). Native phone apps don't need CORS.

`result` values: `CHECKED_IN`, `CHECKED_OUT`, `ALREADY_DONE`, `INVALID_CODE`, `NOT_MEMBER`, `EVENT_CLOSED`, `TOO_EARLY` (the message says when check-in opens), `EVENT_ENDED` (when it closed), `LOCATION_REQUIRED`, `TOO_FAR`. The app sends the QR text as scanned; the server reads the event id and code from the check-in link.

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
- Service unit tests (Mockito, no DB) covering registration, join-request approve/reject/find-managed-initiatives and the answers shown for review (every question in order, blank ones included), the per-question-type answer logic in `VolunteerInitiativeServiceImpl.join()`/`withdraw()`, config key-uniqueness checks, notification creation/ownership-checked mark-read/mark-all-read, the report aggregation logic in `ReportServiceImpl` (participation counts by status, attendance counts by check-in/out, leaderboard sorting and its no-profile-yet case), the CSV rendering (header, per-status rows, comma/quote escaping) in `InitiativeExportServiceImpl`, and the copy semantics (name suffix, disabled by default, question cloning, question count) in `InitiativeDuplicateServiceImpl`
- More service unit tests: bearer tokens (`ApiTokenService`), check-in codes and rules (`CheckInCodes`, `CheckInServiceImpl`: distance, check-in opening an hour before the start and closing at the end, checking out after the end, events without times; with a fixed clock), attendance only for approved members (`AttendServiceImpl`), and the Manage Users rules (`UserAdminServiceImpl`: at least one role, admins can't remove their own Admin role or deactivate themselves, the last active admin stays), and the password rules (`PasswordServiceImpl`: wrong current password, minimum length, admins can't set their own, reset links stored as hashes, one per 2 minutes, nothing sent for unknown or deactivated emails, expired links refused, signing out elsewhere)
- Web-layer tests (`@WebMvcTest`) for the admin, coordinator, auth and notification controllers:
  - `AuthController` — registration validation (duplicate username, password mismatch, happy path); the forgot-password page (with and without mail set up, the same answer for any email) and the reset page (valid, used or expired link, mismatch)
  - `CoordinatorController` — supervisor-scoped authorization (a coordinator can only manage initiatives they supervise; an admin can manage any) and the check that a join request being approved/rejected actually belongs to the initiative in the URL; the request review page (answers shown, yes/no translated, forms pointing back to the list it came from, no buttons once decided, 403 for a coordinator who doesn't manage it). Uses `SecurityMockMvcRequestPostProcessors.user(UserDetails)` to inject a real `UserPrincipal`, since `@WithMockUser`'s generic principal doesn't satisfy code that dereferences it
  - `NotificationController` — same real-`UserPrincipal` technique, asserting list/mark-read/mark-all-read are scoped to the current user's id
  - `InitiativeController` / `OfficeController` / `EventController` / `AttendController` / `QuestionLibCatController` / `QuestionLibController` / `GradeController` — the canonical list/create/edit/update/delete CRUD pattern, plus `@Valid` field-error rendering
  - `InitiativeQuestionController` — that CRUD pattern plus the "copy from library" pre-fill (`?fromLibrary=<id>`)
  - `ConfigSetController` — the duplicate-key rejection path (surfaces as a form error, not a raw SQL constraint violation)
  - `VolunteerAdminController` — grade/points update, including when the volunteer has no profile row yet
  - `VolunteerInitiativeController` — the volunteer's initiatives list: opening on Joined, each membership option and its count, the empty states, and the office grouping (order, "No office" last, the office on each card) and office picker (filtering, counts within the office, the pills keeping it, unknown and `none` values)
  - `ReportsController` — each report view renders the rows returned by the (mocked) `ReportService`
  - `InitiativeExportController` — the CSV download's `Content-Type`, `Content-Disposition` filename, and body come from the (mocked) `InitiativeExportService`
  - `InitiativeDuplicateController` — redirects to the edit page of the newly created (mocked) copy
  - `CoordinatorEventController` / `CoordinatorAttendController` — only the initiative's supervisor, its office coordinator or an admin gets in, and the event/record must belong to the initiative/event in the URL
- A full-context `MockMvc` test asserting the `/admin/**` and `/coordinator/**` access-control rules from `SecurityConfig` (anonymous → redirect to login, wrong role → 403)
- `LazyAssociationRenderingTest` (full context, real repositories, no test-level `@Transactional`) — regression tests for a `LazyInitializationException` bug where a view rendered a lazy `@ManyToOne` association's name/username after the request's Hibernate session had already closed (`open-in-view` is disabled). Unlike the `@WebMvcTest`s above, which mock the service layer, this persists real data with the association populated and hits the actual page, so it exercises Hibernate's real session lifecycle — covering the offices list, initiatives list, the volunteer-facing initiatives list (grouped by office) and initiative detail page, the admin volunteers list, `/profile`, the coordinator's join-requests list and request review page (with a real question and answer), the attendance list, and the event attendance report
- End-to-end flows against the real database (`regression/`, `api/`): join requests, QR check-in (and the dev-only test link), Manage Users (role changes, deactivation ending a live session and revoking mobile tokens), passwords (`PasswordFlowTest`: changing your own keeps that session but ends the others and the app logins, an admin setting one, the emailed link working once, and the API's change and forgot-password calls), admin breadcrumb trails, and the mobile API (login, events, check-in, logout) including CORS and QR parsing
- Translation tests (`i18n/`): English and Arabic bundles have the same keys, `?lang=` switching and its cookie, no missing keys on pages in either language, and notifications rendered in the viewer's language
- Startup data: `DataInitializer` (including an existing `admin` without the `ADMIN` role) and `DemoDataInitializer` (creates the demo data once, only what's missing)

The Flutter app has its own tests (`cd mobile && flutter test`), see [`mobile/README.md`](mobile/README.md).

## Deployment

[`deploy/README.md`](deploy/README.md) runs the portal on one server (for example Oracle Cloud's Always Free tier) with Docker Compose: MySQL 8.4, the app with the `prod` profile, and Caddy in front for HTTPS with a free Let's Encrypt certificate. Only Caddy's ports 80/443 are open. Secrets go in `deploy/.env` (git-ignored), and `deploy/backup.sh` makes nightly database backups. The image is built from the root `Dockerfile`, which skips the tests (they need a database), so run `./mvnw verify` before deploying.

The `prod` profile (`application-prod.yml`) trusts Caddy's forwarded headers so redirects and QR links use the public `https://` address. It also sets a secure session cookie, caches templates, and turns off demo data, the QR test link and CORS.

## What's implemented

- **Auth**: registration, login, logout, BCrypt password hashing, role-based access control (`users` / `roles` / `user_roles`), roles seeded as `ADMIN`, `COORDINATOR`, `VOLUNTEER`. After logging in you return to the page you asked for (so a scanned check-in link survives the login). A deactivated user's website session ends on their next request (`StaleSessionFilter`) and the login page says the account is deactivated
- **Passwords** (`PasswordService`): every change signs the user out elsewhere: their mobile app logins are revoked, and their other website sessions end on the next request because `StaleSessionFilter` sees the stored hash differ from the one they logged in with (the login page then says the password was changed). Three ways to change one:
  - **Change your own** on My Profile (`/profile`): current password, then the new one twice (at least 8 characters, as at registration). The session you change it in stays logged in. The mobile app does the same through `POST /api/auth/password`, keeping its own login
  - **An admin sets one** for someone else (Manage Users → **Roles and password**), e.g. for a user who forgot theirs and can't get email; the admin tells them the new one. Admins change their own on My Profile
  - **Forgot your password?** (login page, or the app's login screen) emails a link to `/reset-password`. It works once and for 60 minutes (`app.password-reset.validity`), only its SHA-256 hash is stored (`password_reset_token`), a newer link or any password change voids it, and at most one is sent per account every 2 minutes. The page answers the same whether or not the email belongs to an active account, and sends in the background, so it can't be used to find out who has an account. The email is in the language of the page (or app) it was asked from. Needs a mail server (`MAIL_HOST`, see [Database setup](#database-setup)); without one the page says to ask an administrator. The link's address is taken from the request, which is safe behind Caddy (it only answers for its own domain)
- **English / Arabic**: all UI text, validation messages and notifications in both languages; Arabic pages render right-to-left. Language from `?lang=en|ar`, remembered in a `lang` cookie, anything else falls back to English
- **Layout**: collapsible left sidebar (icon-only when collapsed, remembered in the browser; collapsed on narrow screens, also when a window is narrowed or a tablet turned, and back to your choice when it is wide again) with an unread-notification badge and a pending join request count; breadcrumb trails on every admin page and on the coordinator pages. Look and feel: navy, cool light grey and safety yellow, with yellow kept for what is current or yours (the current page, your next event, being checked in, counts, keyboard focus). The colours are `--vp-*` tokens in `static/css/app.css`, which also drive Bootstrap's variables. On phones, wide tables scroll sideways inside their frame
- **Home** (`/home`): your next event shown as a pass (date and time, map link, check-in status), later events, your join requests with their status, your grade and points, and unread notifications. Coordinators and admins also see how many join requests are waiting for them. With no upcoming events, it points to the initiatives you can join. The sign-in and register pages show the form beside a short description of the portal
- **Admin** (`/admin/**`, `ADMIN` role):
  - Manage Users (`/admin/users`) — every account with its roles and status; edit roles (Admin, Coordinator, Volunteer), set a new password, and activate/deactivate accounts. Rules: at least one role, admins can't remove their own Admin role or deactivate themselves, and the last active admin can't lose the role or be deactivated. The user is notified when their roles change (the website applies it at their next login, the mobile API on the next request); deactivating also deletes their mobile app tokens
  - Offices CRUD (`/admin/offices`) — an office has many initiatives
  - Initiatives CRUD (`/admin/initiatives`) — the supervisor must be an active user with the `ADMIN` or `COORDINATOR` role (a current supervisor who no longer qualifies stays listed when editing, so saving doesn't silently remove them)
  - Initiative questions CRUD, nested per initiative (`/admin/initiatives/{id}/questions`) — true/false, single-choice, multi-choice, and free-text question types
  - Events CRUD, nested per initiative (`/admin/initiatives/{id}/events`)
  - Attendance (check-in/check-out) CRUD, nested per event (`/admin/initiatives/{id}/events/{eventId}/attendance`)
  - Question library CRUD — categories and reusable questions (`/admin/question-library`), wired into initiative-question creation via a "copy from library" picker (one-time copy, no persistent link back to the library entry)
  - Grades CRUD (`/admin/grades`)
  - Volunteer grade/points management (`/admin/volunteers`) — assign a grade and set points on any volunteer's profile, lazily creating the profile row if the volunteer hasn't visited `/profile` yet
  - Config CRUD (`/admin/config`) for the `configset` key/value table, with a duplicate-key check surfaced as a form error
  - Reports (`/admin/reports`) — read-only: initiative participation (approved/pending/rejected join-request counts per initiative), event attendance (check-in/check-out counts per event), and a volunteer leaderboard sorted by points
  - Initiative volunteer export (`/admin/initiatives/{id}/export`) — downloads a CSV of every join request for an initiative (username, email, status, request/response dates, answer count); values that a spreadsheet would read as a formula are prefixed with an apostrophe
  - Initiative duplication (`/admin/initiatives/{id}/duplicate`) — creates a disabled copy of an initiative (name suffixed "(Copy)") along with copies of all its questions, so a recurring initiative doesn't need to be rebuilt from scratch; join requests, events, and attendance are not carried over
- **Coordinator** (`/coordinator/**`, `COORDINATOR` or `ADMIN` role). An initiative can be managed by its supervisor, its office's coordinator, or any admin:
  - View the initiatives you manage and approve/reject volunteer join requests; `/coordinator/requests` lists every pending request you can decide (admins see all). A request can only be decided once
  - Review a request before deciding: **View answers** on either list opens `/coordinator/requests/{id}` with the volunteer (name, email), the initiative, when they asked, the status, and every one of the initiative's questions with the volunteer's answer ("Not answered" when left blank; yes/no in the reviewer's language). Approve/Reject are there too while it is pending, and return to the list you came from (`?from=initiative` for the initiative's list). Same access rule as the lists
  - Events (`/coordinator/initiatives/{id}/events`) — list, add and edit the initiative's events (deleting stays admin-only)
  - Attendance (`.../events/{eventId}/attendance`) — record, edit and delete check-ins/check-outs; only approved members can be recorded (same rule as the admin pages)
  - Check-in QR (`.../events/{eventId}/checkin`) — a full-screen QR for volunteers to scan, with live check-in/out counts. The code is an HMAC of the event and a 30-second window, accepted for 2 minutes (`app.checkin.*`)
- **Volunteer-facing** (`/initiatives`, any authenticated user):
  - Browse enabled initiatives, view details, and submit a join request answering that initiative's questions. The list opens on the initiatives you've joined (approved); options above it, each with a count, switch to Pending, Not approved, Not joined or All (`?show=joined|pending|rejected|notJoined|all`). With nothing joined yet it points to the ones you can join, as do the home page's "Browse initiatives" links. Each card names its office, and the cards are grouped under office headings (by office name, each with its count; initiatives without an office last, under "No office"). An **Office** picker beside the options (`?office=<office id>`, or `none` for those without one) shows one office once you press **Show** (it doesn't reload on change, so keyboard users can move through the offices with the arrow keys); it lists only offices with an open initiative, works together with the membership options (whose counts are then for that office), and an unknown value shows all offices. Names, descriptions and office names are shown direction-isolated (`<bdi>`), so English text on Arabic pages and Arabic text on English ones keep their punctuation in place
  - Withdraw your own join request while it's still pending (not yet reviewed by a coordinator)
  - Approved members see the initiative's upcoming events
- **QR self check-in** (`/checkin/{eventId}`): scanning the coordinator's QR checks you in, or out if you're already in. It needs a valid code, an enabled event, an approved membership, and, for events with coordinates, a phone location within 300 m (`app.checkin.max-distance-meters`). Check-in opens 60 minutes before the event starts (`app.checkin.opens-before`) and closes when it ends; scanning outside that says when check-in opens or closed, and the confirmation page shows that instead of the button. Checking out is never too early and still possible after the end, for volunteers who forgot; an event without a start or end time has no limit on that side. See [Testing QR check-in](#testing-qr-check-in)
- **Mobile app API** (`/api/**`): bearer-token JSON API for the Flutter app, see [Mobile app API](#mobile-app-api)
- **Profile self-service** (`/profile`, any authenticated user):
  - View/edit your own volunteer profile (name, mobile, city, address); grade and points are shown read-only since they're set by an admin
  - Change your password (see **Passwords** above)
- **In-app notifications** (`/notifications`, any authenticated user):
  - Volunteers are notified on join-request approval/rejection, when an admin updates their grade/points, and when their roles change; the initiative's supervisor, its office coordinator and all active admins are notified of new join requests (never about their own). Each has a link back to the relevant page, and the sidebar shows an unread-count badge on every page
  - Shown in the viewer's language: notifications store a message key and arguments and are rendered when viewed
- Full schema for the volunteer-management domain: `volunteer_profile`, `grade`, `office`, `initiative`, `initiative_question`, `question_lib` / `question_lib_cat`, `volunteer_initiative`, `volunteer_initiative_answer`, `event`, `attend`, `configset`, `notification`, `api_token`, `password_reset_token`
- JPA entities + Spring Data repositories for every table above

## What's not implemented yet

- Emailed notifications (notifications are in-app only; the only email sent is the password reset link)
