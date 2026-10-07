import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_ar.dart';
import 'app_localizations_en.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('ar'),
    Locale('en'),
  ];

  /// No description provided for @appTitle.
  ///
  /// In en, this message translates to:
  /// **'Volunteer Portal'**
  String get appTitle;

  /// No description provided for @loginTagline.
  ///
  /// In en, this message translates to:
  /// **'Join initiatives, see your upcoming events, and check in with your phone when you arrive.'**
  String get loginTagline;

  /// No description provided for @nextEvent.
  ///
  /// In en, this message translates to:
  /// **'Your next event'**
  String get nextEvent;

  /// No description provided for @serverUrl.
  ///
  /// In en, this message translates to:
  /// **'Server address'**
  String get serverUrl;

  /// No description provided for @serverUrlHint.
  ///
  /// In en, this message translates to:
  /// **'https://portal.example.org'**
  String get serverUrlHint;

  /// No description provided for @username.
  ///
  /// In en, this message translates to:
  /// **'Username'**
  String get username;

  /// No description provided for @password.
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get password;

  /// No description provided for @login.
  ///
  /// In en, this message translates to:
  /// **'Log in'**
  String get login;

  /// No description provided for @loggingIn.
  ///
  /// In en, this message translates to:
  /// **'Logging in...'**
  String get loggingIn;

  /// No description provided for @logout.
  ///
  /// In en, this message translates to:
  /// **'Log out'**
  String get logout;

  /// No description provided for @wrongCredentials.
  ///
  /// In en, this message translates to:
  /// **'Wrong username or password.'**
  String get wrongCredentials;

  /// No description provided for @cannotReachServer.
  ///
  /// In en, this message translates to:
  /// **'Can\'t reach the server. Check the address and your connection.'**
  String get cannotReachServer;

  /// No description provided for @sessionExpired.
  ///
  /// In en, this message translates to:
  /// **'Your session has ended. Please log in again.'**
  String get sessionExpired;

  /// No description provided for @somethingWentWrong.
  ///
  /// In en, this message translates to:
  /// **'Something went wrong. Please try again.'**
  String get somethingWentWrong;

  /// No description provided for @required.
  ///
  /// In en, this message translates to:
  /// **'Required'**
  String get required;

  /// No description provided for @invalidServerUrl.
  ///
  /// In en, this message translates to:
  /// **'Enter an address starting with http:// or https://'**
  String get invalidServerUrl;

  /// No description provided for @tabEvents.
  ///
  /// In en, this message translates to:
  /// **'Events'**
  String get tabEvents;

  /// No description provided for @tabScan.
  ///
  /// In en, this message translates to:
  /// **'Check in'**
  String get tabScan;

  /// Bottom tab label for the Check in tab; must fit one line on a 360-wide phone. The screen title uses tabScan.
  ///
  /// In en, this message translates to:
  /// **'Check in'**
  String get tabScanShort;

  /// No description provided for @tabHistory.
  ///
  /// In en, this message translates to:
  /// **'History'**
  String get tabHistory;

  /// No description provided for @tabNotifications.
  ///
  /// In en, this message translates to:
  /// **'Notifications'**
  String get tabNotifications;

  /// Bottom tab label for the Notifications tab; five tabs share a 360-wide phone, so it must be short. The screen title uses tabNotifications.
  ///
  /// In en, this message translates to:
  /// **'Alerts'**
  String get tabNotificationsShort;

  /// No description provided for @tabInitiatives.
  ///
  /// In en, this message translates to:
  /// **'Initiatives'**
  String get tabInitiatives;

  /// No description provided for @filterJoined.
  ///
  /// In en, this message translates to:
  /// **'Joined'**
  String get filterJoined;

  /// No description provided for @filterPending.
  ///
  /// In en, this message translates to:
  /// **'Pending'**
  String get filterPending;

  /// No description provided for @filterRejected.
  ///
  /// In en, this message translates to:
  /// **'Not approved'**
  String get filterRejected;

  /// No description provided for @filterNotJoined.
  ///
  /// In en, this message translates to:
  /// **'Not joined'**
  String get filterNotJoined;

  /// No description provided for @filterAll.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get filterAll;

  /// No description provided for @membershipNone.
  ///
  /// In en, this message translates to:
  /// **'Not joined'**
  String get membershipNone;

  /// No description provided for @membershipPending.
  ///
  /// In en, this message translates to:
  /// **'Pending'**
  String get membershipPending;

  /// No description provided for @membershipApproved.
  ///
  /// In en, this message translates to:
  /// **'Approved'**
  String get membershipApproved;

  /// No description provided for @membershipRejected.
  ///
  /// In en, this message translates to:
  /// **'Not approved'**
  String get membershipRejected;

  /// No description provided for @noJoinedInitiatives.
  ///
  /// In en, this message translates to:
  /// **'You haven\'t joined any initiatives yet.'**
  String get noJoinedInitiatives;

  /// No description provided for @noInitiativesHere.
  ///
  /// In en, this message translates to:
  /// **'No initiatives here.'**
  String get noInitiativesHere;

  /// No description provided for @noInitiativesOpen.
  ///
  /// In en, this message translates to:
  /// **'No initiatives are open right now.'**
  String get noInitiativesOpen;

  /// No description provided for @showInitiativesToJoin.
  ///
  /// In en, this message translates to:
  /// **'Show initiatives you can join'**
  String get showInitiativesToJoin;

  /// No description provided for @tapToJoin.
  ///
  /// In en, this message translates to:
  /// **'Tap an initiative to answer its questions and ask to join.'**
  String get tapToJoin;

  /// No description provided for @requestToJoin.
  ///
  /// In en, this message translates to:
  /// **'Request to join'**
  String get requestToJoin;

  /// No description provided for @joinQuestionsIntro.
  ///
  /// In en, this message translates to:
  /// **'Answer these questions, then send your request.'**
  String get joinQuestionsIntro;

  /// No description provided for @yes.
  ///
  /// In en, this message translates to:
  /// **'Yes'**
  String get yes;

  /// No description provided for @no.
  ///
  /// In en, this message translates to:
  /// **'No'**
  String get no;

  /// No description provided for @yourAnswer.
  ///
  /// In en, this message translates to:
  /// **'Your answer'**
  String get yourAnswer;

  /// No description provided for @requestSent.
  ///
  /// In en, this message translates to:
  /// **'Request sent. A coordinator will review it.'**
  String get requestSent;

  /// No description provided for @pendingHint.
  ///
  /// In en, this message translates to:
  /// **'Your request is waiting for review. Once it\'s approved, you can attend this initiative\'s events.'**
  String get pendingHint;

  /// No description provided for @withdrawRequest.
  ///
  /// In en, this message translates to:
  /// **'Withdraw request'**
  String get withdrawRequest;

  /// No description provided for @withdrawConfirm.
  ///
  /// In en, this message translates to:
  /// **'Withdraw your request to join this initiative?'**
  String get withdrawConfirm;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @requestWithdrawn.
  ///
  /// In en, this message translates to:
  /// **'Request withdrawn.'**
  String get requestWithdrawn;

  /// No description provided for @memberHint.
  ///
  /// In en, this message translates to:
  /// **'You\'re a member. This initiative\'s events are on the Events tab.'**
  String get memberHint;

  /// No description provided for @notApprovedHint.
  ///
  /// In en, this message translates to:
  /// **'Your request to join wasn\'t approved.'**
  String get notApprovedHint;

  /// No description provided for @alreadyRequested.
  ///
  /// In en, this message translates to:
  /// **'You\'ve already asked to join this initiative.'**
  String get alreadyRequested;

  /// No description provided for @alreadyReviewed.
  ///
  /// In en, this message translates to:
  /// **'Your request has already been reviewed, so it can\'t be withdrawn.'**
  String get alreadyReviewed;

  /// No description provided for @initiativeClosed.
  ///
  /// In en, this message translates to:
  /// **'This initiative isn\'t open any more.'**
  String get initiativeClosed;

  /// No description provided for @profile.
  ///
  /// In en, this message translates to:
  /// **'Profile'**
  String get profile;

  /// No description provided for @language.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get language;

  /// No description provided for @retry.
  ///
  /// In en, this message translates to:
  /// **'Retry'**
  String get retry;

  /// No description provided for @noEvents.
  ///
  /// In en, this message translates to:
  /// **'No upcoming events. Once an initiative approves your request, its events appear here.'**
  String get noEvents;

  /// No description provided for @noHistory.
  ///
  /// In en, this message translates to:
  /// **'You haven\'t checked in to any event yet.'**
  String get noHistory;

  /// No description provided for @noNotifications.
  ///
  /// In en, this message translates to:
  /// **'No notifications yet.'**
  String get noNotifications;

  /// No description provided for @markAllRead.
  ///
  /// In en, this message translates to:
  /// **'Mark all as read'**
  String get markAllRead;

  /// No description provided for @statusNotCheckedIn.
  ///
  /// In en, this message translates to:
  /// **'Not checked in'**
  String get statusNotCheckedIn;

  /// No description provided for @statusCheckedIn.
  ///
  /// In en, this message translates to:
  /// **'Checked in'**
  String get statusCheckedIn;

  /// No description provided for @statusCheckedOut.
  ///
  /// In en, this message translates to:
  /// **'Checked out'**
  String get statusCheckedOut;

  /// No description provided for @locationChecked.
  ///
  /// In en, this message translates to:
  /// **'Location is checked'**
  String get locationChecked;

  /// No description provided for @checkIn.
  ///
  /// In en, this message translates to:
  /// **'Check in'**
  String get checkIn;

  /// No description provided for @checkOut.
  ///
  /// In en, this message translates to:
  /// **'Check out'**
  String get checkOut;

  /// No description provided for @scanHint.
  ///
  /// In en, this message translates to:
  /// **'Point the camera at the event\'s check-in QR code.'**
  String get scanHint;

  /// No description provided for @checkingIn.
  ///
  /// In en, this message translates to:
  /// **'Checking in...'**
  String get checkingIn;

  /// No description provided for @gettingLocation.
  ///
  /// In en, this message translates to:
  /// **'Getting your location...'**
  String get gettingLocation;

  /// No description provided for @locationDenied.
  ///
  /// In en, this message translates to:
  /// **'Location access is needed to check in to this event. Allow it in your phone\'s settings.'**
  String get locationDenied;

  /// No description provided for @notACheckInCode.
  ///
  /// In en, this message translates to:
  /// **'This isn\'t an event check-in QR code.'**
  String get notACheckInCode;

  /// No description provided for @scanAgain.
  ///
  /// In en, this message translates to:
  /// **'Scan again'**
  String get scanAgain;

  /// No description provided for @done.
  ///
  /// In en, this message translates to:
  /// **'Done'**
  String get done;

  /// No description provided for @grade.
  ///
  /// In en, this message translates to:
  /// **'Grade'**
  String get grade;

  /// No description provided for @points.
  ///
  /// In en, this message translates to:
  /// **'Points'**
  String get points;

  /// No description provided for @unranked.
  ///
  /// In en, this message translates to:
  /// **'Unranked'**
  String get unranked;

  /// No description provided for @roles.
  ///
  /// In en, this message translates to:
  /// **'Roles'**
  String get roles;

  /// No description provided for @roleAdmin.
  ///
  /// In en, this message translates to:
  /// **'Admin'**
  String get roleAdmin;

  /// No description provided for @roleCoordinator.
  ///
  /// In en, this message translates to:
  /// **'Coordinator'**
  String get roleCoordinator;

  /// No description provided for @roleVolunteer.
  ///
  /// In en, this message translates to:
  /// **'Volunteer'**
  String get roleVolunteer;

  /// No description provided for @cameraUnavailable.
  ///
  /// In en, this message translates to:
  /// **'The camera isn\'t available. Allow camera access in your phone\'s settings.'**
  String get cameraUnavailable;

  /// No description provided for @forgotPassword.
  ///
  /// In en, this message translates to:
  /// **'Forgot your password?'**
  String get forgotPassword;

  /// No description provided for @forgotPasswordTitle.
  ///
  /// In en, this message translates to:
  /// **'Reset password'**
  String get forgotPasswordTitle;

  /// No description provided for @forgotPasswordIntro.
  ///
  /// In en, this message translates to:
  /// **'Enter the email address of your account. We\'ll email you a link to choose a new password on the website.'**
  String get forgotPasswordIntro;

  /// No description provided for @email.
  ///
  /// In en, this message translates to:
  /// **'Email'**
  String get email;

  /// No description provided for @invalidEmail.
  ///
  /// In en, this message translates to:
  /// **'Enter a valid email address'**
  String get invalidEmail;

  /// No description provided for @sendResetLink.
  ///
  /// In en, this message translates to:
  /// **'Send reset link'**
  String get sendResetLink;

  /// No description provided for @resetLinkSent.
  ///
  /// In en, this message translates to:
  /// **'If an active account uses that email, a link to reset its password is on its way. Check your inbox and spam folder; the link works only once.'**
  String get resetLinkSent;

  /// No description provided for @resetUnavailable.
  ///
  /// In en, this message translates to:
  /// **'This server can\'t send email. Ask an administrator to set a new password for you.'**
  String get resetUnavailable;

  /// No description provided for @backToLogin.
  ///
  /// In en, this message translates to:
  /// **'Back to login'**
  String get backToLogin;

  /// No description provided for @changePassword.
  ///
  /// In en, this message translates to:
  /// **'Change password'**
  String get changePassword;

  /// No description provided for @currentPassword.
  ///
  /// In en, this message translates to:
  /// **'Current password'**
  String get currentPassword;

  /// No description provided for @newPassword.
  ///
  /// In en, this message translates to:
  /// **'New password'**
  String get newPassword;

  /// No description provided for @confirmNewPassword.
  ///
  /// In en, this message translates to:
  /// **'Confirm new password'**
  String get confirmNewPassword;

  /// No description provided for @changePasswordHelp.
  ///
  /// In en, this message translates to:
  /// **'At least 8 characters. You\'ll stay logged in here, and be logged out of the website and your other devices.'**
  String get changePasswordHelp;

  /// No description provided for @passwordTooShort.
  ///
  /// In en, this message translates to:
  /// **'Password must be at least 8 characters'**
  String get passwordTooShort;

  /// No description provided for @passwordsDontMatch.
  ///
  /// In en, this message translates to:
  /// **'Passwords do not match'**
  String get passwordsDontMatch;

  /// No description provided for @wrongCurrentPassword.
  ///
  /// In en, this message translates to:
  /// **'Your current password is wrong.'**
  String get wrongCurrentPassword;

  /// No description provided for @passwordChanged.
  ///
  /// In en, this message translates to:
  /// **'Password changed. You\'ve been logged out of the website and your other devices.'**
  String get passwordChanged;

  /// No description provided for @officeFilter.
  ///
  /// In en, this message translates to:
  /// **'Office'**
  String get officeFilter;

  /// No description provided for @allOffices.
  ///
  /// In en, this message translates to:
  /// **'All offices'**
  String get allOffices;

  /// No description provided for @noOffice.
  ///
  /// In en, this message translates to:
  /// **'No office'**
  String get noOffice;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['ar', 'en'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'ar':
      return AppLocalizationsAr();
    case 'en':
      return AppLocalizationsEn();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
