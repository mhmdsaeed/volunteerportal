# Volunteer Portal — mobile app

Flutter app (Android and iPhone) for volunteers, talking to the Spring Boot server's mobile API (`/api/**`, see "Mobile app API" in the main `README.md`).

- **Log in** with the server address and your Volunteer Portal username/password. The token is kept in the phone's secure storage (Keychain / Android Keystore); **Log out** revokes it on the server.
- **Events** — upcoming events of initiatives you're an approved member of, with your check-in status. The next one is shown as a pass, as on the website's home page.
- **Check in** — scan the event's QR code (the one the coordinator shows under Coordinator → Events → Check-in QR). Scanning again checks you out. Location is only asked for at events that check it. Check-in works from an hour before the event starts until it ends; outside that the server says when it opens or closed, and the app shows that message. Checking out still works after the end. The camera turns off as soon as a result is shown, you leave the tab, or you open your profile on top of it; it comes back when you return.
- **History** — your check-ins and check-outs. **Notifications** — in your language; tap to mark read.
- **Profile** (the account button at the top) — your username and email, your grade, points and roles, and the server address; **Log out** is here too.
- **English / Arabic** from the translate button; Arabic switches the whole layout to right-to-left, and server messages come back in Arabic too (`Accept-Language`). Role names are translated with the website's wording (Admin / Coordinator / Volunteer, مسؤول / منسق / متطوع). Dates show Arabic month and day names with Western digits, as on the website (e.g. `الأربعاء، 23 سبتمبر 08:00–22:00`).
- **Same look as the website**: navy bars, a cool light-grey background, and safety yellow for what is current or yours (the selected tab, the next event, being checked in). The Readex Pro font is bundled in `assets/fonts/`. Colours live in `lib/theme.dart` (`VpColors`); use them instead of hard-coded colours.

## Setup (Windows, everything on drive E:)

The SDK lives in `E:\IDEs\flutter` (Flutter 3.47.5, Dart 3.13.4) with `E:\IDEs\flutter\bin` on the user PATH, and Dart packages go to `E:\IDEs\pub-cache` via the `PUB_CACHE` user variable (instead of `C:\Users\…\AppData\Local\Pub\Cache`). Open a new terminal after installing so both are picked up.

```bash
cd mobile
flutter pub get
flutter test          # unit + widget tests (login, events, check-in, history, Arabic, profile), no phone needed: a fake server stands in for the API
flutter analyze
```

## Run it

**In a browser (quickest, no phone):** start the server with the `dev` profile (it allows `http://localhost:*` to call the API and creates demo accounts), then:

```bash
flutter run -d web-server --web-port 5000     # open http://localhost:5000
```

Log in with server `http://localhost:8080`, user `demo_volunteer`, password `demo12345`. A browser has no camera in most setups, so test scanning on a phone, or use the QR page's test link on the website.

**On a phone:** needs the Android SDK (Android Studio — install it and its SDK on E: too, and set `GRADLE_USER_HOME` to a folder on E:, since Gradle caches several GB) or a Mac with Xcode for iPhone. Debug builds may use plain `http://<your-PC-address>:8080` on the same Wi-Fi; release builds need `https`.

## Code map

| Path | What |
|---|---|
| `lib/api/` | `ApiClient` (bearer token, language, JSON errors → `ApiException`/`UnauthorizedException`) and the JSON models |
| `lib/check_in_flow.dart` | Scan → check in; asks for location only when the server answers `LOCATION_REQUIRED` |
| `lib/app_state.dart` | Server, session, language; `guard()` logs out when the token is rejected |
| `lib/app_scope.dart` | `AppScope` (the app state for every screen), `describeError` (API failures as translated text), and `appRouteObserver`, which tells screens when another screen is opened over them (the scan screen turns the camera off then) |
| `lib/screens/` | Login, home tabs (events, scan, history, notifications), profile; `widgets.dart` has the shared list and the date helpers (`formatDateTime`, `formatEventTime`) |
| `lib/services/` | The phone's location (`location_service.dart`), the token in secure storage (`session_store.dart`), and turning the camera off when the scanner closes (`camera_release*.dart`: on the web, mobile_scanner leaves the camera running, so the app stops it; a no-op on Android and iPhone) |
| `lib/theme.dart` | The website's colours (`VpColors`), the app theme, and the shared `StatusPill` and `LanyardStrip` widgets |
| `assets/fonts/` | Readex Pro (Regular, Medium, SemiBold; Latin and Arabic) and its licence, `OFL.txt` |
| `lib/l10n/app_en.arb`, `app_ar.arb` | Translations (`flutter gen-l10n` generates `app_localizations*.dart`) |
| `test/fake_server.dart` | In-memory stand-in for the API used by the tests |
