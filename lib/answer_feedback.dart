import 'package:flutter/services.dart';

/// Gives each submitted answer an immediate, accessible sensory response.
/// Android uses the original app's tones and vibration patterns through the
/// native bridge; other platforms use Flutter's system feedback APIs.
class AnswerFeedback {
  AnswerFeedback._();

  static const _channel = MethodChannel(
    'com.example.faithquiz/answer_feedback',
  );

  static Future<void> correct() => _play('correct');
  static Future<void> incorrect() => _play('incorrect');

  static Future<void> _play(String result) async {
    try {
      await _channel.invokeMethod<void>('play', <String, String>{
        'result': result,
      });
    } on MissingPluginException {
      await _fallback(result);
    } on PlatformException {
      await _fallback(result);
    }
  }

  static Future<void> _fallback(String result) async {
    if (result == 'correct') {
      await HapticFeedback.mediumImpact();
      await SystemSound.play(SystemSoundType.click);
    } else {
      await HapticFeedback.heavyImpact();
      await SystemSound.play(SystemSoundType.alert);
    }
  }
}
