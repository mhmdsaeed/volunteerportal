import 'package:flutter/material.dart';

import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';
import 'forgot_password_screen.dart';
import 'profile_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _form = GlobalKey<FormState>();
  late final TextEditingController _server;
  final _username = TextEditingController();
  final _password = TextEditingController();
  bool _busy = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _server = TextEditingController(text: AppScope.read(context).serverUrl ?? '');
  }

  @override
  void dispose() {
    _server.dispose();
    _username.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) {
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await AppScope.read(context).login(_server.text, _username.text, _password.text);
    } catch (e) {
      if (mounted) {
        setState(() => _error = describeError(context, e));
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final state = AppScope.of(context);
    final error = _error ?? (state.sessionExpired ? t.sessionExpired : null);

    return Scaffold(
      // A plain bar here, so the navy header below stands alone
      appBar: AppBar(
        backgroundColor: VpColors.paper,
        foregroundColor: VpColors.ink,
        actions: const [LanguageMenuButton()],
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _form,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const _LoginHeader(),
                    const SizedBox(height: 28),
                    TextFormField(
                      key: const Key('server'),
                      controller: _server,
                      keyboardType: TextInputType.url,
                      autocorrect: false,
                      textDirection: TextDirection.ltr,
                      decoration: InputDecoration(labelText: t.serverUrl, hintText: t.serverUrlHint),
                      validator: (v) => validateServerUrl(t, v),
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      key: const Key('username'),
                      controller: _username,
                      autocorrect: false,
                      textInputAction: TextInputAction.next,
                      decoration: InputDecoration(labelText: t.username),
                      validator: (v) => (v ?? '').trim().isEmpty ? t.required : null,
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      key: const Key('password'),
                      controller: _password,
                      obscureText: true,
                      onFieldSubmitted: (_) => _submit(),
                      decoration: InputDecoration(labelText: t.password),
                      validator: (v) => (v ?? '').isEmpty ? t.required : null,
                    ),
                    Align(
                      alignment: AlignmentDirectional.centerEnd,
                      child: TextButton(
                        key: const Key('forgotPassword'),
                        onPressed: () => Navigator.of(context).push(MaterialPageRoute<void>(
                            builder: (_) => ForgotPasswordScreen(serverUrl: _server.text))),
                        child: Text(t.forgotPassword),
                      ),
                    ),
                    if (error != null) ...[
                      const SizedBox(height: 16),
                      Text(error, key: const Key('loginError'), style: TextStyle(color: Theme.of(context).colorScheme.error)),
                    ],
                    const SizedBox(height: 24),
                    FilledButton(
                      key: const Key('loginButton'),
                      onPressed: _busy ? null : _submit,
                      child: Padding(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        child: Text(_busy ? t.loggingIn : t.login),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// The server address field's check, shared by the login and forgot-password screens.
String? validateServerUrl(AppLocalizations t, String? v) {
  final value = (v ?? '').trim();
  if (value.isEmpty) {
    return t.required;
  }
  final uri = Uri.tryParse(value.contains('://') ? value : 'https://$value');
  return uri == null || uri.host.isEmpty || !uri.scheme.startsWith('http') ? t.invalidServerUrl : null;
}

/// The portal's name on navy under the yellow lanyard band, like the website's sign-in page.
class _LoginHeader extends StatelessWidget {
  const _LoginHeader();

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final text = Theme.of(context).textTheme;
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: ColoredBox(
        color: VpColors.ink,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const LanyardStrip(),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 28, 20, 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(t.appTitle, style: text.headlineMedium?.copyWith(color: Colors.white)),
                  const SizedBox(height: 8),
                  Text(t.loginTagline, style: text.bodyMedium?.copyWith(color: Colors.white.withValues(alpha: 0.85), height: 1.5)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
