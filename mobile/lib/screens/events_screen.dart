import 'package:flutter/material.dart';

import '../api/models.dart';
import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import 'widgets.dart';

/// Upcoming events of initiatives I'm an approved member of, with my check-in status.
/// The next one is shown as a pass, like on the website's home page.
class EventsScreen extends StatelessWidget {
  const EventsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return AsyncList<EventItem>(
      key: const PageStorageKey('events'),
      load: () => AppScope.read(context).api.events(),
      emptyText: t.noEvents,
      firstItemBuilder: (context, event, _) => _NextEventPass(event),
      itemBuilder: (context, event, _) => ListTile(
        key: Key('event-${event.id}'),
        title: Text(event.name),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (event.initiative != null) Text(event.initiative!),
            if (event.from != null)
              Text([formatDateTime(context, event.from), if (event.to != null) formatDateTime(context, event.to)].join(' – ')),
            if (event.requiresLocation) _LocationNote(),
          ],
        ),
        isThreeLine: true,
        trailing: StatusPillFor(event.status),
      ),
    );
  }
}

/// The next event: a white pass with the yellow lanyard band across the top.
class _NextEventPass extends StatelessWidget {
  const _NextEventPass(this.event);

  final EventItem event;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final text = Theme.of(context).textTheme;
    final from = event.from;

    return Container(
      key: Key('event-${event.id}'),
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: VpColors.surface,
        border: Border.all(color: VpColors.line),
        borderRadius: BorderRadius.circular(14),
        boxShadow: [BoxShadow(color: VpColors.inkDeep.withValues(alpha: 0.25), blurRadius: 24, offset: const Offset(0, 12), spreadRadius: -12)],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const LanyardStrip(),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(t.nextEvent, style: text.labelLarge?.copyWith(color: VpColors.muted)),
                const SizedBox(height: 4),
                Text(event.name, style: text.headlineMedium),
                if (event.initiative != null) ...[
                  const SizedBox(height: 4),
                  Text(event.initiative!, style: text.bodyMedium?.copyWith(color: VpColors.muted)),
                ],
                if (from != null) ...[
                  const SizedBox(height: 12),
                  const Divider(),
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    child: Text(formatEventTime(context, from, event.to),
                        style: text.titleMedium?.copyWith(fontWeight: FontWeight.w500)),
                  ),
                  const Divider(),
                ],
                const SizedBox(height: 12),
                Wrap(
                  spacing: 12,
                  runSpacing: 8,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [StatusPillFor(event.status), if (event.requiresLocation) _LocationNote()],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _LocationNote extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(mainAxisSize: MainAxisSize.min, children: [
      const Icon(Icons.location_on, size: 14, color: VpColors.muted),
      const SizedBox(width: 4),
      Text(AppLocalizations.of(context).locationChecked),
    ]);
  }
}

/// My check-in status as a pill: yellow when I'm checked in right now (the "you / now" colour).
class StatusPillFor extends StatelessWidget {
  const StatusPillFor(this.status, {super.key});

  final EventStatus status;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return switch (status) {
      EventStatus.notCheckedIn => StatusPill(label: t.statusNotCheckedIn, fill: VpColors.slateTint, ink: VpColors.ink),
      EventStatus.checkedIn => StatusPill(label: t.statusCheckedIn, fill: VpColors.vest, ink: VpColors.ink, bold: true),
      EventStatus.checkedOut => StatusPill(label: t.statusCheckedOut, fill: VpColors.leafTint, ink: VpColors.leafInk),
    };
  }
}
