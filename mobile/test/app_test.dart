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

  Future<void> logIn(WidgetTester tester, {String password = 'demo12345'}) async {
    await tester.enterText(find.byKey(const Key('server')), 'portal.example.org');
    await tester.enterText(find.byKey(const Key('username')), 'demo_volunteer');
    await tester.enterText(find.byKey(const Key('password')), password);
    await tester.tap(find.byKey(const Key('loginButton')));
    await tester.pumpAndSettle();
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

    await tester.tap(find.byKey(const Key('loginButton')));
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
    expect(Directionality.of(tester.element(find.text('Demo Event'))), TextDirection.rtl);

    await tester.tap(find.text('الإشعارات'));
    await tester.pumpAndSettle();
    expect(find.text('تم قبول طلب انضمامك'), findsOneWidget);
    expect(server.requests.last.headers['Accept-Language'], 'ar');
  });

  testWidgets('logging out revokes the token and returns to login', (tester) async {
    await startApp(tester);
    await logIn(tester);

    await tester.tap(find.byKey(const Key('profileButton')));
    await tester.pumpAndSettle();
    expect(find.text('demo_volunteer'), findsOneWidget);
    await tester.tap(find.byKey(const Key('logoutButton')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('loginButton')), findsOneWidget);
    expect(server.tokenRevoked, isTrue);
    expect(store.values[SessionStore.token], isNull);
    expect(store.values[SessionStore.serverUrl], 'https://portal.example.org'); // kept for next login
  });
}
