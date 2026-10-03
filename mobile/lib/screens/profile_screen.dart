import 'package:flutter/material.dart';

import '../app_scope.dart';
import '../l10n/app_localizations.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final state = AppScope.of(context);
    final me = state.me;

    return Scaffold(
      appBar: AppBar(title: Text(t.profile), actions: const [LanguageMenuButton()]),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (me != null) ...[
            ListTile(
              leading: const CircleAvatar(child: Icon(Icons.person)),
              title: Text(me.username),
              subtitle: me.email == null
                  ? null
                  : Text(me.email!, textDirection: TextDirection.ltr, maxLines: 1, overflow: TextOverflow.ellipsis),
            ),
            const SizedBox(height: 8),
            // My standing on a white card, like "Your grade and points" on the website's home page
            Card(
              child: Column(children: [
                ListTile(leading: const Icon(Icons.military_tech), title: Text(t.grade), trailing: _Value(me.grade ?? t.unranked)),
                const Divider(height: 1),
                ListTile(leading: const Icon(Icons.star), title: Text(t.points), trailing: _Value('${me.points}')),
                const Divider(height: 1),
                ListTile(leading: const Icon(Icons.badge), title: Text(t.roles), trailing: _Value(me.roles.join(', '))),
              ]),
            ),
            const SizedBox(height: 8),
          ],
          ListTile(leading: const Icon(Icons.dns), title: Text(t.serverUrl), subtitle: Text(state.serverUrl ?? '', textDirection: TextDirection.ltr)),
          const SizedBox(height: 24),
          OutlinedButton.icon(
            key: const Key('logoutButton'),
            onPressed: () async {
              await AppScope.read(context).logout();
              if (context.mounted) {
                Navigator.of(context).popUntil((route) => route.isFirst);
              }
            },
            icon: const Icon(Icons.logout),
            label: Text(t.logout),
          ),
        ],
      ),
    );
  }
}

/// Switches between English and Arabic (the layout turns right-to-left for Arabic).
class LanguageMenuButton extends StatelessWidget {
  const LanguageMenuButton({super.key});

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    return PopupMenuButton<String>(
      key: const Key('languageMenu'),
      icon: const Icon(Icons.translate),
      tooltip: AppLocalizations.of(context).language,
      initialValue: state.language,
      onSelected: (language) => AppScope.read(context).setLanguage(language),
      itemBuilder: (_) => const [
        PopupMenuItem(value: 'en', child: Text('English')),
        PopupMenuItem(value: 'ar', child: Text('العربية')),
      ],
    );
  }
}

class _Value extends StatelessWidget {
  const _Value(this.text);

  final String text;

  @override
  Widget build(BuildContext context) =>
      Text(text, style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600));
}
