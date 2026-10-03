import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:volunteer_portal_app/l10n/app_localizations.dart';
import 'package:volunteer_portal_app/screens/profile_screen.dart';

void main() {
  final en = lookupAppLocalizations(const Locale('en'));
  final ar = lookupAppLocalizations(const Locale('ar'));

  test('role names are translated, with the website\'s wording', () {
    expect(roleNames(en, ['ADMIN', 'COORDINATOR', 'VOLUNTEER']), 'Admin, Coordinator, Volunteer');
    expect(roleNames(ar, ['ADMIN', 'COORDINATOR', 'VOLUNTEER']), 'مسؤول، منسق، متطوع');
  });

  test('a role the app does not know is shown as the server sends it', () {
    expect(roleNames(en, ['VOLUNTEER', 'AUDITOR']), 'Volunteer, AUDITOR');
  });
}
