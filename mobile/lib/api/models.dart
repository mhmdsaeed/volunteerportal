// JSON shapes of the Volunteer Portal mobile API (see "Mobile app API" in the server's README.md).

DateTime? _date(Object? value) => value is String ? DateTime.tryParse(value) : null;

class Me {
  const Me({
    required this.id,
    required this.username,
    required this.email,
    required this.roles,
    required this.grade,
    required this.points,
  });

  final int id;
  final String username;
  final String? email;
  final List<String> roles;
  final String? grade;
  final int points;

  factory Me.fromJson(Map<String, dynamic> json) => Me(
        id: json['id'] as int,
        username: json['username'] as String,
        email: json['email'] as String?,
        roles: (json['roles'] as List? ?? const []).cast<String>(),
        grade: json['grade'] as String?,
        points: (json['points'] as num? ?? 0).toInt(),
      );
}

class LoginResult {
  const LoginResult({required this.token, required this.expiresAt, required this.user});

  final String token;
  final DateTime? expiresAt;
  final Me user;

  factory LoginResult.fromJson(Map<String, dynamic> json) => LoginResult(
        token: json['token'] as String,
        expiresAt: _date(json['expiresAt']),
        user: Me.fromJson(json['user'] as Map<String, dynamic>),
      );
}

/// My membership in an initiative: never asked, asked and waiting, approved or turned down.
enum Membership {
  none,
  pending,
  approved,
  rejected;

  static Membership parse(String? value) => switch (value) {
        'PENDING' => pending,
        'APPROVED' => approved,
        'REJECTED' => rejected,
        _ => none,
      };
}

class InitiativeItem {
  const InitiativeItem({
    required this.id,
    required this.name,
    required this.description,
    required this.office,
    required this.membership,
  });

  final int id;
  final String name;
  final String? description;
  final String? office;
  final Membership membership;

  factory InitiativeItem.fromJson(Map<String, dynamic> json) => InitiativeItem(
        id: json['id'] as int,
        name: json['name'] as String? ?? '',
        description: json['description'] as String?,
        office: json['office'] as String?,
        membership: Membership.parse(json['membership'] as String?),
      );
}

/// How a join question is answered: yes or no, one of its choices, any of its choices, or in words.
enum QuestionType {
  yesNo,
  oneChoice,
  manyChoices,
  text;

  static QuestionType parse(String? value) => switch (value) {
        'YES_NO' => yesNo,
        'ONE_CHOICE' => oneChoice,
        'MANY_CHOICES' => manyChoices,
        _ => text,
      };
}

class Question {
  const Question({required this.id, required this.text, required this.type, required this.choices});

  final int id;
  final String text;
  final QuestionType type;

  /// Numbered from 1 in the answers: choice `n` is `choices[n - 1]`.
  final List<String> choices;

  factory Question.fromJson(Map<String, dynamic> json) => Question(
        id: json['id'] as int,
        text: json['text'] as String? ?? '',
        type: QuestionType.parse(json['type'] as String?),
        choices: (json['choices'] as List? ?? const []).cast<String>(),
      );
}

/// An initiative with my membership and the questions to answer when asking to join.
class InitiativeDetail {
  const InitiativeDetail({
    required this.id,
    required this.name,
    required this.description,
    required this.office,
    required this.membership,
    required this.questions,
  });

  final int id;
  final String name;
  final String? description;
  final String? office;
  final Membership membership;
  final List<Question> questions;

  factory InitiativeDetail.fromJson(Map<String, dynamic> json) => InitiativeDetail(
        id: json['id'] as int,
        name: json['name'] as String? ?? '',
        description: json['description'] as String?,
        office: json['office'] as String?,
        membership: Membership.parse(json['membership'] as String?),
        questions: (json['questions'] as List? ?? const [])
            .cast<Map<String, dynamic>>()
            .map(Question.fromJson)
            .toList(),
      );
}

/// My check-in status for an event.
enum EventStatus { notCheckedIn, checkedIn, checkedOut }

class EventItem {
  const EventItem({
    required this.id,
    required this.name,
    required this.initiative,
    required this.from,
    required this.to,
    required this.locationUrl,
    required this.requiresLocation,
    required this.status,
  });

  final int id;
  final String name;
  final String? initiative;
  final DateTime? from;
  final DateTime? to;
  final String? locationUrl;
  final bool requiresLocation;
  final EventStatus status;

  factory EventItem.fromJson(Map<String, dynamic> json) => EventItem(
        id: json['id'] as int,
        name: json['name'] as String? ?? '',
        initiative: json['initiative'] as String?,
        from: _date(json['from']),
        to: _date(json['to']),
        locationUrl: json['locationUrl'] as String?,
        requiresLocation: json['requiresLocation'] as bool? ?? false,
        status: switch (json['myStatus']) {
          'CHECKED_IN' => EventStatus.checkedIn,
          'CHECKED_OUT' => EventStatus.checkedOut,
          _ => EventStatus.notCheckedIn,
        },
      );
}

/// Outcome of scanning a check-in QR code.
enum CheckInOutcome {
  checkedIn,
  checkedOut,
  alreadyDone,
  invalidCode,
  notMember,
  eventClosed,
  locationRequired,
  tooFar,
  unknown;

  static CheckInOutcome parse(String? value) => switch (value) {
        'CHECKED_IN' => checkedIn,
        'CHECKED_OUT' => checkedOut,
        'ALREADY_DONE' => alreadyDone,
        'INVALID_CODE' => invalidCode,
        'NOT_MEMBER' => notMember,
        'EVENT_CLOSED' => eventClosed,
        'LOCATION_REQUIRED' => locationRequired,
        'TOO_FAR' => tooFar,
        _ => unknown,
      };
}

class CheckInResult {
  const CheckInResult({
    required this.outcome,
    required this.success,
    required this.eventName,
    required this.message,
  });

  final CheckInOutcome outcome;
  final bool success;
  final String? eventName;

  /// Already in the phone's language (the server localizes it from Accept-Language).
  final String message;

  factory CheckInResult.fromJson(Map<String, dynamic> json) => CheckInResult(
        outcome: CheckInOutcome.parse(json['result'] as String?),
        success: json['success'] as bool? ?? false,
        eventName: json['event'] as String?,
        message: json['message'] as String? ?? '',
      );
}

class AttendanceItem {
  const AttendanceItem({
    required this.id,
    required this.event,
    required this.initiative,
    required this.isCheckOut,
    required this.time,
  });

  final int id;
  final String event;
  final String? initiative;
  final bool isCheckOut;
  final DateTime? time;

  factory AttendanceItem.fromJson(Map<String, dynamic> json) => AttendanceItem(
        id: json['id'] as int,
        event: json['event'] as String? ?? '',
        initiative: json['initiative'] as String?,
        isCheckOut: json['type'] == 'CHECK_OUT',
        time: _date(json['time']),
      );
}

class NotificationItem {
  const NotificationItem({required this.id, required this.message, required this.read, required this.createdAt});

  final int id;
  final String message;
  final bool read;
  final DateTime? createdAt;

  factory NotificationItem.fromJson(Map<String, dynamic> json) => NotificationItem(
        id: json['id'] as int,
        message: json['message'] as String? ?? '',
        read: json['read'] as bool? ?? false,
        createdAt: _date(json['createdAt']),
      );
}
