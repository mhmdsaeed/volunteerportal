import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../api/models.dart';
import '../app_scope.dart';
import '../check_in_flow.dart';
import '../l10n/app_localizations.dart';

/// Builds the camera view; calls [onCode] with the text of each QR code it sees.
typedef ScannerBuilder = Widget Function(BuildContext context, void Function(String code) onCode);

Widget cameraScanner(BuildContext context, void Function(String code) onCode) => MobileScanner(
      onDetect: (capture) {
        final value = capture.barcodes.map((b) => b.rawValue).whereType<String>().firstOrNull;
        if (value != null) {
          onCode(value);
        }
      },
      errorBuilder: (context, error) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(AppLocalizations.of(context).cameraUnavailable, textAlign: TextAlign.center),
        ),
      ),
    );

/// Scan the event's QR code to check in (or out, if already checked in).
class ScanScreen extends StatefulWidget {
  const ScanScreen({super.key, this.scannerBuilder = cameraScanner, this.onCheckedIn});

  final ScannerBuilder scannerBuilder;

  /// Called after a successful check-in or check-out, e.g. to refresh the events list.
  final VoidCallback? onCheckedIn;

  @override
  State<ScanScreen> createState() => _ScanScreenState();
}

enum _Phase { scanning, working, locating, done }

class _ScanScreenState extends State<ScanScreen> {
  _Phase _phase = _Phase.scanning;
  ScanOutcome? _outcome;
  String? _error;

  Future<void> _onCode(String code) async {
    if (_phase != _Phase.scanning) {
      return; // the camera reports the same code many times a second
    }
    setState(() => _phase = _Phase.working);
    final state = AppScope.read(context);
    try {
      final outcome = await state.guard(() => CheckInFlow(state.api, state.location)
          .run(code, onLocating: () => mounted ? setState(() => _phase = _Phase.locating) : null));
      if (outcome is ServerAnswer && outcome.result.success) {
        widget.onCheckedIn?.call();
      }
      if (mounted) {
        setState(() {
          _outcome = outcome;
          _phase = _Phase.done;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = describeError(context, e);
          _phase = _Phase.done;
        });
      }
    }
  }

  void _scanAgain() => setState(() {
        _phase = _Phase.scanning;
        _outcome = null;
        _error = null;
      });

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return switch (_phase) {
      _Phase.scanning => Column(
          children: [
            Expanded(child: widget.scannerBuilder(context, _onCode)),
            Padding(padding: const EdgeInsets.all(16), child: Text(t.scanHint, textAlign: TextAlign.center)),
          ],
        ),
      _Phase.working || _Phase.locating => Center(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const CircularProgressIndicator(),
            const SizedBox(height: 16),
            Text(_phase == _Phase.locating ? t.gettingLocation : t.checkingIn),
          ]),
        ),
      _Phase.done => _ResultView(outcome: _outcome, error: _error, onScanAgain: _scanAgain),
    };
  }
}

class _ResultView extends StatelessWidget {
  const _ResultView({required this.outcome, required this.error, required this.onScanAgain});

  final ScanOutcome? outcome;
  final String? error;
  final VoidCallback onScanAgain;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final outcome = this.outcome;
    final (bool success, String? title, String message) = switch (outcome) {
      ServerAnswer(:final result) => (result.success, result.eventName, result.message),
      LocationUnavailable() => (false, null, t.locationDenied),
      NotACheckInCode() => (false, null, t.notACheckInCode),
      null => (false, null, error ?? t.somethingWentWrong),
    };
    final checkedOut = outcome is ServerAnswer && outcome.result.outcome == CheckInOutcome.checkedOut;

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              success ? (checkedOut ? Icons.logout : Icons.check_circle) : Icons.error_outline,
              key: Key(success ? 'scanSuccess' : 'scanProblem'),
              size: 88,
              color: success ? Colors.green : Theme.of(context).colorScheme.error,
            ),
            if (title != null) ...[
              const SizedBox(height: 16),
              Text(title, style: Theme.of(context).textTheme.titleLarge, textAlign: TextAlign.center),
            ],
            const SizedBox(height: 12),
            Text(message, key: const Key('scanMessage'), style: Theme.of(context).textTheme.bodyLarge, textAlign: TextAlign.center),
            const SizedBox(height: 24),
            FilledButton.tonalIcon(
              key: const Key('scanAgain'),
              onPressed: onScanAgain,
              icon: const Icon(Icons.qr_code_scanner),
              label: Text(t.scanAgain),
            ),
          ],
        ),
      ),
    );
  }
}
