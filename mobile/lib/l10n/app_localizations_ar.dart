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
  String get loginTagline =>
      'انضم إلى المبادرات، وتابع فعالياتك القادمة، وسجّل حضورك بهاتفك عند وصولك.';

  @override
  String get nextEvent => 'فعاليتك القادمة';

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
  String get tabScanShort => 'الحضور';

  @override
  String get tabHistory => 'السجل';

  @override
  String get tabNotifications => 'الإشعارات';

  @override
  String get tabNotificationsShort => 'الإشعارات';

  @override
  String get tabInitiatives => 'المبادرات';

  @override
  String get filterJoined => 'المنضم إليها';

  @override
  String get filterPending => 'قيد الانتظار';

  @override
  String get filterRejected => 'غير مقبول';

  @override
  String get filterNotJoined => 'غير منضم';

  @override
  String get filterAll => 'الكل';

  @override
  String get membershipNone => 'غير منضم';

  @override
  String get membershipPending => 'قيد الانتظار';

  @override
  String get membershipApproved => 'مقبول';

  @override
  String get membershipRejected => 'غير مقبول';

  @override
  String get noJoinedInitiatives => 'لم تنضم إلى أي مبادرة بعد.';

  @override
  String get noInitiativesHere => 'لا توجد مبادرات هنا.';

  @override
  String get noInitiativesOpen => 'لا توجد مبادرات متاحة حالياً.';

  @override
  String get showInitiativesToJoin => 'عرض المبادرات المتاحة';

  @override
  String get tapToJoin => 'اضغط على مبادرة للإجابة عن أسئلتها وطلب الانضمام.';

  @override
  String get requestToJoin => 'طلب الانضمام';

  @override
  String get joinQuestionsIntro => 'أجب عن هذه الأسئلة، ثم أرسل طلبك.';

  @override
  String get yes => 'نعم';

  @override
  String get no => 'لا';

  @override
  String get yourAnswer => 'إجابتك';

  @override
  String get requestSent => 'تم إرسال طلبك. سيراجعه أحد المنسقين.';

  @override
  String get pendingHint =>
      'طلبك قيد المراجعة. بعد قبوله، يمكنك حضور فعاليات هذه المبادرة.';

  @override
  String get withdrawRequest => 'سحب الطلب';

  @override
  String get withdrawConfirm => 'هل تريد سحب طلب انضمامك إلى هذه المبادرة؟';

  @override
  String get cancel => 'إلغاء';

  @override
  String get requestWithdrawn => 'تم سحب الطلب.';

  @override
  String get memberHint =>
      'أنت عضو في هذه المبادرة، وتجد فعالياتها في تبويب الفعاليات.';

  @override
  String get notApprovedHint => 'لم يُقبل طلب انضمامك.';

  @override
  String get alreadyRequested => 'لقد طلبت الانضمام إلى هذه المبادرة من قبل.';

  @override
  String get alreadyReviewed => 'تمت مراجعة طلبك، لذا لا يمكن سحبه.';

  @override
  String get initiativeClosed => 'هذه المبادرة لم تعد متاحة.';

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
  String get roleAdmin => 'مسؤول';

  @override
  String get roleCoordinator => 'منسق';

  @override
  String get roleVolunteer => 'متطوع';

  @override
  String get cameraUnavailable =>
      'الكاميرا غير متاحة. اسمح بالوصول إلى الكاميرا من إعدادات هاتفك.';
}
