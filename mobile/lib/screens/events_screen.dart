import 'package:flutter/material.dart';

import '../api/models.dart';
import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import 'widgets.dart';

/// Upcoming events of initiatives I'm an approved member of, with my check-in status.
class EventsScreen extends StatelessWidget {
  const EventsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return AsyncList<EventItem>(
      key: const PageStorageKey('events'),
      load: () => AppScope.read(context).api.events(),
      emptyText: t.noEvents,
      itemBuilder: (context, event, _) => ListTile(
        key: Key('event-${event.id}'),
        title: Text(event.name),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (event.initiative != null) Text(event.initiative!),
            if (event.from != null)
              Text([formatDateTime(context, event.from), if (event.to != null) formatDateTime(context, event.to)].join(' – ')),
            if (event.requiresLocation)
              Row(children: [const Icon(Icons.location_on, size: 14), const SizedBox(width: 4), Text(t.locationChecked)]),
          ],
        ),
        isThreeLine: true,
        trailing: _StatusChip(event.status),
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip(this.status);

  final EventStatus status;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final (label, color) = switch (status) {
      EventStatus.notCheckedIn => (t.statusNotCheckedIn, colors.outline),
      EventStatus.checkedIn => (t.statusCheckedIn, Colors.green),
      EventStatus.checkedOut => (t.statusCheckedOut, colors.primary),
    };
    return Chip(label: Text(label), side: BorderSide(color: color), visualDensity: VisualDensity.compact);
  }
}
