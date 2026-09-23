import 'api/api_client.dart';
import 'api/models.dart';
import 'services/location_service.dart';

/// What the scan screen shows after a scan.
sealed class ScanOutcome {
  const ScanOutcome();
}

/// The server's answer (checked in/out, or why not), with its message in the app's language.
class ServerAnswer extends ScanOutcome {
  const ServerAnswer(this.result);

  final CheckInResult result;
}

/// The event checks location, but the phone couldn't give it (off or permission refused).
class LocationUnavailable extends ScanOutcome {
  const LocationUnavailable();
}

/// The scanned QR code isn't an event check-in code.
class NotACheckInCode extends ScanOutcome {
  const NotACheckInCode();
}

/// Checks in with a scanned QR code. Location is only asked for when the server says the event
/// needs it, so volunteers aren't prompted for it at events that don't check it.
class CheckInFlow {
  const CheckInFlow(this.api, this.location);

  final ApiClient api;
  final LocationService location;

  Future<ScanOutcome> run(String qr, {void Function()? onLocating}) async {
    try {
      final first = await api.checkIn(qr);
      if (first.outcome != CheckInOutcome.locationRequired) {
        return ServerAnswer(first);
      }
      onLocating?.call();
      final here = await location.current();
      if (here == null) {
        return const LocationUnavailable();
      }
      return ServerAnswer(await api.checkIn(qr, latitude: here.latitude, longitude: here.longitude));
    } on ApiException catch (e) {
      if (e.statusCode == 400 && e.error == 'invalid_qr') {
        return const NotACheckInCode();
      }
      rethrow;
    }
  }
}
