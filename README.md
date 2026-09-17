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
1. Flyway applies `src/main/resources/db/migration/V1__init_schema.sql` (full schema) and `V2__seed_roles.sql` (default roles: `ADMIN`, `COORDINATOR`, `VOLUNTEER`).
2. `DataInitializer` seeds a default `admin` user with the `ADMIN` role (unless `SEED_ADMIN=false`) — **change its password immediately in any non-local environment.**

Visit `http://localhost:8080/register` to create a volunteer account, or log in as `admin`/`admin123` (default) to reach `/admin`.

## Build / test

```bash
./mvnw clean verify
```

Requires the database above to be reachable — there's currently only a context-load smoke test (`VolunteerPortalApplicationTests`); repository/controller tests are a natural next addition.

## What's implemented

- Auth: registration, login, logout, BCrypt password hashing, role-based access control (`users` / `roles` / `user_roles`)
- Full schema for the volunteer-management domain: `volunteer_profile`, `grade`, `office`, `initiative`, `initiative_question`, `question_lib` / `question_lib_cat`, `volunteer_initiative`, `volunteer_initiative_answer`, `event`, `attend`, `configset`
- JPA entities + Spring Data repositories for every table above
- A role-protected `/admin` page proving the authorization wiring works

## What's not implemented yet

Everything domain-specific beyond the schema/entities/repositories: no controllers or UI yet for offices, initiatives, events, attendance, or the question library — just the data layer and the auth scaffold.
