# Volunteer Portal — mobile app

Flutter app (Android and iPhone) for volunteers, talking to the Spring Boot server's mobile API (`/api/**`, see "Mobile app API" in the main `README.md`).

- **Log in** with the server address and your Volunteer Portal username/password. The token is kept in the phone's secure storage (Keychain / Android Keystore); **Log out** revokes it on the server. **Forgot your password?** under the password asks for your account's email; the server emails a link to the website's reset page (the link works once, for an hour). If the server has no mail set up, the app says to ask an administrator.
- **Events** — upcoming events of initiatives you're an approved member of, with your check-in status. The next one is shown as a pass, as on the website's home page.
- **Initiatives** — the open initiatives with your status in each (Approved, Pending, Not approved, Not joined). It opens on the ones you've joined; the options above the list, each with a count, switch to Pending, Not approved, Not joined or All, as on the website's Initiatives page. The list is grouped under office headings (by office name, each with its count; "No office" last), and the **Office** picker under the options shows one office, or those without one; the counts above are then for that office, and if its last open initiative closes the list goes back to all offices. With nothing joined yet (and no office chosen), a button shows the ones you can join. **Tap an initiative** to open it: not joined yet, it shows the initiative's questions (yes/no, one choice, several choices, or your own words; any can be left blank, as on the website) and **Request to join** sends your answers; the request then waits for a coordinator, who is notified. While it is pending you can **Withdraw request** (after confirming). The list reloads with your new status.
- **Check in** — scan the event's QR code (the one the coordinator shows under Coordinator → Events → Check-in QR). Scanning again checks you out. Location is only asked for at events that check it. Check-in works from an hour before the event starts until it ends; outside that the server says when it opens or closed, and the app shows that message. Checking out still works after the end. The camera turns off as soon as a result is shown, you leave the tab, you open your profile on top of it, or the app goes to the background; it comes back when you return.
- **History** — your check-ins and check-outs. **Notifications** (the "Alerts" tab in English, so five tabs fit on a small phone) — in your language; tap to mark read.
- **Profile** (the account button at the top) — your username and email, your grade, points and roles, and the server address; **Change password** (your current one, then the new one twice, at least 8 characters: the app stays logged in, and you are logged out of the website and your other devices); **Log out** is here too.
- **English / Arabic** from the translate button; Arabic switches the whole layout to right-to-left, and server messages come back in Arabic too (`Accept-Language`). Role names are translated with the website's wording (Admin / Coordinator / Volunteer, مسؤول / منسق / متطوع). Dates show Arabic month and day names with Western digits, as on the website (e.g. `الأربعاء، 23 سبتمبر 08:00–22:00`).
- **Same look as the website**: navy bars, a cool light-grey background, and safety yellow for what is current or yours (the selected tab, the next event, being checked in). The Readex Pro font is bundled in `assets/fonts/`. Colours live in `lib/theme.dart` (`VpColors`); use them instead of hard-coded colours.

## Setup (Windows, everything on drive E:)

The SDK lives in `E:\IDEs\flutter` (Flutter 3.47.5, Dart 3.13.4) with `E:\IDEs\flutter\bin` on the user PATH, and Dart packages go to `E:\IDEs\pub-cache` via the `PUB_CACHE` user variable (instead of `C:\Users\…\AppData\Local\Pub\Cache`). Open a new terminal after installing so both are picked up.

```bash
cd mobile
flutter pub get
flutter test          # unit + widget tests (login, events, initiatives and joining (filters, office grouping and picker), check-in, history, Arabic, profile, change and forgotten password), no phone needed: a fake server stands in for the API
flutter analyze
```

## Run it

Three ways, all against the server running on your PC: in a browser (quickest), on the Android emulator, or on your own Android phone (the only one that can scan a real QR code). The commands are for PowerShell.

### 1. Start the server

MySQL must be running. From the project folder:

```powershell
cd E:\workspace_self_emp\claude_ws\volunteerportal
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot'   # the system JAVA_HOME is an older JDK
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw -o spring-boot:run
```

The website is then at http://localhost:8080. The `dev` profile lets `http://localhost:*` call the API and creates demo accounts, all with password `demo12345`:

| Account | Use it to |
|---|---|
| `demo_volunteer` | Log in to the app as a member of *Demo Initiative* (its *Demo Event* runs all day today, so check-in is always open) |
| `demo_pending` | See a pending request |
| `demo_coordinator` | Approve or reject requests on the website, and show the event's check-in QR |

These are the accounts as first created. The demo setup only adds what is missing, so once you approve `demo_pending`'s request it stays approved on later starts; register new accounts to try those steps again. Only *Demo Event* is reset on every start (enabled, all day today).

To try **joining an initiative**, register a new account at http://localhost:8080/register, then log in to the app with it.

To try **Forgot your password?**, use a registered account's email: the `dev` server has no mail server, so it writes the link to its console (`Password reset link for <user>: ...`). Open that link in a browser on the PC to choose the new password. The link uses the address the app called, so from the emulator it starts with `http://10.0.2.2:8080`: change that to `http://localhost:8080` on the PC.

### 2a. In a browser

In a second PowerShell window:

```powershell
cd E:\workspace_self_emp\claude_ws\volunteerportal\mobile
flutter run -d edge          # or: flutter run -d web-server --web-port 5000, then open http://localhost:5000
```

The first time, the debug build takes about a minute to compile and then shows a blank page for up to 40 seconds while it loads; later runs are quicker. Log in with server `http://localhost:8080`. In the terminal, `r` reloads after a code change and `q` quits. There is no camera here, so the Check in tab can't scan; use the QR page's test link on the website instead.

### 2b. On the Android emulator

The Android SDK and an emulator phone (`vp_pixel`, a Pixel 6) are installed on E:. The Android tools must be told so in each PowerShell window you use for them:

```powershell
$env:ANDROID_HOME = 'E:\IDEs\Android\Sdk'
$env:ANDROID_USER_HOME = 'E:\IDEs\Android\user'
$env:ANDROID_AVD_HOME = 'E:\IDEs\Android\user\avd'                       # where the emulator looks for vp_pixel
$env:GRADLE_USER_HOME = 'E:\IDEs\gradle'                                  # Gradle caches several GB
$env:JAVA_TOOL_OPTIONS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT'    # lets Gradle download through the PC's certificates
```

With the last one set, Java prints `Picked up JAVA_TOOL_OPTIONS: ...` when it starts; that is a notice, not an error.

Start the emulator with its window, in one window (with those variables):

```powershell
& E:\IDEs\Android\Sdk\emulator\emulator.exe -avd vp_pixel
```

When the phone has started, in another (with the same variables):

```powershell
cd E:\workspace_self_emp\claude_ws\volunteerportal\mobile
flutter run -d emulator-5554          # the first build takes several minutes
```

In the app, use server **`http://10.0.2.2:8080`**: inside the emulator that address is your PC (`localhost` would be the emulator itself). If "System UI isn't responding" appears at startup, tap **Wait**. `flutter run` keeps the app's data, so the app reopens logged in as whoever used it last; use Profile → **Log out** to switch accounts. The emulator's camera shows a virtual room, so it can't scan a real QR code.

### 2c. On your own Android phone

1. On the phone, turn on **Developer options** (Settings → About phone → tap *Build number* 7 times), then **USB debugging** in them.
2. Connect it by USB and accept the prompt on the phone. `flutter devices` should list it.
3. In a PowerShell window with the variables from 2b: `flutter run`.
4. In the app, use server `http://<your PC's Wi-Fi address>:8080` (the IPv4 address of the Wi-Fi adapter in `ipconfig`); the phone must be on the same Wi-Fi. If it can't connect, allow Java through Windows Firewall on private networks.

Plain `http://` works because these are debug builds (`android/app/src/debug/AndroidManifest.xml` allows it); release builds need `https`. An iPhone needs a Mac with Xcode.

### 3. What to try

| Feature | How |
|---|---|
| Events | Log in as `demo_volunteer`: *Demo Event* is shown as your next event |
| Check-in | On the website as `demo_coordinator`, open Coordinator → *Demo Initiative* → Events → *Demo Event* → **Check-in QR**, and scan it from the app's Check in tab as `demo_volunteer` (a real phone; scan again to check out). Without a camera, open the test link under the QR in a browser logged in to the website as `demo_volunteer`: the app's Events and History then show the check-in |
| Initiatives and joining | Register a new account on the website and log in to the app with it. Initiatives → **Not joined** → open one → answer its questions → **Request to join**. Approve or reject it on the website as `demo_coordinator` (Join Requests), then pull to refresh |
| Change password | Profile (the account button at the top) → **Change password**. The app stays logged in; a website session of the same account is logged out |
| Forgot password | Log out, then **Forgot your password?** on the login screen. The `dev` server has no mail server, so it prints the link in its window (`Password reset link for <user>: ...`); open it in a browser on the PC (from the emulator, change `10.0.2.2` in it to `localhost`) |
| Arabic | The translate button at the top: the whole app turns right-to-left, and server messages come back in Arabic |
| History and Alerts | After checking in, or after a coordinator decides your join request |

To start over as someone else, log out in Profile; to test joining or approval again, register another account (the demo accounts keep their state between starts).

## Code map

| Path | What |
|---|---|
| `lib/api/` | `ApiClient` (bearer token, language, JSON errors → `ApiException`/`UnauthorizedException`) and the JSON models |
| `lib/check_in_flow.dart` | Scan → check in; asks for location only when the server answers `LOCATION_REQUIRED` |
| `lib/app_state.dart` | Server, session, language; `guard()` logs out when the token is rejected |
| `lib/app_scope.dart` | `AppScope` (the app state for every screen), `describeError` (API failures as translated text), and `appRouteObserver`, which tells screens when another screen is opened over them (the scan screen turns the camera off then) |
| `lib/screens/` | Login, home tabs (events, initiatives, scan, history, notifications), profile; `initiatives_screen.dart` has the filter (`InitiativeFilter`, the same rules as the website's `MembershipFilter`) and the office grouping and picker (`inOffice`, `officesOf`, as on the website's `?office=`); `initiative_screen.dart` is one initiative, with its questions, Request to join and Withdraw request; `change_password_screen.dart` (from Profile) and `forgot_password_screen.dart` (from the login screen); `widgets.dart` has the shared list (`AsyncList`, which can also filter what it loaded and show a header such as filter chips, or split it into titled sections with `groups`) and the date helpers (`formatDateTime`, `formatEventTime`) |
| `lib/services/` | The phone's location (`location_service.dart`), the token in secure storage (`session_store.dart`), and turning the camera off when the scanner closes or the app goes to the background (`camera_release*.dart`: on the web, mobile_scanner leaves the camera running, so the app stops it; a no-op on Android and iPhone) |
| `lib/theme.dart` | The website's colours (`VpColors`), the app theme, and the shared `StatusPill` and `LanyardStrip` widgets |
| `assets/fonts/` | Readex Pro (Regular, Medium, SemiBold; Latin and Arabic) and its licence, `OFL.txt` |
| `lib/l10n/app_en.arb`, `app_ar.arb` | Translations (`flutter gen-l10n` generates `app_localizations*.dart`) |
| `test/fake_server.dart` | In-memory stand-in for the API used by the tests |
