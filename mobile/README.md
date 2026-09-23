# Volunteer Portal — mobile app

Flutter app (Android and iPhone) for volunteers, talking to the Spring Boot server's mobile API (`/api/**`, see "Mobile app API" in the main `README.md`).

- **Log in** with the server address and your Volunteer Portal username/password. The token is kept in the phone's secure storage (Keychain / Android Keystore); **Log out** revokes it on the server.
- **Events** — upcoming events of initiatives you're an approved member of, with your check-in status.
- **Check in** — scan the event's QR code (the one the coordinator shows under Coordinator → Events → Check-in QR). Scanning again checks you out. Location is only asked for at events that check it.
- **History** — your check-ins and check-outs. **Notifications** — in your language; tap to mark read.
- **English / Arabic** from the translate button; Arabic switches the whole layout to right-to-left, and server messages come back in Arabic too (`Accept-Language`).

## Setup (Windows, everything on drive E:)

The SDK lives in `E:\IDEs\flutter` (Flutter 3.47.5, Dart 3.13.4) with `E:\IDEs\flutter\bin` on the user PATH, and Dart packages go to `E:\IDEs\pub-cache` via the `PUB_CACHE` user variable (instead of `C:\Users\…\AppData\Local\Pub\Cache`). Open a new terminal after installing so both are picked up.

```bash
cd mobile
flutter pub get
flutter test          # unit + widget tests, no phone needed (a fake server stands in for the API)
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
| `lib/screens/` | Login, home tabs (events, scan, history, notifications), profile |
| `lib/l10n/app_en.arb`, `app_ar.arb` | Translations (`flutter gen-l10n` generates `app_localizations*.dart`) |
| `test/fake_server.dart` | In-memory stand-in for the API used by the tests |
