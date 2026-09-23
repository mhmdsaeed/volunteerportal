import 'package:flutter/widgets.dart';

import 'api/api_client.dart';
import 'app_state.dart';
import 'l10n/app_localizations.dart';

/// Makes the [AppState] available to every screen and rebuilds them when it changes.
class AppScope extends InheritedNotifier<AppState> {
  const AppScope({super.key, required AppState state, required super.child}) : super(notifier: state);

  static AppState of(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<AppScope>()!.notifier!;

  /// Like [of], without rebuilding when the state changes (for callbacks).
  static AppState read(BuildContext context) => context.getInheritedWidgetOfExactType<AppScope>()!.notifier!;
}

/// A short, translated description of an API failure for the user.
String describeError(BuildContext context, Object error) {
  final t = AppLocalizations.of(context);
  if (error is UnauthorizedException) {
    return t.sessionExpired;
  }
  if (error is ApiException) {
    if (error.isUnreachable) {
      return t.cannotReachServer;
    }
    if (error.error == 'invalid_credentials') {
      return t.wrongCredentials;
    }
  }
  return t.somethingWentWrong;
}
