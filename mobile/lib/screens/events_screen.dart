import 'package:flutter/material.dart';

import '../api/models.dart';
import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import 'widgets.dart';

/// My grade and points, then the upcoming events of initiatives I'm an approved member of, with my check-in
/// status. The next one is shown as a pass, like on the website's home page. Loading (or pulling to refresh)
/// also refreshes my grade and points, which an admin can change at any time.
class EventsScreen extends StatelessWidget {
  const EventsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return AsyncList<EventItem>(
      key: const PageStorageKey('events'),
      load: () async {
        final state = AppScope.read(context);
        final events = state.api.events();
        await state.refreshMe();
        return events;
      },
      headerBuilder: (context, _) => const _Standing(),
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

/// My grade and points, as "Your grade and points" on the website's home page: what my volunteering has earned.
class _Standing extends StatelessWidget {
  const _Standing();

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final me = AppScope.of(context).me;
    if (me == null) {
      return const SizedBox.shrink();
    }
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
      child: Container(
        key: const Key('standing'),
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
        decoration: BoxDecoration(
          color: VpColors.surface,
          border: Border.all(color: VpColors.line),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(t.yourStanding, style: Theme.of(context).textTheme.titleSmall?.copyWith(color: VpColors.muted)),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // A grade name can be long (and wrap); points are a short number
                Expanded(
                  flex: 3,
                  child: _Figure(
                      key: const Key('grade'), icon: Icons.military_tech, label: t.grade, value: me.grade ?? t.unranked, maxLines: 2),
                ),
                const SizedBox(width: 12),
                Expanded(
                    flex: 2, child: _Figure(key: const Key('points'), icon: Icons.star, label: t.points, value: '${me.points}')),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// One figure of my standing: a yellow badge (yellow means mine), the label, and the value large.
class _Figure extends StatelessWidget {
  const _Figure({super.key, required this.icon, required this.label, required this.value, this.maxLines = 1});

  final IconData icon;
  final String label;
  final String value;
  final int maxLines;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Row(
      children: [
        Container(
          width: 44,
          height: 44,
          decoration: const BoxDecoration(color: VpColors.vestTint, shape: BoxShape.circle),
          child: Icon(icon, color: VpColors.ink),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: text.labelMedium?.copyWith(color: VpColors.muted)),
              Text(value,
                  maxLines: maxLines,
                  overflow: TextOverflow.ellipsis,
                  style: text.titleLarge?.copyWith(fontWeight: FontWeight.w600, color: VpColors.ink)),
            ],
          ),
        ),
      ],
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
