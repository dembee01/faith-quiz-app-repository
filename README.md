# Faith Quiz

Faith Quiz is an offline-first Android Bible quiz built with Kotlin and Jetpack Compose. It includes 30 progressive levels, focused topic packs, a deterministic daily challenge, saved sessions, adaptive level recommendations, and spaced review of missed questions.

## Highlights

- One versioned, canonical question catalog for the UI and Room database
- Four distinct choices and validated answer indexes for every question
- Source metadata shown with each explanation; wording is marked translation-independent unless a question explicitly supplies another label
- A stable daily question that changes once per calendar day
- Spaced review scheduling for missed questions
- Light/dark themes, large text, reduced motion, and screen-reader labels
- Optional Sentry crash reporting that is disabled by default and requires user consent
- Debug unit tests, Android instrumentation-test sources, Android lint, and GitHub Actions checks

## Technology

- Android 15 target (API 35), minimum Android 7.0 (API 24)
- Kotlin, Jetpack Compose, Material 3, Navigation Compose
- Hilt dependency injection
- Room and Preferences DataStore
- Gradle 8.9 and Android Gradle Plugin 8.7.3

## Development checks

These checks do not package an APK:

```bash
./gradlew testDebugUnitTest compileDebugAndroidTestKotlin lintDebug compileReleaseKotlin
```

Device tests require an Android emulator or physical device:

```bash
./gradlew connectedDebugAndroidTest
```

## Building on the release machine

Debug APK:

```bash
./gradlew assembleDebug
```

For a signed release, create an untracked `keystore.properties` file in the repository root:

```properties
storeFile=path/to/release-keystore.jks
storeType=PKCS12
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

Then run:

```bash
./gradlew assembleRelease
```

Release minification and resource shrinking are enabled. When signing properties are absent, Gradle can still compile release sources but will not attach a release signing configuration.

## Optional crash reporting

No crash data is sent by default. To make the opt-in setting available, provide a Sentry DSN only on the build machine:

```bash
./gradlew assembleRelease -PSENTRY_DSN="https://public-key@host/project-id"
```

Users must still enable **Settings → Crash reporting**. Personally identifiable information and performance tracing are disabled in the SDK configuration.

## Content maintenance

`QuestionBank` is the authoritative level catalog. `TopicQuestionBank` owns topic-only material and supplements from that catalog. Increment `QuestionContent.VERSION` whenever bundled wording, answers, or source metadata changes; the Room projection will then refresh transactionally without deleting player progress.

Before publishing content changes, run the tests. They verify question structure, distinct options, valid answer indexes, source/translation metadata, known corrected answers, topic counts, canonical database projection, and daily-question rotation.
