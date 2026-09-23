import 'package:flutter/material.dart';

import '../api/models.dart';
import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import 'widgets.dart';

/// My notifications (already in the app's language); tapping an unread one marks it read.
class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  final _list = GlobalKey<AsyncListState<NotificationItem>>();

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final state = AppScope.read(context);
    return Column(
      children: [
        Align(
          alignment: AlignmentDirectional.centerEnd,
          child: TextButton.icon(
            onPressed: () async {
              await state.guard(() => state.api.markAllNotificationsRead());
              _list.currentState?.reload();
            },
            icon: const Icon(Icons.done_all),
            label: Text(t.markAllRead),
          ),
        ),
        Expanded(
          child: AsyncList<NotificationItem>(
            key: _list,
            load: () => state.api.notifications(),
            emptyText: t.noNotifications,
            itemBuilder: (context, item, reload) => ListTile(
              leading: Icon(item.read ? Icons.notifications_none : Icons.notifications_active,
                  color: item.read ? null : Theme.of(context).colorScheme.primary),
              title: Text(item.message, style: item.read ? null : const TextStyle(fontWeight: FontWeight.w600)),
              subtitle: Text(formatDateTime(context, item.createdAt)),
              onTap: item.read
                  ? null
                  : () async {
                      await state.guard(() => state.api.markNotificationRead(item.id));
                      reload();
                    },
            ),
          ),
        ),
      ],
    );
  }
}
