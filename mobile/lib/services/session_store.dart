import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// What the app remembers between launches: the server address, the login token and the language.
abstract class SessionStore {
  Future<String?> read(String key);
  Future<void> write(String key, String? value);

  static const serverUrl = 'serverUrl';
  static const token = 'token';
  static const language = 'language';
}

/// Keeps the token in the platform's secure storage (Keychain on iPhone, Keystore-backed on Android).
class SecureSessionStore implements SessionStore {
  const SecureSessionStore([this._storage = const FlutterSecureStorage()]);

  final FlutterSecureStorage _storage;

  @override
  Future<String?> read(String key) => _storage.read(key: key);

  @override
  Future<void> write(String key, String? value) =>
      value == null ? _storage.delete(key: key) : _storage.write(key: key, value: value);
}

/// For tests.
class MemorySessionStore implements SessionStore {
  MemorySessionStore([Map<String, String>? initial]) : values = {...?initial};

  final Map<String, String> values;

  @override
  Future<String?> read(String key) async => values[key];

  @override
  Future<void> write(String key, String? value) async {
    if (value == null) {
      values.remove(key);
    } else {
      values[key] = value;
    }
  }
}
