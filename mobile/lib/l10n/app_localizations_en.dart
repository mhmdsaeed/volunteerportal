// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'Volunteer Portal';

  @override
  String get serverUrl => 'Server address';

  @override
  String get serverUrlHint => 'https://portal.example.org';

  @override
  String get username => 'Username';

  @override
  String get password => 'Password';

  @override
  String get login => 'Log in';

  @override
  String get loggingIn => 'Logging in...';

  @override
  String get logout => 'Log out';

  @override
  String get wrongCredentials => 'Wrong username or password.';

  @override
  String get cannotReachServer =>
      'Can\'t reach the server. Check the address and your connection.';

  @override
  String get sessionExpired => 'Your session has ended. Please log in again.';

  @override
  String get somethingWentWrong => 'Something went wrong. Please try again.';

  @override
  String get required => 'Required';

  @override
  String get invalidServerUrl =>
      'Enter an address starting with http:// or https://';

  @override
  String get tabEvents => 'Events';

  @override
  String get tabScan => 'Check in';

  @override
  String get tabHistory => 'History';

  @override
  String get tabNotifications => 'Notifications';

  @override
  String get profile => 'Profile';

  @override
  String get language => 'Language';

  @override
  String get retry => 'Retry';

  @override
  String get noEvents =>
      'No upcoming events. Once an initiative approves your request, its events appear here.';

  @override
  String get noHistory => 'You haven\'t checked in to any event yet.';

  @override
  String get noNotifications => 'No notifications yet.';

  @override
  String get markAllRead => 'Mark all as read';

  @override
  String get statusNotCheckedIn => 'Not checked in';

  @override
  String get statusCheckedIn => 'Checked in';

  @override
  String get statusCheckedOut => 'Checked out';

  @override
  String get locationChecked => 'Location is checked';

  @override
  String get checkIn => 'Check in';

  @override
  String get checkOut => 'Check out';

  @override
  String get scanHint => 'Point the camera at the event\'s check-in QR code.';

  @override
  String get checkingIn => 'Checking in...';

  @override
  String get gettingLocation => 'Getting your location...';

  @override
  String get locationDenied =>
      'Location access is needed to check in to this event. Allow it in your phone\'s settings.';

  @override
  String get notACheckInCode => 'This isn\'t an event check-in QR code.';

  @override
  String get scanAgain => 'Scan again';

  @override
  String get done => 'Done';

  @override
  String get grade => 'Grade';

  @override
  String get points => 'Points';

  @override
  String get unranked => 'Unranked';

  @override
  String get roles => 'Roles';

  @override
  String get cameraUnavailable =>
      'The camera isn\'t available. Allow camera access in your phone\'s settings.';
}
