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
