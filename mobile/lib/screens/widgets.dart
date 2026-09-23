import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../app_scope.dart';
import '../l10n/app_localizations.dart';

/// Loads a list from the API and shows it, with loading, error (+ retry), empty and
/// pull-to-refresh states. Calls go through [AppState.guard], so an expired token logs out.
class AsyncList<T> extends StatefulWidget {
  const AsyncList({super.key, required this.load, required this.itemBuilder, required this.emptyText});

  final Future<List<T>> Function() load;
  final Widget Function(BuildContext context, T item, VoidCallback reload) itemBuilder;
  final String emptyText;

  @override
  State<AsyncList<T>> createState() => AsyncListState<T>();
}

class AsyncListState<T> extends State<AsyncList<T>> {
  late Future<List<T>> _items;

  @override
  void initState() {
    super.initState();
    _items = _fetch();
  }

  Future<List<T>> _fetch() => AppScope.read(context).guard(widget.load);

  void reload() => setState(() => _items = _fetch());

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<T>>(
      future: _items,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return MessageView(
            icon: Icons.cloud_off,
            text: describeError(context, snapshot.error!),
            action: FilledButton.tonal(onPressed: reload, child: Text(AppLocalizations.of(context).retry)),
          );
        }
        final items = snapshot.data!;
        return RefreshIndicator(
          onRefresh: () async {
            reload();
            await _items.catchError((_) => <T>[]);
          },
          child: items.isEmpty
              ? ListView(children: [SizedBox(height: 360, child: MessageView(icon: Icons.inbox, text: widget.emptyText))])
              : ListView.separated(
                  itemCount: items.length,
                  separatorBuilder: (_, _) => const Divider(height: 1),
                  itemBuilder: (context, index) => widget.itemBuilder(context, items[index], reload),
                ),
        );
      },
    );
  }
}

/// A centered icon + text (+ optional button), for empty and error states.
class MessageView extends StatelessWidget {
  const MessageView({super.key, required this.icon, required this.text, this.action});

  final IconData icon;
  final String text;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 56, color: Theme.of(context).colorScheme.outline),
            const SizedBox(height: 16),
            Text(text, textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyLarge),
            if (action != null) ...[const SizedBox(height: 16), action!],
          ],
        ),
      ),
    );
  }
}

/// Date and time in the app's language, e.g. "Sep 23, 2026 9:00 AM".
String formatDateTime(BuildContext context, DateTime? value) {
  if (value == null) {
    return '';
  }
  final locale = Localizations.localeOf(context).languageCode;
  return DateFormat.yMMMd(locale).add_jm().format(value);
}
