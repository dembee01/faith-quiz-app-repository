# Faith Quiz Flutter migration

This worktree is the isolated branch `feature/flutter-migration`. The original
Jetpack Compose Android worktree remains unchanged.

## Completed migration slices

- Flutter Android and iOS project scaffolding with stable IDs:
  `com.example.faithquiz` on both platforms.
- Slate Indigo visual system, including the original splash, menu cards, level
  grid, journey map, timer HUD, quiz feedback, results dashboard, and grand
  completion screen.
- Shared Preferences-backed progress for unlocked levels, scores, daily and
  devotion streaks, adaptive level, achievements, topic scores, attempt/time
  tracking, theme, text scale, reduced motion, and crash-report consent.
- Full canonical level question catalog converted from the Kotlin source by
  `tool/convert_question_bank.js`.
- Full Gospel, Prophet, and Parable topic catalogs converted from
  `TopicQuestionBank.kt`, including the native supplemental-question behavior.
- Home, level selection, covenant journey, quiz, daily challenge, topic packs,
  review, leaderboard, settings, results, and grand-completion flows.
- Resumable classic, daily, speed, survival, and topic quiz sessions with stable
  seeds; detailed mistake history and spaced-review due scheduling.
- Answer confirmation before feedback: choosing an option only highlights it;
  `SUBMIT ANSWER` records it and then reveals the explanation. The timer HUD
  shows per-question and total level time and pauses after submission.
- Existing logo, journey, and leaderboard assets copied into Flutter assets.
- Android debug build verified at `FaithQuiz-flutter-debug.apk` (package
  `com.example.faithquiz`, version `1.0.0`, SHA-256
  `BDE69378C107D6C8B5ACE5FE0503C5B499D1ECFED95CF39711B658934E0630DB`).
- `flutter analyze` and `flutter test` pass with no issues. Widget tests cover
  splash/menu navigation, answer confirmation, feedback, and timer updates.

## Remaining parity work before release

The remaining release-readiness work is:

- Audio/haptic feedback parity. The native helper has no Flutter equivalent yet.
- Optional Sentry configuration after a production DSN is supplied; the consent
  toggle is already persisted and defaults off.
- Device screenshot and interaction parity checks on an Android emulator or
  physical Android device.
- iOS simulator/device build and signing on macOS/Xcode.

## Build commands

```bash
flutter pub get
flutter analyze
flutter test
flutter build apk --debug
```

For distribution, configure the same shared `keystore.properties` used by the
Android app and build a signed release. Never distribute the debug APK; it is
machine-locally signed and may trigger Play Protect warnings.
