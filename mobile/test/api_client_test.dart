import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:volunteer_portal_app/api/api_client.dart';
import 'package:volunteer_portal_app/api/models.dart';

import 'fake_server.dart';

void main() {
  group('normalizeBaseUrl', () {
    test('adds https:// and removes trailing slashes, keeps http:// for local testing', () {
      expect(ApiClient.normalizeBaseUrl('portal.example.org/'), 'https://portal.example.org');
      expect(ApiClient.normalizeBaseUrl(' http://192.168.100.11:8080// '), 'http://192.168.100.11:8080');
      expect(ApiClient.normalizeBaseUrl('https://x.org'), 'https://x.org');
    });
  });

  group('ApiClient', () {
    late FakeServer server;
    late ApiClient api;

    setUp(() {
      server = FakeServer();
      api = ApiClient(baseUrl: 'https://portal.example.org', httpClient: server.client);
    });

    test('login sends JSON credentials and returns the token and user', () async {
      final result = await api.login('demo_volunteer', 'demo12345', deviceName: 'Test');

      expect(result.token, FakeServer.token);
      expect(result.user.username, 'demo_volunteer');
      expect(result.user.roles, ['VOLUNTEER']);
      final request = server.requests.single;
      expect(request.method, 'POST');
      expect(request.url.toString(), 'https://portal.example.org/api/auth/login');
      expect(jsonDecode(request.body), {'username': 'demo_volunteer', 'password': 'demo12345', 'deviceName': 'Test'});
      expect(request.headers['Authorization'], isNull);
    });

    test('wrong password is invalid_credentials, not a logged-out session', () async {
      await expectLater(
        api.login('demo_volunteer', 'nope'),
        throwsA(isA<ApiException>()
            .having((e) => e.error, 'error', 'invalid_credentials')
            .having((e) => e, 'type', isNot(isA<UnauthorizedException>()))),
      );
    });

    test('calls send the bearer token and the language', () async {
      api
        ..token = FakeServer.token
        ..language = 'ar';

      await api.me();

      expect(server.requests.single.headers['Authorization'], 'Bearer ${FakeServer.token}');
      expect(server.requests.single.headers['Accept-Language'], 'ar');
    });

    test('a rejected token throws UnauthorizedException', () async {
      api.token = 'vp_wrong';

      await expectLater(api.events(), throwsA(isA<UnauthorizedException>()));
    });

    test('events are parsed, including my status', () async {
      api.token = FakeServer.token;
      server.myStatus = 'CHECKED_IN';

      final events = await api.events();

      expect(events.single.name, 'Demo Event');
      expect(events.single.initiative, 'Demo Initiative');
      expect(events.single.status, EventStatus.checkedIn);
      expect(events.single.from, DateTime(2026, 9, 23, 8));
    });

    test('check-in sends the scanned text, and location only when given', () async {
      api.token = FakeServer.token;

      final result = await api.checkIn(FakeServer.validQr);
      expect(result.outcome, CheckInOutcome.checkedIn);
      expect(result.success, isTrue);
      expect(jsonDecode(server.requests.last.body), {'qr': FakeServer.validQr});

      await api.checkIn(FakeServer.validQr, latitude: 24.7, longitude: 46.6);
      expect(jsonDecode(server.requests.last.body), {'qr': FakeServer.validQr, 'latitude': 24.7, 'longitude': 46.6});
    });

    test('an unreachable server is reported as such', () async {
      final offline = ApiClient(
        baseUrl: 'https://portal.example.org',
        httpClient: MockClient((_) => throw http.ClientException('Connection refused')),
      );

      await expectLater(offline.me(), throwsA(isA<ApiException>().having((e) => e.isUnreachable, 'unreachable', isTrue)));
    });

    test('unknown check-in results are tolerated', () {
      expect(CheckInOutcome.parse('SOMETHING_NEW'), CheckInOutcome.unknown);
    });
  });
}
