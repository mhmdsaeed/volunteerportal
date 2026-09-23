import 'package:flutter/material.dart';

import '../api/models.dart';
import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import 'widgets.dart';

/// My check-ins and check-outs, newest first.
class HistoryScreen extends StatelessWidget {
  const HistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return AsyncList<AttendanceItem>(
      key: const PageStorageKey('history'),
      load: () => AppScope.read(context).api.attendance(),
      emptyText: t.noHistory,
      itemBuilder: (context, item, _) => ListTile(
        leading: Icon(item.isCheckOut ? Icons.logout : Icons.login, color: item.isCheckOut ? null : Colors.green),
        title: Text(item.event),
        subtitle: Text([if (item.initiative != null) item.initiative!, formatDateTime(context, item.time)].join('\n')),
        isThreeLine: item.initiative != null,
        trailing: Text(item.isCheckOut ? t.checkOut : t.checkIn),
      ),
    );
  }
}
