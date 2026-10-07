import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:volunteer_portal_app/api/api_client.dart';
import 'package:volunteer_portal_app/app_state.dart';
import 'package:volunteer_portal_app/main.dart';
import 'package:volunteer_portal_app/screens/scan_screen.dart';
import 'package:volunteer_portal_app/services/location_service.dart';
import 'package:volunteer_portal_app/services/session_store.dart';

import 'fake_server.dart';

class NoLocation implements LocationService {
  @override
  Future<Coordinates?> current() async => null;
}

/// Stands in for the camera: a button that "scans" the given text.
ScannerBuilder fakeScanner(String code) =>
    (context, onCode) => Center(child: ElevatedButton(key: const Key('fakeScan'), onPressed: () => onCode(code), child: const Text('scan')));

void main() {
  late FakeServer server;
  late MemorySessionStore store;

  AppState newState() => AppState(
        store: store,
        location: NoLocation(),
        clientFactory: (url) => ApiClient(baseUrl: url, httpClient: server.client),
      );

  Future<AppState> startApp(WidgetTester tester, {String qr = FakeServer.validQr}) async {
    final state = newState();
    await tester.pumpWidget(VolunteerApp(state: state, scannerBuilder: fakeScanner(qr)));
    await state.load();
    await tester.pumpAndSettle();
    return state;
  }

  /// The test screen (800x600) is shorter than a phone, so the button sits below the fold: scroll to it first.
  Future<void> tapLogin(WidgetTester tester) async {
    await tester.ensureVisible(find.byKey(const Key('loginButton')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('loginButton')));
  }

  Future<void> logIn(WidgetTester tester, {String password = 'demo12345'}) async {
    await tester.enterText(find.byKey(const Key('server')), 'portal.example.org');
    await tester.enterText(find.byKey(const Key('username')), 'demo_volunteer');
    await tester.enterText(find.byKey(const Key('password')), password);
    await tapLogin(tester);
    await tester.pumpAndSettle();
  }

  /// Taps a filter chip on the Initiatives tab, scrolling the chip row to it first (it scrolls sideways).
  Future<void> showFilter(WidgetTester tester, String key) async {
    await tester.ensureVisible(find.byKey(Key(key)));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(Key(key)));
  }

  setUp(() {
    server = FakeServer();
    store = MemorySessionStore();
  });

  testWidgets('logging in shows my events and remembers the session', (tester) async {
    await startApp(tester);
    expect(find.byKey(const Key('loginButton')), findsOneWidget);

    await logIn(tester);

    expect(find.text('Demo Event'), findsOneWidget);
    expect(find.text('Not checked in'), findsOneWidget);
    expect(store.values[SessionStore.token], FakeServer.token);
    expect(store.values[SessionStore.serverUrl], 'https://portal.example.org');
  });

  testWidgets('wrong password shows an error and stays on the login screen', (tester) async {
    await startApp(tester);

    await logIn(tester, password: 'nope');

    expect(find.text('Wrong username or password.'), findsOneWidget);
    expect(find.byKey(const Key('loginButton')), findsOneWidget);
    expect(store.values[SessionStore.token], isNull);
  });

  testWidgets('empty fields are required', (tester) async {
    await startApp(tester);

    await tapLogin(tester);
    await tester.pumpAndSettle();

    expect(find.text('Required'), findsNWidgets(3));
    expect(server.requests, isEmpty);
  });

  testWidgets('a saved session opens straight into the app', (tester) async {
    store.values.addAll({SessionStore.serverUrl: 'https://portal.example.org', SessionStore.token: FakeServer.token});

    await startApp(tester);

    expect(find.text('Demo Event'), findsOneWidget);
  });

  testWidgets('scanning the event QR checks in, and Events and History follow', (tester) async {
    await startApp(tester);
    await logIn(tester);

    await tester.tap(find.byKey(const Key('scanTab')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('fakeScan')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('scanSuccess')), findsOneWidget);
    expect(find.text('You are checked in. Welcome!'), findsOneWidget);

    await tester.tap(find.text('Events'));
    await tester.pumpAndSettle();
    expect(find.text('Checked in'), findsOneWidget);

    await tester.tap(find.text('History'));
    await tester.pumpAndSettle();
    expect(find.text('Demo Event'), findsOneWidget);
    expect(find.text('Check in'), findsWidgets);
  });

  testWidgets('scanning some other QR code says it is not a check-in code', (tester) async {
    await startApp(tester, qr: 'WIFI:S:office;;');
    await logIn(tester);

    await tester.tap(find.byKey(const Key('scanTab')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('fakeScan')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('scanProblem')), findsOneWidget);
    expect(find.text("This isn't an event check-in QR code."), findsOneWidget);

    await tester.tap(find.byKey(const Key('scanAgain')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('fakeScan')), findsOneWidget);
  });

  testWidgets('the camera is off while Profile covers the Check in tab, and back after', (tester) async {
    await startApp(tester);
    await logIn(tester);
    await tester.tap(find.byKey(const Key('scanTab')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('fakeScan')), findsOneWidget);

    await tester.tap(find.byKey(const Key('profileButton')));
    await tester.pumpAndSettle();
    // skipOffstage: false, or the covered screen's scanner wouldn't be found even if it were still there
    expect(find.byKey(const Key('fakeScan'), skipOffstage: false), findsNothing);

    await tester.pageBack();
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('fakeScan')), findsOneWidget);
  });

  testWidgets('a revoked token sends the user back to login with an explanation', (tester) async {
    await startApp(tester);
    await logIn(tester);
    server.tokenRevoked = true;

    await tester.tap(find.text('History'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('loginButton')), findsOneWidget);
    expect(find.text('Your session has ended. Please log in again.'), findsOneWidget);
    expect(store.values[SessionStore.token], isNull);
  });

  testWidgets('Initiatives opens on the ones I joined and filters by my membership', (tester) async {
    await startApp(tester);
    await logIn(tester);

    await tester.tap(find.byKey(const Key('initiativesTab')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('initiative-487')), findsOneWidget);
    expect(find.text('Demo Initiative'), findsOneWidget);
    expect(find.text('Approved'), findsOneWidget);
    expect(find.text('Library Reading'), findsNothing);
    expect(find.descendant(of: find.byKey(const Key('filter-all')), matching: find.text('4')), findsOneWidget);
    expect(find.descendant(of: find.byKey(const Key('filter-joined')), matching: find.text('1')), findsOneWidget);

    await showFilter(tester, 'filter-notJoined');
    await tester.pumpAndSettle();
    expect(find.text('Library Reading'), findsOneWidget);
    expect(find.text('Demo Initiative'), findsNothing);
    expect(find.text('Tap an initiative to answer its questions and ask to join.'), findsOneWidget);

    await showFilter(tester, 'filter-pending');
    await tester.pumpAndSettle();
    expect(find.text('Beach Clean-up'), findsOneWidget);

    await showFilter(tester, 'filter-rejected');
    await tester.pumpAndSettle();
    expect(find.text('Food Bank'), findsOneWidget);
    expect(find.text('Not approved'), findsNWidgets(2)); // the chip and the pill

    await showFilter(tester, 'filter-all');
    await tester.pumpAndSettle();
    for (final id in [487, 488, 489, 490]) {
      expect(find.byKey(Key('initiative-$id')), findsOneWidget);
    }
    // Switching filters reuses the loaded list
    expect(server.requests.where((r) => r.url.path == '/api/initiatives'), hasLength(1));
  });

  /// Opens an initiative from the Initiatives tab, under the given filter.
  Future<void> openInitiative(WidgetTester tester, String filter, int id) async {
    await tester.tap(find.byKey(const Key('initiativesTab')));
    await tester.pumpAndSettle();
    await showFilter(tester, filter);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(Key('initiative-$id')));
    await tester.pumpAndSettle();
  }

  /// Taps something on the initiative screen, scrolling down to it first: the list only builds what is near the screen.
  Future<void> tapVisible(WidgetTester tester, Key key) async {
    await tester.dragUntilVisible(find.byKey(key), find.byType(ListView), const Offset(0, -200));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.byKey(key));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(key));
    await tester.pumpAndSettle();
  }

  testWidgets('joining from the app sends the answers, and the request then waits for review', (tester) async {
    await startApp(tester);
    await logIn(tester);
    await openInitiative(tester, 'filter-notJoined', 490);

    expect(find.text('Reading to children.'), findsOneWidget);
    expect(find.text('Are you over 18?'), findsOneWidget);
    await tapVisible(tester, const Key('answer-11-1')); // yes
    await tapVisible(tester, const Key('answer-12-1'));
    await tapVisible(tester, const Key('answer-12-2')); // one choice: Evening replaces Morning
    await tapVisible(tester, const Key('answer-13-3'));
    await tapVisible(tester, const Key('answer-13-1')); // many choices: Reading and Tidying
    await tester.ensureVisible(find.byKey(const Key('answer-14')));
    await tester.enterText(find.byKey(const Key('answer-14')), '  I like books ');
    await tapVisible(tester, const Key('joinButton'));

    expect(server.joinAnswers, {
      '11': ['1'],
      '12': ['2'],
      '13': ['1', '3'],
      '14': ['I like books'],
    });
    expect(find.text('Request sent. A coordinator will review it.'), findsOneWidget);
    // Back on the list, reloaded: it is no longer under Not joined, but under Pending
    expect(find.text('Library Reading'), findsNothing);
    await showFilter(tester, 'filter-pending');
    await tester.pumpAndSettle();
    expect(find.text('Library Reading'), findsOneWidget);
  });

  testWidgets('a pending request can be withdrawn after confirming', (tester) async {
    await startApp(tester);
    await logIn(tester);
    await openInitiative(tester, 'filter-pending', 488);

    expect(find.byKey(const Key('joinButton')), findsNothing);
    await tapVisible(tester, const Key('withdrawButton'));
    await tester.tap(find.text('Cancel'));
    await tester.pumpAndSettle();
    expect(server.requests.where((r) => r.url.path.endsWith('/withdraw')), isEmpty);

    await tapVisible(tester, const Key('withdrawButton'));
    await tester.tap(find.byKey(const Key('confirmWithdraw')));
    await tester.pumpAndSettle();

    expect(find.text('Request withdrawn.'), findsOneWidget);
    expect(find.text('Beach Clean-up'), findsNothing);
    await showFilter(tester, 'filter-notJoined');
    await tester.pumpAndSettle();
    expect(find.text('Beach Clean-up'), findsOneWidget);
  });

  testWidgets('a joined initiative says where its events are, with nothing to send', (tester) async {
    await startApp(tester);
    await logIn(tester);
    await openInitiative(tester, 'filter-joined', 487);

    expect(find.text("You're a member. This initiative's events are on the Events tab."), findsOneWidget);
    expect(find.byKey(const Key('joinButton')), findsNothing);
    expect(find.byKey(const Key('withdrawButton')), findsNothing);
  });

  testWidgets('a refused join request says why and stays on the initiative', (tester) async {
    await startApp(tester);
    await logIn(tester);
    await openInitiative(tester, 'filter-notJoined', 490);
    server.initiatives.firstWhere((i) => i['id'] == 490)['membership'] = 'PENDING'; // asked meanwhile, e.g. on the website

    await tapVisible(tester, const Key('joinButton'));

    expect(find.text("You've already asked to join this initiative."), findsOneWidget);
    expect(find.byKey(const Key('joinButton')), findsOneWidget);
  });

  testWidgets('with nothing joined, Initiatives points to the ones I can join', (tester) async {
    server.initiatives = [
      {'id': 490, 'name': 'Library Reading', 'description': null, 'office': null, 'membership': 'NONE'},
    ];
    await startApp(tester);
    await logIn(tester);

    await tester.tap(find.byKey(const Key('initiativesTab')));
    await tester.pumpAndSettle();
    expect(find.text("You haven't joined any initiatives yet."), findsOneWidget);
    expect(find.text('Library Reading'), findsNothing);

    await tester.tap(find.byKey(const Key('showInitiativesToJoin')));
    await tester.pumpAndSettle();
    expect(find.text('Library Reading'), findsOneWidget);
    expect(tester.widget<ChoiceChip>(find.byKey(const Key('filter-notJoined'))).selected, isTrue);

    await showFilter(tester, 'filter-pending');
    await tester.pumpAndSettle();
    expect(find.text('No initiatives here.'), findsOneWidget);
    expect(find.byKey(const Key('showInitiativesToJoin')), findsNothing);
  });

  testWidgets('Arabic turns the layout right-to-left, translates the app and is sent to the server', (tester) async {
    await startApp(tester);
    await logIn(tester);

    final state = newState();
    await state.setLanguage('ar'); // same store: remembered for next launch
    expect(store.values[SessionStore.language], 'ar');

    await tester.pumpWidget(VolunteerApp(state: state, scannerBuilder: fakeScanner(FakeServer.validQr)));
    await state.load();
    await tester.pumpAndSettle();

    expect(find.text('الفعاليات'), findsWidgets);
    expect(find.text('المبادرات'), findsOneWidget); // the Initiatives tab
    expect(Directionality.of(tester.element(find.text('Demo Event'))), TextDirection.rtl);
    // Arabic month and weekday names, but Western digits like the website
    expect(find.textContaining('23 سبتمبر'), findsOneWidget);
    expect(find.textContaining('08:00'), findsOneWidget);
    expect(find.textContaining('٢٣'), findsNothing);

    await tester.tap(find.text('الإشعارات'));
    await tester.pumpAndSettle();
    expect(find.text('تم قبول طلب انضمامك'), findsOneWidget);
    expect(find.textContaining('2026'), findsOneWidget); // the notification's date, Western digits too
    expect(server.requests.last.headers['Accept-Language'], 'ar');
  });

  testWidgets('logging out revokes the token and returns to login', (tester) async {
    await startApp(tester);
    await logIn(tester);

    await tester.tap(find.byKey(const Key('profileButton')));
    await tester.pumpAndSettle();
    expect(find.text('demo_volunteer'), findsOneWidget);
    expect(find.text('Volunteer'), findsOneWidget); // the VOLUNTEER role, translated
    await tester.tap(find.byKey(const Key('logoutButton')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('loginButton')), findsOneWidget);
    expect(server.tokenRevoked, isTrue);
    expect(store.values[SessionStore.token], isNull);
    expect(store.values[SessionStore.serverUrl], 'https://portal.example.org'); // kept for next login
  });

  testWidgets('changing my password from Profile keeps me logged in', (tester) async {
    await startApp(tester);
    await logIn(tester);
    await tester.tap(find.byKey(const Key('profileButton')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('changePasswordButton')));
    await tester.pumpAndSettle();

    // Checked in the app first: too short, then not the same twice
    await tester.enterText(find.byKey(const Key('currentPassword')), 'demo12345');
    await tester.enterText(find.byKey(const Key('newPassword')), 'short');
    await tester.enterText(find.byKey(const Key('confirmNewPassword')), 'other');
    await tester.tap(find.byKey(const Key('savePassword')));
    await tester.pumpAndSettle();
    expect(find.text('Password must be at least 8 characters'), findsOneWidget);
    expect(find.text('Passwords do not match'), findsOneWidget);
    expect(server.requests.where((r) => r.url.path == '/api/auth/password'), isEmpty);

    // The server refuses a wrong current password
    await tester.enterText(find.byKey(const Key('currentPassword')), 'not-it');
    await tester.enterText(find.byKey(const Key('newPassword')), 'brand-new-pass');
    await tester.enterText(find.byKey(const Key('confirmNewPassword')), 'brand-new-pass');
    await tester.tap(find.byKey(const Key('savePassword')));
    await tester.pumpAndSettle();
    expect(find.text('Your current password is wrong.'), findsOneWidget);

    await tester.enterText(find.byKey(const Key('currentPassword')), 'demo12345');
    await tester.tap(find.byKey(const Key('savePassword')));
    await tester.pumpAndSettle();

    expect(server.password, 'brand-new-pass');
    expect(find.textContaining('Password changed.'), findsOneWidget);
    expect(find.byKey(const Key('logoutButton')), findsOneWidget); // back on Profile, still logged in
    expect(store.values[SessionStore.token], FakeServer.token);
  });

  testWidgets('a forgotten password: the login screen asks the server to email a reset link', (tester) async {
    await startApp(tester);
    await tester.enterText(find.byKey(const Key('server')), 'portal.example.org');
    await tester.tap(find.byKey(const Key('forgotPassword')));
    await tester.pumpAndSettle();

    // The server typed on the login screen is carried over
    expect(find.widgetWithText(TextFormField, 'portal.example.org'), findsOneWidget);
    await tester.enterText(find.byKey(const Key('resetEmail')), 'not-an-email');
    await tester.tap(find.byKey(const Key('sendResetLink')));
    await tester.pumpAndSettle();
    expect(find.text('Enter a valid email address'), findsOneWidget);

    await tester.enterText(find.byKey(const Key('resetEmail')), ' volunteer@example.org ');
    await tester.tap(find.byKey(const Key('sendResetLink')));
    await tester.pumpAndSettle();

    expect(server.resetEmail, 'volunteer@example.org');
    expect(server.requests.last.url.toString(), 'https://portal.example.org/api/auth/forgot-password');
    expect(find.byKey(const Key('resetSent')), findsOneWidget);
    await tester.tap(find.byKey(const Key('backToLogin')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('loginButton')), findsOneWidget);
  });

  testWidgets('a server that cannot send email says to ask an administrator', (tester) async {
    server.resetAvailable = false;
    await startApp(tester);
    await tester.tap(find.byKey(const Key('forgotPassword')));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('resetServer')), 'portal.example.org');
    await tester.enterText(find.byKey(const Key('resetEmail')), 'volunteer@example.org');
    await tester.tap(find.byKey(const Key('sendResetLink')));
    await tester.pumpAndSettle();

    expect(find.text("This server can't send email. Ask an administrator to set a new password for you."), findsOneWidget);
    expect(find.byKey(const Key('resetSent')), findsNothing);
  });
}
