// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Arabic (`ar`).
class AppLocalizationsAr extends AppLocalizations {
  AppLocalizationsAr([String locale = 'ar']) : super(locale);

  @override
  String get appTitle => 'بوابة المتطوعين';

  @override
  String get serverUrl => 'عنوان الخادم';

  @override
  String get serverUrlHint => 'https://portal.example.org';

  @override
  String get username => 'اسم المستخدم';

  @override
  String get password => 'كلمة المرور';

  @override
  String get login => 'تسجيل الدخول';

  @override
  String get loggingIn => 'جارٍ تسجيل الدخول...';

  @override
  String get logout => 'تسجيل الخروج';

  @override
  String get wrongCredentials => 'اسم المستخدم أو كلمة المرور غير صحيحة.';

  @override
  String get cannotReachServer =>
      'تعذّر الاتصال بالخادم. تحقق من العنوان ومن اتصالك.';

  @override
  String get sessionExpired => 'انتهت جلستك. يرجى تسجيل الدخول مرة أخرى.';

  @override
  String get somethingWentWrong => 'حدث خطأ ما. يرجى المحاولة مرة أخرى.';

  @override
  String get required => 'مطلوب';

  @override
  String get invalidServerUrl => 'أدخل عنواناً يبدأ بـ http:// أو https://';

  @override
  String get tabEvents => 'الفعاليات';

  @override
  String get tabScan => 'تسجيل الحضور';

  @override
  String get tabHistory => 'السجل';

  @override
  String get tabNotifications => 'الإشعارات';

  @override
  String get profile => 'الملف الشخصي';

  @override
  String get language => 'اللغة';

  @override
  String get retry => 'إعادة المحاولة';

  @override
  String get noEvents =>
      'لا توجد فعاليات قادمة. عند قبول طلب انضمامك إلى مبادرة، تظهر فعالياتها هنا.';

  @override
  String get noHistory => 'لم تسجّل حضورك في أي فعالية بعد.';

  @override
  String get noNotifications => 'لا توجد إشعارات بعد.';

  @override
  String get markAllRead => 'تعليم الكل كمقروء';

  @override
  String get statusNotCheckedIn => 'لم يُسجَّل الحضور';

  @override
  String get statusCheckedIn => 'تم تسجيل الحضور';

  @override
  String get statusCheckedOut => 'تم تسجيل الانصراف';

  @override
  String get locationChecked => 'يتم التحقق من الموقع';

  @override
  String get checkIn => 'تسجيل حضور';

  @override
  String get checkOut => 'تسجيل انصراف';

  @override
  String get scanHint =>
      'وجّه الكاميرا نحو رمز QR لتسجيل الحضور الخاص بالفعالية.';

  @override
  String get checkingIn => 'جارٍ تسجيل الحضور...';

  @override
  String get gettingLocation => 'جارٍ تحديد موقعك...';

  @override
  String get locationDenied =>
      'يلزم الوصول إلى الموقع لتسجيل الحضور في هذه الفعالية. اسمح بذلك من إعدادات هاتفك.';

  @override
  String get notACheckInCode => 'هذا ليس رمز QR لتسجيل الحضور في فعالية.';

  @override
  String get scanAgain => 'مسح مرة أخرى';

  @override
  String get done => 'تم';

  @override
  String get grade => 'الدرجة';

  @override
  String get points => 'النقاط';

  @override
  String get unranked => 'بدون درجة';

  @override
  String get roles => 'الأدوار';

  @override
  String get cameraUnavailable =>
      'الكاميرا غير متاحة. اسمح بالوصول إلى الكاميرا من إعدادات هاتفك.';
}
