import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

/// A stand-in for the Volunteer Portal API that answers like the real one, for tests.
class FakeServer {
  FakeServer({this.password = 'demo12345', this.eventNeedsLocation = false});

  final String password;
  final bool eventNeedsLocation;
  static const token = 'vp_test-token';

  final List<http.Request> requests = [];
  bool tokenRevoked = false;
  String myStatus = 'NOT_CHECKED_IN';
  final List<Map<String, Object?>> attendance = [];
  bool notificationRead = false;

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
    if (request.headers['Authorization'] != 'Bearer $token' || tokenRevoked) {
      return _error(401, 'unauthorized');
    }
    switch (path) {
      case '/api/auth/logout':
        tokenRevoked = true;
        return http.Response('', 204);
      case '/api/me':
        return _json(_me);
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

  static const Map<String, Object?> _me = {
    'id': 648,
    'username': 'demo_volunteer',
    'email': 'demo_volunteer@demo.volunteerportal.local',
    'roles': ['VOLUNTEER'],
    'grade': null,
    'points': 0,
  };

  static const validQr = 'https://192.168.100.11:8443/checkin/112?code=59672954-abc';
}
