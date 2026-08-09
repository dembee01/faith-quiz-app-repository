# Faith Quiz Flutter migration

This worktree is the isolated branch `feature/flutter-migration`. The original
Jetpack Compose Android worktree remains unchanged.

## Completed migration slices

- Flutter Android and iOS project scaffolding with stable IDs:
  `com.example.faithquiz` on both platforms.
- Slate/Indigo Material 3 theme with dark/light settings.
- Shared Preferences-backed progress for unlocked levels, scores, daily and
  devotion streaks, adaptive level, achievements, topic scores, attempt/time
  tracking, theme, text scale, reduced motion, and crash-report consent.
- Full canonical level question catalog converted from the Kotlin source by
  `tool/convert_question_bank.js`.
- Full Gospel, Prophet, and Parable topic catalogs converted from
  `TopicQuestionBank.kt`, including the native supplemental-question behavior.
- Home, level selection, covenant journey, quiz, daily challenge, topic packs,
  review, leaderboard, settings, and results flows.
- Resumable classic, daily, speed, survival, and topic quiz sessions with stable
  seeds; detailed mistake history and spaced-review due scheduling.
- Existing logo, journey, and leaderboard assets copied into Flutter assets.
- Android debug build verified at `FaithQuiz-flutter-debug.apk` (package
  `com.example.faithquiz`, version `1.0.0`, SHA-256
  `AC5A0996AE646FBC8BEE6588119E14937B5414806A05388345E309D7172964E9`).
- `flutter analyze` and `flutter test` pass with no issues.

## Remaining parity work before release

The remaining release-readiness work is:

- Audio helper and reduced-motion behavior across every animation.
- Optional Sentry configuration after a production DSN is supplied; the consent
  toggle is already persisted and defaults off.
- Screenshot and interaction parity for every redesigned screen.
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
