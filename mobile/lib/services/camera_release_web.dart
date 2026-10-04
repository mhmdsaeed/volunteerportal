import 'dart:js_interop';
import 'dart:js_interop_unsafe';

import 'package:web/web.dart' as web;

/// On the web, mobile_scanner's stop() only drops its reference to the camera stream and leaves it running
/// (camera light on, battery drain), and its video element isn't in the page, so it can't be found later.
/// So remember every stream the browser hands out for the camera, and stop them when the scanner goes away.
const _trackStreams = '''
(function () {
  if (window.__vpStopCameras || !navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) return;
  var streams = [];
  var getUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
  navigator.mediaDevices.getUserMedia = function (constraints) {
    return getUserMedia(constraints).then(function (stream) { streams.push(stream); return stream; });
  };
  window.__vpStopCameras = function () {
    streams.forEach(function (stream) { stream.getTracks().forEach(function (track) { track.stop(); }); });
    streams = [];
  };
})();
''';

/// Starts remembering camera streams; call before the scanner asks for the camera. Safe to call again.
void trackCameraStreams() {
  if (globalContext.has('__vpStopCameras')) {
    return;
  }
  final script = web.HTMLScriptElement()..text = _trackStreams;
  web.document.head?.append(script);
}

/// Turns the camera off: stops every camera stream handed out since [trackCameraStreams].
void releaseCamera() {
  if (globalContext.has('__vpStopCameras')) {
    globalContext.callMethod('__vpStopCameras'.toJS);
  }
}
