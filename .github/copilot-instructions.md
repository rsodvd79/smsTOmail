# smsTOmail Copilot Instructions

## Build, test, and lint commands

Use the Gradle wrapper from the repository root. Prefer module-scoped tasks on `:app`.

- Build debug APK: `./gradlew app:assembleDebug` (`gradlew.bat` on Windows)
- Full app build/check cycle: `./gradlew app:build`
- Unit tests: `./gradlew app:testDebugUnitTest`
- Single unit test class: `./gradlew app:testDebugUnitTest --tests "it.drhack.smstomail.ExampleUnitTest"`
- Single unit test method: `./gradlew app:testDebugUnitTest --tests "it.drhack.smstomail.ExampleUnitTest.addition_isCorrect"`
- Instrumented tests on a connected device/emulator: `./gradlew app:connectedDebugAndroidTest`
- Single instrumented test class: `./gradlew app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=it.drhack.smstomail.ExampleInstrumentedTest`
- Lint: `./gradlew app:lintDebug`
- Clean: `./gradlew app:clean`

The app targets Java 11 (`sourceCompatibility`/`targetCompatibility` and Kotlin JVM target are 11), so use JDK 11+ for Gradle. The checked-in wrapper points to Gradle 9.3.1; trust `gradle/wrapper/gradle-wrapper.properties`. `gradle.properties` contains several deprecated AGP properties (e.g. `android.newDsl=false`, `android.builtInKotlin=false`) that produce warnings on every build — they will be removed in AGP 10.0, so do not add new properties in the same style.

## High-level architecture

This is a single-module Android app (`:app`) that forwards incoming SMS messages to email. The entry points and responsibilities are split across broadcast receivers, a foreground service, Room, and Compose activities rather than a separate domain/viewmodel layer.

- `SmsReceiver` is the real SMS ingress point. It receives `Telephony.Sms.Intents.SMS_RECEIVED_ACTION`, reconstructs multipart SMS bodies, loads filters and email config from Room, applies `SmsFilterProcessor`, sends email through `EmailSender`, and persists the outcome in `sms_log`.
- `EmailSender` wraps JavaMail SMTP delivery. It supports direct SSL on port 465 and STARTTLS-style setups on other ports, and surfaces Gmail-specific auth guidance when Google rejects normal account passwords.
- `AppDatabase` is the persistence hub. It stores filters, a single-row email configuration, and SMS forwarding history. Passwords are encrypted through `EncryptedStringConverter`/`CryptoManager`, which uses Android Keystore-backed AES/GCM.
- `MainActivity` is mostly an orchestration/activity shell: it gates startup on runtime permissions, forces first-run email configuration, loads SMS history from Room, and exposes navigation to email config and filter management.
- `EmailConfigActivity` and `FilterActivity` are Compose screens that read/write Room directly. There is no repository or ViewModel abstraction in between.
- `BootReceiver` and `SmsBackgroundService` exist for boot/background handling, but SMS forwarding logic still lives in `SmsReceiver`. `SmsBackgroundService` only processes an SMS when `sender` and `message` extras are present; otherwise it stops quickly.
- `NotificationHelper` is a separate singleton that fires a high-priority auth-error notification when Gmail credentials are rejected. It is independent of the foreground service notification managed inside `SmsBackgroundService`.

## Key conventions

- `email_config` is treated as a singleton row with `id = 0`. When changing config persistence, preserve that assumption in `EmailConfig`, `EmailConfigDao`, and startup code in `MainActivity`.
- Room schema changes are handled manually. If you change an entity, also update `AppDatabase` version/migrations and keep the exported schema JSON in `app/schemas/it.drhack.smstomail.AppDatabase/` in sync.
- Sensitive email credentials are encrypted at the Room converter layer, not manually in activities or DAOs. Reuse `EncryptedStringConverter`/`CryptoManager` instead of introducing a second encryption path. The `EncryptedValue` wrapper type exists solely to give Room an unambiguous distinct type for the converter; always wrap plaintext passwords in `EncryptedValue` before storing and unwrap after retrieval.
- Room TypeConverters encode `FilterType` as `Int` (INCLUDE=0, EXCLUDE=1) and `Date` as `Long` epoch millis. If you add new enum or custom fields to entities, add a corresponding `TypeConverter` class and register it in `@TypeConverters` on `AppDatabase`.
- Filter behavior is intentionally permissive by default: with no filters configured, every SMS is forwarded. Include/exclude evaluation is centralized in `SmsFilterProcessor`; change filtering rules there rather than duplicating logic in receivers or UI.
- SMS log retention is enforced inside `SmsLogDao.insert()`, not in UI code. The `maxSmsToKeep` setting from `EmailConfig` controls pruning after each insert.
- Italian is the source locale (`res/values/strings.xml`) and English lives in `res/values-en/strings.xml`. When changing user-facing strings, update both locales when the text is already localized.
- The project mixes resource-based localization with some still-hardcoded Italian UI text, especially in `MainActivity`. Before adding new literals, check whether the surrounding screen already uses string resources and keep that style consistent within the file you touch.
- Theming: `Theme.SmsTOmail` in `res/values/themes.xml` extends `Material.Light.NoActionBar`. All screens then apply Compose-level theming via `SmsTOmailTheme` (in `ui/theme/`). Do not add XML-based view layouts; all UI is Jetpack Compose.
