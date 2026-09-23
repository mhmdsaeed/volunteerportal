import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import 'api/api_client.dart';
import 'api/models.dart';
import 'services/location_service.dart';
import 'services/session_store.dart';

/// App-wide state: which server, who is logged in, and the language.
class AppState extends ChangeNotifier {
  AppState({
    required this.store,
    this.location = const GeolocatorLocationService(),
    ApiClient Function(String baseUrl)? clientFactory,
  }) : _clientFactory = clientFactory ?? ((url) => ApiClient(baseUrl: url, httpClient: http.Client()));

  final SessionStore store;
  final LocationService location;
  final ApiClient Function(String baseUrl) _clientFactory;

  ApiClient? _api;
  Me? _me;
  String _language = 'en';
  String? _serverUrl;
  bool _ready = false;
  bool _sessionExpired = false;

  bool get ready => _ready;
  bool get loggedIn => _api?.token != null;
  Me? get me => _me;
  String get language => _language;
  String? get serverUrl => _serverUrl;

  /// True once after a 401, so the login screen can say why the user was logged out.
  bool get sessionExpired => _sessionExpired;

  ApiClient get api {
    final api = _api;
    if (api == null) {
      throw StateError('Not logged in');
    }
    return api;
  }

  /// Restores the saved server, token and language, if any.
  Future<void> load() async {
    _language = await store.read(SessionStore.language) ?? _language;
    _serverUrl = await store.read(SessionStore.serverUrl);
    final token = await store.read(SessionStore.token);
    if (_serverUrl != null && token != null) {
      _api = _clientFactory(_serverUrl!)
        ..token = token
        ..language = _language;
    }
    _ready = true;
    notifyListeners();
    if (loggedIn) {
      await refreshMe();
    }
  }

  Future<void> login(String serverUrl, String username, String password) async {
    final api = _clientFactory(serverUrl)..language = _language;
    final result = await api.login(username.trim(), password, deviceName: 'Volunteer Portal app');
    api.token = result.token;
    _api = api;
    _me = result.user;
    _serverUrl = api.baseUrl;
    _sessionExpired = false;
    await store.write(SessionStore.serverUrl, api.baseUrl);
    await store.write(SessionStore.token, result.token);
    notifyListeners();
  }

  Future<void> logout() async {
    try {
      await _api?.logout();
    } on ApiException {
      // Logging out locally matters most; the token also expires on its own
    }
    await _clearSession();
  }

  Future<void> refreshMe() => guard(() async {
        _me = await api.me();
        notifyListeners();
      });

  Future<void> setLanguage(String language) async {
    _language = language;
    _api?.language = language;
    await store.write(SessionStore.language, language);
    notifyListeners();
  }

  /// Runs an API call; if the token is no longer valid, logs out (the login screen then shows).
  Future<T> guard<T>(Future<T> Function() call) async {
    try {
      return await call();
    } on UnauthorizedException {
      _sessionExpired = true;
      await _clearSession();
      rethrow;
    }
  }

  Future<void> _clearSession() async {
    _api = null;
    _me = null;
    await store.write(SessionStore.token, null);
    notifyListeners();
  }
}
