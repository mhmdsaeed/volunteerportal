# CLAUDE.md

Volunteer management portal: a Spring Boot 4.1.1 / Java 21 / Thymeleaf / MySQL website and JSON API (`src/`), a Flutter volunteer app (`mobile/`), and a Docker Compose + Caddy production setup (`deploy/`). `README.md` describes the features, the API and the run/test setup; keep it up to date when you add or change features.

## Commands

```bash
./mvnw spring-boot:run                                    # needs MySQL (see README "Database setup")
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run         # + demo users/initiative/event (password demo12345)
./mvnw clean verify                                       # full test suite
./mvnw test -Dtest=CheckInServiceImplTest                 # one test class (or Class#method)

cd mobile && flutter pub get && flutter test && flutter analyze
```

**Every test except the plain Mockito ones needs the MySQL database from `application.yml` to be running.** There's no embedded database: the migrations are MySQL-specific, and repository tests use `@AutoConfigureTestDatabase(replace = NONE)`.

## Server layout (`com.volunteerportal.app`)

- `controller/`: website MVC controllers. Admin pages are under `/admin/**` and coordinator pages under `/coordinator/**`, with access rules in `config/SecurityConfig`.
- `service/`: an interface plus an `*Impl` for each service. Business rules belong here, with `@Transactional` on writes.
- `model/`: JPA entities (Lombok `@Getter/@Setter/@NoArgsConstructor`). Associations are `LAZY`.
- `repository/`: Spring Data repositories.
- `dto/`: `*Form` classes are validated form-backing objects; `*Row` classes are report rows.
- `api/`: the mobile JSON API under `/api/**`. It has its own stateless security chain (`config/ApiSecurityConfig`) with bearer tokens (`security/ApiTokenAuthenticationFilter`), and it never uses the website session.
- `init/`: `DataInitializer` seeds the admin user. `DemoDataInitializer` runs only with the `dev` profile.
- Spring profiles: `dev` (demo data, QR test link, CORS for `localhost`), `https` (self-signed cert on 8443 for phone testing), and `prod` (behind Caddy). Never enable `dev` or `https` in production.

## Mobile app (`mobile/`, Flutter)

`mobile/README.md` has the setup and a code map. Things that are easy to miss:

- **It talks only to `/api/**`.** If you change a DTO in `api/ApiDtos.java`, update `lib/api/models.dart` and `test/fake_server.dart`; the tests run against that fake server, not the real API.
- **Translations** go in both `lib/l10n/app_en.arb` and `app_ar.arb`. Then run `flutter gen-l10n`, and commit the generated `app_localizations*.dart` files too. Reuse the website's wording (`messages*.properties`) for shared terms such as role names (`roleNames` in `screens/profile_screen.dart`). A build or `pub get` can regenerate those files with only line-ending changes; if `git diff` shows no content change, restore them with `git checkout` instead of committing the noise.
- **Camera**: show it only through `cameraScanner` in `screens/scan_screen.dart`, never a bare `MobileScanner`. On the web, mobile_scanner 7.4.2 leaves the camera running after it stops, so that wrapper turns it off when it closes and when the app goes to the background (`services/camera_release*.dart`; mobile_scanner restarts it on return). Anything else that stops the camera must call `releaseCamera()` too. The release is a no-op in tests, so check camera changes in the web build, e.g. by counting the page's live video tracks (`readyState === 'live'`). The scan screen also removes the camera while another screen is on top of it (`RouteAware` with `appRouteObserver` in `app_scope.dart`); open new screens with `Navigator.push` on the app's navigator, or that observer won't see them.
- **Browser-only code** (`package:web`, `dart:js_interop`) doesn't compile for Android or iPhone. Put it behind a conditional export with a stub for other platforms, as `services/camera_release.dart` does.
- **Dates**: always format them with `formatDateTime` / `formatEventTime` in `screens/widgets.dart`. They keep Western digits in Arabic, as the website does; a bare `DateFormat` would show Arabic-Indic digits.
- **Styling**: use `VpColors` and the theme in `lib/theme.dart`, never hard-coded colours. Its values mirror the website's `--vp-*` tokens, so change both together. Yellow (`VpColors.vest`) means current or yours, as on the website.
- **Bottom tabs**: five tabs share a 360-wide phone, so a tab label must fit one line in both languages. Long ones get a short tab-only key (`tabScanShort`, `tabNotificationsShort` = "Alerts"); the screen title keeps the full name. Check a new or renamed tab in the web build at 360px, in English and Arabic.
- **Lists** use `AsyncList` in `screens/widgets.dart`. To filter a loaded list (as the Initiatives tab does), pass `where` and `headerBuilder` rather than reloading from the API. Initiatives' `InitiativeFilter` repeats the server's `MembershipFilter` rules, so change both together.
- **Font**: Readex Pro is bundled as three static TTF weights (400/500/600) in `assets/fonts/`. Use one of those weights; Flutter doesn't reliably map weights onto variable fonts.
- **Tests** (`test/app_test.dart`) find widgets by `Key` (`loginButton`, `scanTab`, `event-<id>`, …) and by visible text, so keep those keys when restyling. Finders skip offstage widgets, and a screen covered by another is offstage: to check something is gone from a covered screen, use `skipOffstage: false`, or the test passes either way. The Initiatives filter chips scroll sideways and the test screen is too narrow for all of them, so `ensureVisible` a chip before tapping it (the `showFilter` helper); otherwise the tap misses and the test fails on what's shown.

## Conventions and gotchas

- **`open-in-view` is disabled.** A view that shows a lazy association (an office name, a username, …) needs it loaded in the service or repository. Use a `JOIN FETCH` query or `@EntityGraph`, as the existing repositories do. Otherwise rendering throws a `LazyInitializationException`; `regression/LazyAssociationRenderingTest` guards this.
- **Schema changes go in a new Flyway migration** (`src/main/resources/db/migration/V<n>__name.sql`). Never edit an applied migration. `ddl-auto: validate` means entities must match the schema exactly.
- **All user-visible text is translated.** Add every new key to both `messages.properties` and `messages_ar.properties`; `i18n/I18nMessagesTest` fails if their keys differ. Templates use `#{...}` keys. Validation errors and service refusals use message codes. Arabic pages render right-to-left, so use logical CSS properties (`start`/`end`, not `left`/`right`).
- **Message files trim trailing spaces** (editors strip them, and an escaped ` ` gets mangled by most tools), so never end a value in `messages*.properties` with a space; add it in code instead (see `common.listSeparator`).
- **Notifications** are created with `NotificationService.notify(user, messageKey, link, args...)`. They store a key and arguments and are rendered in the viewer's language. Show them with `NotificationService.text(notification, locale)`, never by resolving the key directly: it also translates role codes in role-change notifications.
- **Check-in rules** live in `CheckInServiceImpl.checkIn`, shared by the website (`/checkin/{eventId}`) and the mobile API: valid code → enabled event → approved member → not already done → time window (opens `app.checkin.opens-before` before the start, closes at the end; check-outs and events without times are exempt) → location. When you add a `CheckInService.Result`, add `checkin.result.<NAME>` in both languages; if its message shows a time as `{0}`, return that time from `CheckInService.messageTime` so both the website and the API fill it in. The mobile app shows the server's message for results it doesn't know, so it needs no change.
- **Time-dependent code takes a `Clock`** (`CheckInServiceImpl`, `CheckInCodes`): use `LocalDateTime.now(clock)`, give tests a fixed clock through the package-private constructor, and mark the public constructor `@Autowired`, or Spring can't choose between the two and the application context fails to start.
- **The `dev` demo event is reset to 00:00–23:59 today on every start**, so check-in always works locally. Any other test event needs times around now, because check-in is refused outside its window.
- **Coordinator access**: an initiative can be managed by an admin, its supervisor, or its office's coordinator. Check this with `CoordinatorAccess.assertCanManage` / `JoinRequestService.canManage`, and also check that nested ids in the URL belong together (the event to the initiative, the record to the event).
- **A volunteer's membership status** (joined = approved, pending, not approved, not joined) is decided by `service/MembershipFilter.matches`; reuse it instead of re-checking `responseJoinDttm` / `enabled`. The mobile app gets the same status as the API's `membership` value and filters with `InitiativeFilter`, which must follow the same rules. The volunteer's `/initiatives` list opens on Joined, so a link that invites someone to join an initiative should go to `/initiatives?show=notJoined` (as the home page's do).
- **Some templates are shared between admin and coordinator pages.** The event and attendance pages take their base URLs from the `eventsPath` / `attendancePath` model attributes.
- **Layout fragments** live in `templates/fragments/layout.html`: `head(title)`, the `navbar` sidebar, `scripts`, and the breadcrumb fragments `crumb(label, url)`, `crumbText(label)` and `crumbHere(label)`. Every admin page starts with a breadcrumb trail.
- **Styling**: use the `--vp-*` colour tokens in `static/css/app.css`, never hard-coded colours, and keep plain Bootstrap markup. The stylesheet maps Bootstrap's variables and its `btn-*`, `text-bg-*`, `table` and `alert` classes onto the tokens. Safety yellow (`--vp-vest`) is reserved for what is current or the user's own: the current page, the next event, being checked in, counts, and keyboard focus. Use logical properties (`inline-start`/`end`) so Arabic mirrors.
- **Tests**: add a `@WebMvcTest` for a new controller, a Mockito unit test for service rules, and a flow test under `regression/` against the real database when the behaviour spans layers (security, Hibernate sessions, real queries). For code that reads the logged-in user, inject a real `UserPrincipal` with `SecurityMockMvcRequestPostProcessors.user(...)`; `@WithMockUser` doesn't provide one.
- **Line endings**: shell scripts, `mvnw`, `Dockerfile` and `deploy/Caddyfile` must keep LF (see `.gitattributes`).
- **Deployment**: the `Dockerfile` skips tests, so run `./mvnw verify` before deploying. Secrets come from `deploy/.env`, which is git-ignored and must never be committed.
