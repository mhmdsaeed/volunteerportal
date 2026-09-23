import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:volunteer_portal_app/api/api_client.dart';
import 'package:volunteer_portal_app/api/models.dart';
import 'package:volunteer_portal_app/check_in_flow.dart';
import 'package:volunteer_portal_app/services/location_service.dart';

import 'fake_server.dart';

class FakeLocation implements LocationService {
  FakeLocation(this.position);

  final Coordinates? position;
  int calls = 0;

  @override
  Future<Coordinates?> current() async {
    calls++;
    return position;
  }
}

void main() {
  ApiClient apiFor(FakeServer server) =>
      ApiClient(baseUrl: 'https://portal.example.org', httpClient: server.client)..token = FakeServer.token;

  test('event without a location check: one call, location never asked for', () async {
    final server = FakeServer();
    final location = FakeLocation(const Coordinates(24.7, 46.6));

    final outcome = await CheckInFlow(apiFor(server), location).run(FakeServer.validQr);

    expect(outcome, isA<ServerAnswer>().having((a) => a.result.outcome, 'outcome', CheckInOutcome.checkedIn));
    expect(location.calls, 0);
    expect(server.requests, hasLength(1));
  });

  test('event with a location check: asks for location, then retries with it', () async {
    final server = FakeServer(eventNeedsLocation: true);
    final location = FakeLocation(const Coordinates(24.7, 46.6));
    var locating = false;

    final outcome = await CheckInFlow(apiFor(server), location).run(FakeServer.validQr, onLocating: () => locating = true);

    expect(outcome, isA<ServerAnswer>().having((a) => a.result.success, 'success', isTrue));
    expect(locating, isTrue);
    expect(location.calls, 1);
    expect(jsonDecode(server.requests.last.body)['latitude'], 24.7);
  });

  test('location refused: says so instead of calling again', () async {
    final server = FakeServer(eventNeedsLocation: true);

    final outcome = await CheckInFlow(apiFor(server), FakeLocation(null)).run(FakeServer.validQr);

    expect(outcome, isA<LocationUnavailable>());
    expect(server.requests, hasLength(1));
  });

  test('a QR code that is not a check-in code', () async {
    final outcome = await CheckInFlow(apiFor(FakeServer()), FakeLocation(null)).run('https://example.com/menu');

    expect(outcome, isA<NotACheckInCode>());
  });

  test('second scan checks out, third is already done', () async {
    final server = FakeServer();
    final flow = CheckInFlow(apiFor(server), FakeLocation(null));

    await flow.run(FakeServer.validQr);
    final second = await flow.run(FakeServer.validQr) as ServerAnswer;
    final third = await flow.run(FakeServer.validQr) as ServerAnswer;

    expect(second.result.outcome, CheckInOutcome.checkedOut);
    expect(third.result.outcome, CheckInOutcome.alreadyDone);
    expect(third.result.success, isFalse);
  });
}
