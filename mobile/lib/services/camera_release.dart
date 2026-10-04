// Turns the camera off when the scanner goes away. Only the web needs this (see camera_release_web.dart);
// on Android and iPhone mobile_scanner releases the camera itself.
export 'camera_release_stub.dart' if (dart.library.js_interop) 'camera_release_web.dart';
