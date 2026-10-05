import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/models.dart';
import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import 'initiatives_screen.dart';
import 'widgets.dart';

/// One initiative: what it is and my status in it. Not joined yet, it shows the initiative's questions and
/// sends the join request with the answers, as the website's initiative page does; while the request is
/// pending, it can be withdrawn. Pops with `true` when the membership changed, so the list reloads.
class InitiativeScreen extends StatefulWidget {
  const InitiativeScreen({super.key, required this.initiativeId, required this.title});

  final int initiativeId;
  final String title;

  @override
  State<InitiativeScreen> createState() => _InitiativeScreenState();
}

class _InitiativeScreenState extends State<InitiativeScreen> {
  late Future<InitiativeDetail> _detail;

  /// Choice answers by question id: "1"/"0" for yes/no, or choice numbers from 1.
  final Map<int, Set<String>> _choices = {};
  final Map<int, TextEditingController> _texts = {};
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _detail = _load();
  }

  @override
  void dispose() {
    for (final controller in _texts.values) {
      controller.dispose();
    }
    super.dispose();
  }

  Future<InitiativeDetail> _load() {
    final state = AppScope.read(context);
    return state.guard(() => state.api.initiative(widget.initiativeId));
  }

  TextEditingController _text(int questionId) => _texts.putIfAbsent(questionId, TextEditingController.new);

  /// The answered questions only: like the website's form, every question may be left blank.
  Map<int, List<String>> _answers(InitiativeDetail detail) {
    final answers = <int, List<String>>{};
    for (final question in detail.questions) {
      final text = _text(question.id).text.trim();
      final values = question.type == QuestionType.text
          ? [if (text.isNotEmpty) text]
          : ([...?_choices[question.id]]..sort((a, b) => int.parse(a).compareTo(int.parse(b))));
      if (values.isNotEmpty) {
        answers[question.id] = values;
      }
    }
    return answers;
  }

  /// Runs a join or withdraw call; on success says so and goes back to the list, which reloads.
  Future<void> _send(Future<void> Function(ApiClient api) call, String done) async {
    final state = AppScope.read(context);
    final messenger = ScaffoldMessenger.of(context);
    final navigator = Navigator.of(context);
    setState(() => _busy = true);
    try {
      await state.guard(() => call(state.api));
      messenger.showSnackBar(SnackBar(content: Text(done)));
      navigator.pop(true);
    } on UnauthorizedException {
      // guard() logged out; the login screen takes over
    } catch (e) {
      if (!mounted) {
        return;
      }
      final t = AppLocalizations.of(context);
      messenger.showSnackBar(SnackBar(
        content: Text(e is ApiException && e.statusCode == 404 ? t.initiativeClosed : describeError(context, e)),
      ));
      setState(() => _busy = false);
    }
  }

  Future<void> _join(InitiativeDetail detail) {
    final answers = _answers(detail);
    return _send((api) => api.join(detail.id, answers), AppLocalizations.of(context).requestSent);
  }

  Future<void> _withdraw() async {
    final t = AppLocalizations.of(context);
    final sure = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        content: Text(t.withdrawConfirm),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: Text(t.cancel)),
          TextButton(
            key: const Key('confirmWithdraw'),
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(t.withdrawRequest),
          ),
        ],
      ),
    );
    if (sure == true) {
      await _send((api) => api.withdraw(widget.initiativeId), t.requestWithdrawn);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(widget.title)),
      body: FutureBuilder<InitiativeDetail>(
        future: _detail,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return MessageView(
              icon: Icons.cloud_off,
              text: snapshot.error is ApiException && (snapshot.error as ApiException).statusCode == 404
                  ? t.initiativeClosed
                  : describeError(context, snapshot.error!),
              action: FilledButton.tonal(onPressed: () => setState(() => _detail = _load()), child: Text(t.retry)),
            );
          }
          return _content(context, snapshot.data!);
        },
      ),
    );
  }

  Widget _content(BuildContext context, InitiativeDetail detail) {
    final t = AppLocalizations.of(context);
    final text = Theme.of(context).textTheme;
    final muted = text.bodyMedium?.copyWith(color: VpColors.muted);

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 32),
      children: [
        Text(detail.name, style: text.headlineSmall),
        if (detail.office != null) ...[const SizedBox(height: 4), Text(detail.office!, style: muted)],
        const SizedBox(height: 12),
        Align(alignment: AlignmentDirectional.centerStart, child: MembershipPill(detail.membership)),
        if ((detail.description ?? '').isNotEmpty) ...[const SizedBox(height: 16), Text(detail.description!)],
        const SizedBox(height: 24),
        ...switch (detail.membership) {
          Membership.none => [
              if (detail.questions.isNotEmpty) ...[
                Text(t.joinQuestionsIntro, style: muted),
                const SizedBox(height: 12),
                for (final question in detail.questions) _QuestionCard(question: question, answer: _answerFor(question)),
                const SizedBox(height: 8),
              ],
              FilledButton(
                key: const Key('joinButton'),
                onPressed: _busy ? null : () => _join(detail),
                child: _busy ? const _ButtonSpinner() : Text(t.requestToJoin),
              ),
            ],
          Membership.pending => [
              Text(t.pendingHint),
              const SizedBox(height: 16),
              OutlinedButton(
                key: const Key('withdrawButton'),
                onPressed: _busy ? null : _withdraw,
                child: _busy ? const _ButtonSpinner() : Text(t.withdrawRequest),
              ),
            ],
          Membership.approved => [Text(t.memberHint)],
          Membership.rejected => [Text(t.notApprovedHint)],
        },
      ],
    );
  }

  /// The input for one question, holding its answer in this screen's state.
  Widget _answerFor(Question question) {
    final t = AppLocalizations.of(context);
    final chosen = _choices.putIfAbsent(question.id, () => <String>{});

    Widget chip(String value, String label, {required bool many}) => _AnswerChip(
          key: Key('answer-${question.id}-$value'),
          label: label,
          selected: chosen.contains(value),
          onTap: () => setState(() {
            if (chosen.contains(value)) {
              chosen.remove(value);
            } else {
              if (!many) {
                chosen.clear();
              }
              chosen.add(value);
            }
          }),
        );

    return switch (question.type) {
      QuestionType.yesNo => Wrap(spacing: 8, runSpacing: 8, children: [
          chip('1', t.yes, many: false),
          chip('0', t.no, many: false),
        ]),
      QuestionType.oneChoice || QuestionType.manyChoices => Wrap(spacing: 8, runSpacing: 8, children: [
          for (final (index, label) in question.choices.indexed)
            chip('${index + 1}', label, many: question.type == QuestionType.manyChoices),
        ]),
      QuestionType.text => TextField(
          key: Key('answer-${question.id}'),
          controller: _text(question.id),
          minLines: 1,
          maxLines: 4,
          textInputAction: TextInputAction.newline,
          decoration: InputDecoration(hintText: t.yourAnswer),
        ),
    };
  }
}

class _QuestionCard extends StatelessWidget {
  const _QuestionCard({required this.question, required this.answer});

  final Question question;
  final Widget answer;

  @override
  Widget build(BuildContext context) {
    return Card(
      key: Key('question-${question.id}'),
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(question.text, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            answer,
          ],
        ),
      ),
    );
  }
}

/// An answer choice: outlined, or filled navy once chosen (like the filter chips on the list).
class _AnswerChip extends StatelessWidget {
  const _AnswerChip({super.key, required this.label, required this.selected, required this.onTap});

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ChoiceChip(
      label: Text(label),
      selected: selected,
      onSelected: (_) => onTap(),
      showCheckmark: false,
      backgroundColor: VpColors.surface,
      selectedColor: VpColors.ink,
      side: BorderSide(color: selected ? VpColors.ink : VpColors.line),
      labelStyle: Theme.of(context).textTheme.labelLarge?.copyWith(
            color: selected ? Colors.white : VpColors.ink,
            fontWeight: FontWeight.w500,
            letterSpacing: 0,
          ),
    );
  }
}

class _ButtonSpinner extends StatelessWidget {
  const _ButtonSpinner();

  @override
  Widget build(BuildContext context) =>
      const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: VpColors.muted));
}
