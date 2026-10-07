import 'package:flutter/material.dart';

import '../app_scope.dart';
import '../l10n/app_localizations.dart';

/// Changing my own password (opened from Profile). The app stays logged in; the server ends my website
/// sessions and other app logins.
class ChangePasswordScreen extends StatefulWidget {
  const ChangePasswordScreen({super.key});

  /// The same minimum as the server's.
  static const minLength = 8;

  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  final _form = GlobalKey<FormState>();
  final _current = TextEditingController();
  final _new = TextEditingController();
  final _confirm = TextEditingController();
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _current.dispose();
    _new.dispose();
    _confirm.dispose();
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
    final t = AppLocalizations.of(context);
    final messenger = ScaffoldMessenger.of(context);
    final navigator = Navigator.of(context);
    try {
      await AppScope.read(context).changePassword(_current.text, _new.text);
      messenger.showSnackBar(SnackBar(content: Text(t.passwordChanged)));
      navigator.pop();
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
      appBar: AppBar(title: Text(t.changePassword)),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _form,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextFormField(
                  key: const Key('currentPassword'),
                  controller: _current,
                  obscureText: true,
                  textInputAction: TextInputAction.next,
                  decoration: InputDecoration(labelText: t.currentPassword),
                  validator: (v) => (v ?? '').isEmpty ? t.required : null,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  key: const Key('newPassword'),
                  controller: _new,
                  obscureText: true,
                  textInputAction: TextInputAction.next,
                  decoration: InputDecoration(labelText: t.newPassword, helperText: t.changePasswordHelp, helperMaxLines: 3),
                  validator: (v) {
                    if ((v ?? '').isEmpty) {
                      return t.required;
                    }
                    return v!.length < ChangePasswordScreen.minLength ? t.passwordTooShort : null;
                  },
                ),
                const SizedBox(height: 12),
                TextFormField(
                  key: const Key('confirmNewPassword'),
                  controller: _confirm,
                  obscureText: true,
                  onFieldSubmitted: (_) => _submit(),
                  decoration: InputDecoration(labelText: t.confirmNewPassword),
                  validator: (v) => v != _new.text ? t.passwordsDontMatch : null,
                ),
                if (_error != null) ...[
                  const SizedBox(height: 16),
                  Text(_error!, key: const Key('changePasswordError'), style: TextStyle(color: Theme.of(context).colorScheme.error)),
                ],
                const SizedBox(height: 24),
                FilledButton(
                  key: const Key('savePassword'),
                  onPressed: _busy ? null : _submit,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    child: Text(t.changePassword),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
