import 'package:flutter/material.dart';

import '../api/models.dart';
import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import 'initiative_screen.dart';
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

/// The `office` choice meaning initiatives that belong to no office, as on the website (`?office=none`).
const noOffice = 'none';

/// Whether [initiative] is in the chosen office: null means all offices, [noOffice] those without one, else an
/// office id. The same rule as the website's Initiatives page (`VolunteerInitiativeController`).
bool inOffice(InitiativeItem initiative, String? office) => switch (office) {
      null => true,
      noOffice => initiative.officeId == null,
      _ => initiative.officeId?.toString() == office,
    };

/// The offices of [initiatives] as (id, name), by name, each once.
List<(int, String)> officesOf(List<InitiativeItem> initiatives) {
  final byId = <int, String>{
    for (final i in initiatives)
      if (i.officeId != null) i.officeId!: i.office ?? '',
  };
  return byId.entries.map((e) => (e.key, e.value)).toList()
    ..sort((a, b) => a.$2.toLowerCase().compareTo(b.$2.toLowerCase()));
}

/// Open initiatives with my membership in each, opening on the ones I've joined. Tapping one opens it, to ask
/// to join (answering its questions) or withdraw a pending request.
class InitiativesScreen extends StatefulWidget {
  const InitiativesScreen({super.key});

  @override
  State<InitiativesScreen> createState() => _InitiativesScreenState();
}

class _InitiativesScreenState extends State<InitiativesScreen> {
  InitiativeFilter _filter = InitiativeFilter.joined;

  /// null: all offices; [noOffice]; or an office id.
  String? _office;

  // One key per chip, to scroll the chosen one into view: the row scrolls sideways on phones
  final _chipKeys = {for (final filter in InitiativeFilter.values) filter: GlobalKey()};
  // The row is built again (at the start) whenever the list reloads, e.g. after joining: show the chosen chip then too
  late final _chipRow = ScrollController(keepScrollOffset: false, onAttach: (_) => _revealChosen(animate: false));

  @override
  void dispose() {
    _chipRow.dispose();
    super.dispose();
  }

  void _show(InitiativeFilter filter) {
    setState(() => _filter = filter);
    _revealChosen();
  }

  /// Scrolls the chip row so the chosen chip is in view, after this frame (when it has been laid out).
  void _revealChosen({bool animate = true}) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final chip = _chipKeys[_filter]!.currentContext;
      if (chip != null && chip.mounted) {
        Scrollable.ensureVisible(chip,
            alignment: 0.5, duration: animate ? const Duration(milliseconds: 200) : Duration.zero);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return AsyncList<InitiativeItem>(
      key: const PageStorageKey('initiatives'),
      load: () => AppScope.read(context).api.initiatives(),
      where: (initiative) => _filter.matches(initiative.membership) && inOffice(initiative, _office),
      headerBuilder: (context, all) {
        final offices = officesOf(all);
        final anyWithoutOffice = all.any((i) => i.officeId == null);
        final known = _office == null ||
            (_office == noOffice ? anyWithoutOffice : offices.any((o) => o.$1.toString() == _office));
        if (!known) {
          // The chosen office has no open initiative any more (after a reload): back to all offices
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted) setState(() => _office = null);
          });
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // The counts are for the chosen office, as on the website
            _FilterBar(
                selected: _filter,
                all: all.where((i) => inOffice(i, _office)).toList(),
                onSelected: _show,
                chipKeys: _chipKeys,
                chipRow: _chipRow),
            if (offices.isNotEmpty)
              _OfficePicker(
                offices: offices,
                anyWithoutOffice: anyWithoutOffice,
                selected: known ? _office : null,
                onSelected: (office) => setState(() => _office = office),
              ),
          ],
        );
      },
      groups: (context, items) => _byOffice(context, items),
      emptyText: switch (_filter) {
        _ when _office != null => t.noInitiativesHere,
        InitiativeFilter.joined => t.noJoinedInitiatives,
        InitiativeFilter.all => t.noInitiativesOpen,
        _ => t.noInitiativesHere,
      },
      emptyAction: _filter == InitiativeFilter.joined && _office == null
          ? FilledButton(
              key: const Key('showInitiativesToJoin'),
              onPressed: () => _show(InitiativeFilter.notJoined),
              child: Text(t.showInitiativesToJoin),
            )
          : null,
      itemBuilder: (context, initiative, reload) => ListTile(
        key: Key('initiative-${initiative.id}'),
        onTap: () async {
          final changed = await Navigator.of(context).push<bool>(MaterialPageRoute(
            builder: (_) => InitiativeScreen(initiativeId: initiative.id, title: initiative.name),
          ));
          if (changed == true) {
            reload();
          }
        },
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

  /// One section per office, by office name, under a heading with its count; those without an office last.
  List<ListGroup<InitiativeItem>> _byOffice(BuildContext context, List<InitiativeItem> items) {
    final t = AppLocalizations.of(context);
    final groups = [
      for (final (id, name) in officesOf(items))
        (key: 'office-$id', name: name, items: items.where((i) => i.officeId == id).toList()),
      if (items.any((i) => i.officeId == null))
        (key: 'office-$noOffice', name: t.noOffice, items: items.where((i) => i.officeId == null).toList()),
    ];
    return [
      for (final group in groups)
        ListGroup(
          header: _OfficeHeading(key: Key('heading-${group.key}'), name: group.name, count: group.items.length),
          items: group.items,
        ),
    ];
  }
}

/// An office's name over its initiatives, with how many there are.
class _OfficeHeading extends StatelessWidget {
  const _OfficeHeading({super.key, required this.name, required this.count});

  final String name;
  final int count;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsetsDirectional.fromSTEB(16, 16, 16, 8),
      child: Row(
        children: [
          const Icon(Icons.apartment, size: 20, color: VpColors.ink),
          const SizedBox(width: 8),
          Flexible(child: Text(name, style: text.titleMedium?.copyWith(fontWeight: FontWeight.w600))),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 1),
            decoration: const ShapeDecoration(color: VpColors.slateTint, shape: StadiumBorder()),
            child: Text('$count', style: text.labelLarge?.copyWith(color: VpColors.muted)),
          ),
        ],
      ),
    );
  }
}

/// Chooses the office to show: all, one, or (if some have none) those without an office.
class _OfficePicker extends StatelessWidget {
  const _OfficePicker({required this.offices, required this.anyWithoutOffice, required this.selected, required this.onSelected});

  final List<(int, String)> offices;
  final bool anyWithoutOffice;
  final String? selected;
  final ValueChanged<String?> onSelected;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 4),
      // A plain DropdownButton (not the FormField one): it shows [selected] on every build, also when the
      // screen resets it
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: t.officeFilter,
          prefixIcon: const Icon(Icons.apartment),
          contentPadding: const EdgeInsetsDirectional.only(start: 12, end: 8),
        ),
        child: DropdownButtonHideUnderline(
          child: DropdownButton<String?>(
            key: const Key('officeFilter'),
            value: selected,
            isExpanded: true,
            items: [
              DropdownMenuItem(key: const Key('office-all'), value: null, child: Text(t.allOffices)),
              for (final (id, name) in offices)
                DropdownMenuItem(key: Key('office-$id'), value: '$id', child: Text(name, overflow: TextOverflow.ellipsis)),
              if (anyWithoutOffice)
                DropdownMenuItem(key: const Key('office-$noOffice'), value: noOffice, child: Text(t.noOffice)),
            ],
            onChanged: onSelected,
          ),
        ),
      ),
    );
  }
}

/// One chip per filter with its count; the chosen one is filled navy, as on the website. Under Not joined,
/// a line says how to join.
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
            child: Text(t.tapToJoin, style: const TextStyle(color: VpColors.muted)),
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
