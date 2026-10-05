import 'package:flutter/material.dart';

import '../api/models.dart';
import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import 'widgets.dart';

/// The options above the initiatives list, as on the website's Initiatives page (`?show=`).
enum InitiativeFilter {
  joined,
  pending,
  rejected,
  notJoined,
  all;

  bool matches(Membership membership) => switch (this) {
        joined => membership == Membership.approved,
        pending => membership == Membership.pending,
        rejected => membership == Membership.rejected,
        notJoined => membership == Membership.none,
        all => true,
      };
}

/// Open initiatives with my membership in each, opening on the ones I've joined. View only: joining
/// (and answering the initiative's questions) happens on the website.
class InitiativesScreen extends StatefulWidget {
  const InitiativesScreen({super.key});

  @override
  State<InitiativesScreen> createState() => _InitiativesScreenState();
}

class _InitiativesScreenState extends State<InitiativesScreen> {
  InitiativeFilter _filter = InitiativeFilter.joined;

  // One key per chip, to scroll the chosen one into view: the row scrolls sideways on phones
  final _chipKeys = {for (final filter in InitiativeFilter.values) filter: GlobalKey()};
  final _chipRow = ScrollController(keepScrollOffset: false);

  @override
  void dispose() {
    _chipRow.dispose();
    super.dispose();
  }

  void _show(InitiativeFilter filter) {
    setState(() => _filter = filter);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final chip = _chipKeys[filter]!.currentContext;
      if (chip != null && chip.mounted) {
        Scrollable.ensureVisible(chip, alignment: 0.5, duration: const Duration(milliseconds: 200));
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return AsyncList<InitiativeItem>(
      key: const PageStorageKey('initiatives'),
      load: () => AppScope.read(context).api.initiatives(),
      where: (initiative) => _filter.matches(initiative.membership),
      headerBuilder: (context, all) => _FilterBar(selected: _filter, all: all, onSelected: _show, chipKeys: _chipKeys, chipRow: _chipRow),
      emptyText: switch (_filter) {
        InitiativeFilter.joined => t.noJoinedInitiatives,
        InitiativeFilter.all => t.noInitiativesOpen,
        _ => t.noInitiativesHere,
      },
      emptyAction: _filter == InitiativeFilter.joined
          ? FilledButton(
              key: const Key('showInitiativesToJoin'),
              onPressed: () => _show(InitiativeFilter.notJoined),
              child: Text(t.showInitiativesToJoin),
            )
          : null,
      itemBuilder: (context, initiative, _) => ListTile(
        key: Key('initiative-${initiative.id}'),
        title: Text(initiative.name),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (initiative.office != null) Text(initiative.office!, style: const TextStyle(color: VpColors.muted)),
            if (initiative.description != null && initiative.description!.isNotEmpty)
              Text(initiative.description!, maxLines: 3, overflow: TextOverflow.ellipsis),
          ],
        ),
        isThreeLine: initiative.office != null && (initiative.description ?? '').isNotEmpty,
        trailing: MembershipPill(initiative.membership),
      ),
    );
  }
}

/// One chip per filter with its count; the chosen one is filled navy, as on the website. Under Not joined,
/// a line says where to join.
class _FilterBar extends StatelessWidget {
  const _FilterBar({required this.selected, required this.all, required this.onSelected, required this.chipKeys, required this.chipRow});

  final InitiativeFilter selected;
  final List<InitiativeItem> all;
  final ValueChanged<InitiativeFilter> onSelected;
  final Map<InitiativeFilter, GlobalKey> chipKeys;
  final ScrollController chipRow;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final labelStyle = Theme.of(context).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w500, letterSpacing: 0);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SingleChildScrollView(
          // Not remembered: it would share the list's saved scroll offset, and a new login should start at Joined
          controller: chipRow,
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Row(
            children: [
              for (final filter in InitiativeFilter.values) ...[
                if (filter != InitiativeFilter.joined) const SizedBox(width: 8),
                _chip(
                  filter: filter,
                  label: switch (filter) {
                    InitiativeFilter.joined => t.filterJoined,
                    InitiativeFilter.pending => t.filterPending,
                    InitiativeFilter.rejected => t.filterRejected,
                    InitiativeFilter.notJoined => t.filterNotJoined,
                    InitiativeFilter.all => t.filterAll,
                  },
                  count: all.where((i) => filter.matches(i.membership)).length,
                  labelStyle: labelStyle,
                ),
              ],
            ],
          ),
        ),
        if (selected == InitiativeFilter.notJoined)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: Text(t.joinOnWebsite, style: const TextStyle(color: VpColors.muted)),
          ),
      ],
    );
  }

  Widget _chip({required InitiativeFilter filter, required String label, required int count, TextStyle? labelStyle}) {
    final chosen = filter == selected;
    return KeyedSubtree(
      key: chipKeys[filter],
      child: ChoiceChip(
        key: Key('filter-${filter.name}'),
        selected: chosen,
        onSelected: (_) => onSelected(filter),
        showCheckmark: false,
        backgroundColor: VpColors.surface,
        selectedColor: VpColors.ink,
        side: BorderSide(color: chosen ? VpColors.ink : VpColors.line),
        label: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(label, style: labelStyle?.copyWith(color: chosen ? Colors.white : VpColors.ink)),
            const SizedBox(width: 6),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 1),
              decoration: ShapeDecoration(color: chosen ? Colors.white : VpColors.slateTint, shape: const StadiumBorder()),
              child: Text('$count', style: labelStyle?.copyWith(color: chosen ? VpColors.ink : VpColors.muted)),
            ),
          ],
        ),
      ),
    );
  }
}

/// My membership as a pill, in the website's badge colours.
class MembershipPill extends StatelessWidget {
  const MembershipPill(this.membership, {super.key});

  final Membership membership;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return switch (membership) {
      Membership.none => StatusPill(label: t.membershipNone, fill: VpColors.slateTint, ink: VpColors.ink),
      Membership.pending => StatusPill(label: t.membershipPending, fill: VpColors.vestTint, ink: VpColors.ink),
      Membership.approved => StatusPill(label: t.membershipApproved, fill: VpColors.leafTint, ink: VpColors.leafInk),
      Membership.rejected => StatusPill(label: t.membershipRejected, fill: VpColors.brickTint, ink: VpColors.brickInk),
    };
  }
}
