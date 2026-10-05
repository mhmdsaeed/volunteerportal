import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';

/// Loads a list from the API and shows it, with loading, error (+ retry), empty and
/// pull-to-refresh states. Calls go through [AppState.guard], so an expired token logs out.
class AsyncList<T> extends StatefulWidget {
  const AsyncList({
    super.key,
    required this.load,
    required this.itemBuilder,
    required this.emptyText,
    this.firstItemBuilder,
    this.where,
    this.headerBuilder,
    this.emptyAction,
  });

  final Future<List<T>> Function() load;
  final Widget Function(BuildContext context, T item, VoidCallback reload) itemBuilder;
  final String emptyText;

  /// Builds the first item differently (e.g. the next event as a pass), shown above the list.
  final Widget Function(BuildContext context, T item, VoidCallback reload)? firstItemBuilder;

  /// Shows only the loaded items that pass; changing it filters again without reloading.
  final bool Function(T item)? where;

  /// Shown above the list, also when it is empty, and given every loaded item (e.g. filter chips with counts).
  final Widget Function(BuildContext context, List<T> all)? headerBuilder;

  /// A button under [emptyText].
  final Widget? emptyAction;

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
        final all = snapshot.data!;
        final where = widget.where;
        final items = where == null ? all : all.where(where).toList();
        final header = widget.headerBuilder?.call(context, all);
        return RefreshIndicator(
          onRefresh: () async {
            reload();
            await _items.catchError((_) => <T>[]);
          },
          child: items.isEmpty
              ? ListView(physics: const AlwaysScrollableScrollPhysics(), children: [
                  ?header,
                  SizedBox(
                    height: 360,
                    child: MessageView(icon: Icons.inbox, text: widget.emptyText, action: widget.emptyAction),
                  ),
                ])
              : _list(context, items, header),
        );
      },
    );
  }

  /// The items as rows on a white sheet; with [AsyncList.firstItemBuilder], the first item sits above it.
  Widget _list(BuildContext context, List<T> items, Widget? header) {
    final first = widget.firstItemBuilder;
    final rows = first == null ? items : items.skip(1).toList();
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(), // so pull-to-refresh works on short lists
      children: [
        ?header,
        if (first != null) Padding(padding: const EdgeInsets.fromLTRB(16, 16, 16, 8), child: first(context, items.first, reload)),
        if (rows.isNotEmpty)
          Material(
            color: VpColors.surface, // a Material, so tapped rows still show their ripple
            child: Column(children: [
              for (final (index, item) in rows.indexed) ...[
                if (index > 0) const Divider(height: 1),
                widget.itemBuilder(context, item, reload),
              ],
            ]),
          ),
      ],
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
  return _westernDigits(DateFormat.yMMMd(locale).add_jm()).format(value);
}

/// An event's time for the next-event pass, e.g. "Sunday, October 4 08:00–22:00" (the end shows only its
/// time when it is the same day).
String formatEventTime(BuildContext context, DateTime from, DateTime? to) {
  final locale = Localizations.localeOf(context).languageCode;
  final dayAndTime = _westernDigits(DateFormat.MMMMEEEEd(locale).add_Hm());
  final start = dayAndTime.format(from);
  if (to == null) {
    return start;
  }
  final sameDay = DateUtils.isSameDay(from, to);
  return '$start–${sameDay ? _westernDigits(DateFormat.Hm(locale)).format(to) : dayAndTime.format(to)}';
}

/// Arabic dates keep Western digits (23, 08:00), like the website, instead of Arabic-Indic (٢٣).
DateFormat _westernDigits(DateFormat format) => format..useNativeDigits = false;
