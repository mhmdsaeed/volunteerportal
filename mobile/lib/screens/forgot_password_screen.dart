import 'package:flutter/material.dart';

import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import 'login_screen.dart';

/// "Forgot your password?": the server emails a link to its website's reset page. The answer is the same
/// whether or not the email has an account, so the screen only says a link is on its way.
class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key, this.serverUrl = ''});

  /// What the login screen's server field held, so it needn't be typed again.
  final String serverUrl;

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _form = GlobalKey<FormState>();
  late final TextEditingController _server = TextEditingController(text: widget.serverUrl);
  final _email = TextEditingController();
  bool _busy = false;
  bool _sent = false;
  String? _error;

  @override
  void dispose() {
    _server.dispose();
    _email.dispose();
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
      await AppScope.read(context).forgotPassword(_server.text, _email.text);
      if (mounted) {
        setState(() => _sent = true);
      }
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
    return Scaffold(
      appBar: AppBar(title: Text(t.forgotPasswordTitle)),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: _sent ? _sentMessage(t) : _formFields(t),
            ),
          ),
        ),
      ),
    );
  }

  Widget _sentMessage(AppLocalizations t) => Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Icon(Icons.mark_email_read_outlined, size: 48),
          const SizedBox(height: 16),
          Text(t.resetLinkSent, key: const Key('resetSent'), textAlign: TextAlign.center),
          const SizedBox(height: 24),
          FilledButton(
            key: const Key('backToLogin'),
            onPressed: () => Navigator.of(context).pop(),
            child: Text(t.backToLogin),
          ),
        ],
      );

  Widget _formFields(AppLocalizations t) => Form(
        key: _form,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(t.forgotPasswordIntro),
            const SizedBox(height: 20),
            TextFormField(
              key: const Key('resetServer'),
              controller: _server,
              keyboardType: TextInputType.url,
              autocorrect: false,
              textDirection: TextDirection.ltr,
              decoration: InputDecoration(labelText: t.serverUrl, hintText: t.serverUrlHint),
              validator: (v) => validateServerUrl(t, v),
            ),
            const SizedBox(height: 12),
            TextFormField(
              key: const Key('resetEmail'),
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              autocorrect: false,
              textDirection: TextDirection.ltr,
              onFieldSubmitted: (_) => _submit(),
              decoration: InputDecoration(labelText: t.email),
              validator: (v) {
                final value = (v ?? '').trim();
                if (value.isEmpty) {
                  return t.required;
                }
                return RegExp(r'^[^@\s]+@[^@\s]+$').hasMatch(value) ? null : t.invalidEmail;
              },
            ),
            if (_error != null) ...[
              const SizedBox(height: 16),
              Text(_error!, key: const Key('resetError'), style: TextStyle(color: Theme.of(context).colorScheme.error)),
            ],
            const SizedBox(height: 24),
            FilledButton(
              key: const Key('sendResetLink'),
              onPressed: _busy ? null : _submit,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 12),
                child: Text(t.sendResetLink),
              ),
            ),
          ],
        ),
      );
}
