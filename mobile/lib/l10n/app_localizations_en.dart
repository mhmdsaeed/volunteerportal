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
  String get loginTagline =>
      'Join initiatives, see your upcoming events, and check in with your phone when you arrive.';

  @override
  String get nextEvent => 'Your next event';

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
  String get tabScanShort => 'Check in';

  @override
  String get tabHistory => 'History';

  @override
  String get tabNotifications => 'Notifications';

  @override
  String get tabNotificationsShort => 'Alerts';

  @override
  String get tabInitiatives => 'Initiatives';

  @override
  String get filterJoined => 'Joined';

  @override
  String get filterPending => 'Pending';

  @override
  String get filterRejected => 'Not approved';

  @override
  String get filterNotJoined => 'Not joined';

  @override
  String get filterAll => 'All';

  @override
  String get membershipNone => 'Not joined';

  @override
  String get membershipPending => 'Pending';

  @override
  String get membershipApproved => 'Approved';

  @override
  String get membershipRejected => 'Not approved';

  @override
  String get noJoinedInitiatives => 'You haven\'t joined any initiatives yet.';

  @override
  String get noInitiativesHere => 'No initiatives here.';

  @override
  String get noInitiativesOpen => 'No initiatives are open right now.';

  @override
  String get showInitiativesToJoin => 'Show initiatives you can join';

  @override
  String get tapToJoin =>
      'Tap an initiative to answer its questions and ask to join.';

  @override
  String get requestToJoin => 'Request to join';

  @override
  String get joinQuestionsIntro =>
      'Answer these questions, then send your request.';

  @override
  String get yes => 'Yes';

  @override
  String get no => 'No';

  @override
  String get yourAnswer => 'Your answer';

  @override
  String get requestSent => 'Request sent. A coordinator will review it.';

  @override
  String get pendingHint =>
      'Your request is waiting for review. Once it\'s approved, you can attend this initiative\'s events.';

  @override
  String get withdrawRequest => 'Withdraw request';

  @override
  String get withdrawConfirm =>
      'Withdraw your request to join this initiative?';

  @override
  String get cancel => 'Cancel';

  @override
  String get requestWithdrawn => 'Request withdrawn.';

  @override
  String get memberHint =>
      'You\'re a member. This initiative\'s events are on the Events tab.';

  @override
  String get notApprovedHint => 'Your request to join wasn\'t approved.';

  @override
  String get alreadyRequested =>
      'You\'ve already asked to join this initiative.';

  @override
  String get alreadyReviewed =>
      'Your request has already been reviewed, so it can\'t be withdrawn.';

  @override
  String get initiativeClosed => 'This initiative isn\'t open any more.';

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
  String get roleAdmin => 'Admin';

  @override
  String get roleCoordinator => 'Coordinator';

  @override
  String get roleVolunteer => 'Volunteer';

  @override
  String get cameraUnavailable =>
      'The camera isn\'t available. Allow camera access in your phone\'s settings.';

  @override
  String get forgotPassword => 'Forgot your password?';

  @override
  String get forgotPasswordTitle => 'Reset password';

  @override
  String get forgotPasswordIntro =>
      'Enter the email address of your account. We\'ll email you a link to choose a new password on the website.';

  @override
  String get email => 'Email';

  @override
  String get invalidEmail => 'Enter a valid email address';

  @override
  String get sendResetLink => 'Send reset link';

  @override
  String get resetLinkSent =>
      'If an active account uses that email, a link to reset its password is on its way. Check your inbox and spam folder; the link works only once.';

  @override
  String get resetUnavailable =>
      'This server can\'t send email. Ask an administrator to set a new password for you.';

  @override
  String get backToLogin => 'Back to login';

  @override
  String get changePassword => 'Change password';

  @override
  String get currentPassword => 'Current password';

  @override
  String get newPassword => 'New password';

  @override
  String get confirmNewPassword => 'Confirm new password';

  @override
  String get changePasswordHelp =>
      'At least 8 characters. You\'ll stay logged in here, and be logged out of the website and your other devices.';

  @override
  String get passwordTooShort => 'Password must be at least 8 characters';

  @override
  String get passwordsDontMatch => 'Passwords do not match';

  @override
  String get wrongCurrentPassword => 'Your current password is wrong.';

  @override
  String get passwordChanged =>
      'Password changed. You\'ve been logged out of the website and your other devices.';
}
