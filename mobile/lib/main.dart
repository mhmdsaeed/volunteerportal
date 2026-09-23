import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'app_scope.dart';
import 'app_state.dart';
import 'l10n/app_localizations.dart';
import 'screens/home_screen.dart';
import 'screens/login_screen.dart';
import 'screens/scan_screen.dart';
import 'services/session_store.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final state = AppState(store: const SecureSessionStore());
  runApp(VolunteerApp(state: state));
  await state.load();
}

class VolunteerApp extends StatelessWidget {
  const VolunteerApp({super.key, required this.state, this.scannerBuilder = cameraScanner});

  final AppState state;
  final ScannerBuilder scannerBuilder;

  @override
  Widget build(BuildContext context) {
    return AppScope(
      state: state,
      child: ListenableBuilder(
        listenable: state,
        builder: (context, _) => MaterialApp(
          onGenerateTitle: (context) => AppLocalizations.of(context).appTitle,
          debugShowCheckedModeBanner: false,
          theme: ThemeData(colorSchemeSeed: const Color(0xFF0D6EFD), useMaterial3: true),
          locale: Locale(state.language),
          supportedLocales: AppLocalizations.supportedLocales,
          localizationsDelegates: const [
            AppLocalizations.delegate,
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          home: !state.ready
              ? const Scaffold(body: Center(child: CircularProgressIndicator()))
              : state.loggedIn
                  ? HomeScreen(scannerBuilder: scannerBuilder)
                  : const LoginScreen(),
        ),
      ),
    );
  }
}
