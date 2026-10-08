import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

/// A stand-in for the Volunteer Portal API that answers like the real one, for tests.
class FakeServer {
  FakeServer({this.password = 'demo12345', this.eventNeedsLocation = false});

  /// Changes when the app changes it (POST /api/auth/password).
  String password;
  final bool eventNeedsLocation;
  static const token = 'vp_test-token';

  final List<http.Request> requests = [];
  bool tokenRevoked = false;
  String myStatus = 'NOT_CHECKED_IN';
  final List<Map<String, Object?>> attendance = [];
  bool notificationRead = false;

  /// Open initiatives with my membership in each: one of every kind, as GET /api/initiatives returns them.
  /// Offices: Coast Office (8) has the food bank, Demo Office (7) the demo initiative and library; the beach has none.
  List<Map<String, Object?>> initiatives = [
    {'id': 487, 'name': 'Demo Initiative', 'description': 'Created by the dev profile.', 'office': 'Demo Office', 'officeId': 7, 'membership': 'APPROVED'},
    {'id': 488, 'name': 'Beach Clean-up', 'description': null, 'office': null, 'membership': 'PENDING'},
    {'id': 489, 'name': 'Food Bank', 'description': 'Sorting donations.', 'office': 'Coast Office', 'officeId': 8, 'membership': 'REJECTED'},
    {'id': 490, 'name': 'Library Reading', 'description': 'Reading to children.', 'office': 'Demo Office', 'officeId': 7, 'membership': 'NONE'},
  ];

  /// Join questions by initiative id, as GET /api/initiatives/{id} returns them.
  Map<int, List<Map<String, Object?>>> questions = {
    490: [
      {'id': 11, 'text': 'Are you over 18?', 'type': 'YES_NO', 'choices': <String>[]},
      {'id': 12, 'text': 'When can you come?', 'type': 'ONE_CHOICE', 'choices': ['Morning', 'Evening']},
      {'id': 13, 'text': 'What can you help with?', 'type': 'MANY_CHOICES', 'choices': ['Reading', 'Games', 'Tidying']},
      {'id': 14, 'text': 'Anything else?', 'type': 'TEXT', 'choices': <String>[]},
    ],
  };

  /// Whether the server can email reset links (POST /api/auth/forgot-password), and who asked for one.
  bool resetAvailable = true;
  String? resetEmail;

  /// The answers of the last join request, as sent.
  Map<String, dynamic>? joinAnswers;

  late final http.Client client = MockClient((request) async {
    requests.add(request);
    return _handle(request);
  });

  http.Response _json(Object body, [int status = 200]) =>
      http.Response.bytes(utf8.encode(jsonEncode(body)), status, headers: {'content-type': 'application/json'});

  http.Response _error(int status, String error) => _json({'error': error, 'message': error}, status);

  bool get _arabic => requests.last.headers['Accept-Language'] == 'ar';

  Future<http.Response> _handle(http.Request request) async {
    final path = request.url.path;
    if (path == '/api/auth/login') {
      final body = jsonDecode(request.body) as Map<String, dynamic>;
      if (body['username'] == 'demo_volunteer' && body['password'] == password) {
        tokenRevoked = false;
        return _json({'token': token, 'expiresAt': '2026-10-23T10:00:00', 'user': _me});
      }
      return _error(401, 'invalid_credentials');
    }
    if (path == '/api/auth/forgot-password') {
      if (!resetAvailable) {
        return _error(409, 'reset_unavailable');
      }
      resetEmail = (jsonDecode(request.body) as Map<String, dynamic>)['email'] as String?;
      return http.Response('', 202);
    }
    if (request.headers['Authorization'] != 'Bearer $token' || tokenRevoked) {
      return _error(401, 'unauthorized');
    }
    final initiative = RegExp(r'^/api/initiatives/(\d+)(/join|/withdraw)?$').firstMatch(path);
    if (initiative != null) {
      return _initiative(int.parse(initiative.group(1)!), initiative.group(2), request);
    }
    switch (path) {
      case '/api/auth/logout':
        tokenRevoked = true;
        return http.Response('', 204);
      case '/api/auth/password':
        final body = jsonDecode(request.body) as Map<String, dynamic>;
        if (body['currentPassword'] != password) {
          return _error(400, 'wrong_password');
        }
        if ((body['newPassword'] as String).length < 8) {
          return _error(400, 'password_too_short');
        }
        password = body['newPassword'] as String;
        return http.Response('', 204);
      case '/api/me':
        return _json(_me);
      case '/api/initiatives':
        return _json(initiatives);
      case '/api/events':
        return _json([
          {
            'id': 112,
            'name': 'Demo Event',
            'initiativeId': 487,
            'initiative': 'Demo Initiative',
            'from': '2026-09-23T08:00:00',
            'to': '2026-09-23T22:00:00',
            'locationUrl': null,
            'latitude': null,
            'longitude': null,
            'requiresLocation': eventNeedsLocation,
            'myStatus': myStatus,
          }
        ]);
      case '/api/checkin':
        return _checkIn(jsonDecode(request.body) as Map<String, dynamic>);
      case '/api/attendance':
        return _json(attendance);
      case '/api/notifications':
        return _json([
          {'id': 5, 'message': _arabic ? 'تم قبول طلب انضمامك' : 'Your request was approved.', 'link': null,
            'read': notificationRead, 'createdAt': '2026-09-23T09:00:00'}
        ]);
      case '/api/notifications/5/read':
      case '/api/notifications/read-all':
        notificationRead = true;
        return http.Response('', 204);
    }
    return _error(404, 'not_found');
  }

  http.Response _initiative(int id, String? action, http.Request request) {
    final item = initiatives.where((i) => i['id'] == id).firstOrNull;
    if (item == null) {
      return _error(404, 'not_found');
    }
    switch (action) {
      case '/join':
        if (item['membership'] != 'NONE') {
          return _error(409, 'already_requested');
        }
        joinAnswers = request.body.isEmpty ? {} : (jsonDecode(request.body) as Map<String, dynamic>)['answers'] as Map<String, dynamic>;
        item['membership'] = 'PENDING';
        return _json(item);
      case '/withdraw':
        if (item['membership'] != 'PENDING') {
          return _error(409, 'already_reviewed');
        }
        item['membership'] = 'NONE';
        return http.Response('', 204);
    }
    return _json({...item, 'questions': questions[id] ?? const []});
  }

  http.Response _checkIn(Map<String, dynamic> body) {
    final qr = body['qr'] as String? ?? '';
    if (!qr.contains('/checkin/112?code=')) {
      return _error(400, 'invalid_qr');
    }
    if (eventNeedsLocation && body['latitude'] == null) {
      return _result('LOCATION_REQUIRED', false, 'Please allow location access');
    }
    switch (myStatus) {
      case 'NOT_CHECKED_IN':
        myStatus = 'CHECKED_IN';
        attendance.insert(0, {'id': 1, 'eventId': 112, 'event': 'Demo Event', 'initiative': 'Demo Initiative',
          'type': 'CHECK_IN', 'time': '2026-09-23T10:00:00', 'note': 'QR self check-in'});
        return _result('CHECKED_IN', true, _arabic ? 'تم تسجيل حضورك. أهلاً بك!' : 'You are checked in. Welcome!');
      case 'CHECKED_IN':
        myStatus = 'CHECKED_OUT';
        return _result('CHECKED_OUT', true, 'You are checked out. Thank you for volunteering!');
      default:
        return _result('ALREADY_DONE', false, 'You have already checked in and out of this event.');
    }
  }

  http.Response _result(String result, bool success, String message) =>
      _json({'result': result, 'success': success, 'eventId': 112, 'event': 'Demo Event', 'message': message});

  /// My grade (null: unranked) and points, which an admin may change while the app is open.
  String? grade;
  int points = 0;

  Map<String, Object?> get _me => {
        'id': 648,
        'username': 'demo_volunteer',
        'email': 'demo_volunteer@demo.volunteerportal.local',
        'roles': ['VOLUNTEER'],
        'grade': grade,
        'points': points,
      };

  static const validQr = 'https://192.168.100.11:8443/checkin/112?code=59672954-abc';
}
