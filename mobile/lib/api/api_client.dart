import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import 'models.dart';

/// Any failure talking to the API.
class ApiException implements Exception {
  const ApiException(this.statusCode, this.error, [this.message]);

  /// 0 when the server couldn't be reached at all.
  final int statusCode;
  final String error;
  final String? message;

  bool get isUnreachable => statusCode == 0;

  @override
  String toString() => 'ApiException($statusCode, $error, $message)';
}

/// The token is missing, expired or revoked: the user has to log in again.
class UnauthorizedException extends ApiException {
  const UnauthorizedException([String? message]) : super(401, 'unauthorized', message);
}

/// Client for the Volunteer Portal mobile API. Sends the bearer token and the app's language.
class ApiClient {
  ApiClient({required String baseUrl, http.Client? httpClient, this.timeout = const Duration(seconds: 20)})
      : baseUrl = normalizeBaseUrl(baseUrl),
        _http = httpClient ?? http.Client();

  final String baseUrl;
  final Duration timeout;
  final http.Client _http;

  String? token;

  /// Language code sent as Accept-Language, so server messages come back in it ("en" or "ar").
  String language = 'en';

  /// "portal.example.org/" -> "https://portal.example.org"; keeps http:// for local testing.
  static String normalizeBaseUrl(String url) {
    var value = url.trim();
    if (!value.startsWith('http://') && !value.startsWith('https://')) {
      value = 'https://$value';
    }
    while (value.endsWith('/')) {
      value = value.substring(0, value.length - 1);
    }
    return value;
  }

  Future<LoginResult> login(String username, String password, {String? deviceName}) async {
    try {
      final json = await _send('POST', '/api/auth/login',
          body: {'username': username, 'password': password, 'deviceName': deviceName}, authenticated: false);
      return LoginResult.fromJson(json as Map<String, dynamic>);
    } on UnauthorizedException {
      throw const ApiException(401, 'invalid_credentials');
    }
  }

  Future<void> logout() => _send('POST', '/api/auth/logout');

  Future<Me> me() async => Me.fromJson(await _send('GET', '/api/me') as Map<String, dynamic>);

  Future<List<EventItem>> events() async =>
      _list(await _send('GET', '/api/events')).map(EventItem.fromJson).toList();

  Future<CheckInResult> checkIn(String qr, {double? latitude, double? longitude}) async {
    final json = await _send('POST', '/api/checkin', body: {
      'qr': qr,
      'latitude': ?latitude,
      'longitude': ?longitude,
    });
    return CheckInResult.fromJson(json as Map<String, dynamic>);
  }

  Future<List<AttendanceItem>> attendance() async =>
      _list(await _send('GET', '/api/attendance')).map(AttendanceItem.fromJson).toList();

  Future<List<NotificationItem>> notifications() async =>
      _list(await _send('GET', '/api/notifications')).map(NotificationItem.fromJson).toList();

  Future<void> markNotificationRead(int id) => _send('POST', '/api/notifications/$id/read');

  Future<void> markAllNotificationsRead() => _send('POST', '/api/notifications/read-all');

  List<Map<String, dynamic>> _list(Object? json) => (json as List).cast<Map<String, dynamic>>();

  Future<Object?> _send(String method, String path, {Map<String, Object?>? body, bool authenticated = true}) async {
    final request = http.Request(method, Uri.parse('$baseUrl$path'))
      ..headers['Accept'] = 'application/json'
      ..headers['Accept-Language'] = language;
    if (authenticated && token != null) {
      request.headers['Authorization'] = 'Bearer $token';
    }
    if (body != null) {
      request.headers['Content-Type'] = 'application/json; charset=utf-8';
      request.body = jsonEncode(body);
    }

    final http.Response response;
    try {
      response = await http.Response.fromStream(await _http.send(request).timeout(timeout));
    } on TimeoutException {
      throw const ApiException(0, 'timeout');
    } on Exception catch (e) {
      throw ApiException(0, 'unreachable', e.toString());
    }

    final text = utf8.decode(response.bodyBytes);
    final Object? json = text.isEmpty ? null : _tryDecode(text);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return json;
    }
    final error = json is Map ? json['error'] as String? : null;
    final message = json is Map ? json['message'] as String? : null;
    if (response.statusCode == 401) {
      throw UnauthorizedException(message);
    }
    throw ApiException(response.statusCode, error ?? 'http_${response.statusCode}', message);
  }

  static Object? _tryDecode(String text) {
    try {
      return jsonDecode(text);
    } on FormatException {
      return null;
    }
  }
}
