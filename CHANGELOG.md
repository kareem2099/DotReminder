# Changelog

All notable changes to **DotReminder** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.7] - 2026-11-04

### Added
- An authenticated DotSuite AI gateway with Firebase token verification, account/network usage limits, bounded payloads and provider deadlines.
- Firebase-backed authenticator enrollment, reauthentication and TOTP challenges in account security.
- A biometric app lock tied to the signed-in account and foreground lifecycle.
- Firebase SMS verification and recipient-bound resend/cooldown handling during registration.
- In-app privacy links, a support email entry (`support@dotsuite.dev`) and an explicit account/associated-data deletion request explanation.
- Device-local reminder alarm records and restoration after reboot, application update and clock/access changes.
- Focused unit, isolated Compose UI, reminder-device and emulator-rule review coverage.

### Changed
- Rebuilt the app screens around shared glass surfaces, responsive forms and reusable UI components.
- Updated Settings to show the actual build version **0.0.7 (8)**.
- Retained **compileSdk 36 / targetSdk 36 (Android 16)** and **minSdk 29 (Android 10)**; version code increased from 7 to 8 for the new bundle.
- Account deletion is an email-request workflow after ownership verification, with a 30-day processing commitment. The separate DotSuite website provides the policy and request page in five languages.
- Bounded authentication network calls, cancellation-aware controllers and non-blocking session metadata writes.

### Fixed
- Notification permission handling and exact reminder timing, including recurrence, snooze and stable sound-channel behavior.
- Recurring reminder dates/counts appearing overdue after the first occurrence.
- AI chat keyboard spacing, conversation continuity and review-before-save reminder drafts.
- Registration retries, late phone verification callbacks and cross-account handling.
- App-lock policy reload/retry behavior and sensitive-dialog state across foreground transitions.
- Missing owner-only Firestore session subcollection rules in the checked-in rule source.

### Removed
- The Gemini API key field from Android BuildConfig and the direct Gemini client SDK; provider credentials now stay in the server environment.

### Release notes
- The Settings deletion action opens a request pathway; it does not automatically erase a Firebase account when tapped.
- Website publishing, production Firestore rule state, provider app-signing validation and Play review remain separate deployment checks.
- See `docs/protected-ai-gateway.md` and `docs/auth-release-review.md` for gateway deployment and production checks.

---

## [0.0.6] - 2026-07-31

### Added
- **Android 16 Support (API 36)** — Target SDK updated to API 36 to meet Google Play Console requirements.
- **Android Credential Manager API** — Full migration of Google Authentication to `androidx.credentials:credentials` & `googleid:1.1.1`.
- **Single Ticking Clock Source** — Unified `currentTime` ticker (`produceState` 30s) in `HomeScreen` ensuring zero clock drift between top stat cards and card status badges.

### Changed & Redesigned
- **Home Screen UI Redesign** —
  - `WelcomeSection` upgraded to modern glassmorphic `Surface` cards with 16dp rounded corners.
  - `PremiumReminderCard` redesigned with dark surface container, vertical status indicator bar (Red = Overdue, Green = Complete, Primary = Upcoming), and high-contrast text.
  - FAB layout upgraded to `ExtendedFloatingActionButton` paired with AI Assistant `SmallFloatingActionButton` and 88dp bottom content padding.
### Fixed & Cleaned
- **Deprecation Warnings Cleanup** — Completely removed legacy `GoogleSignInClient` / `GoogleSignIn` SDK references and imports across `GoogleAuthServiceImpl`, `GoogleSignInOnlyServiceImpl`, and `FirebaseAuthServiceImpl`.
- **Compiler Flags** — Added `-Xannotation-default-target=param-property` to Kotlin `freeCompilerArgs` in `app/build.gradle.kts` to eliminate parameter-property annotation warnings.
- **MapLocationPickerDialog** — Fixed redundant `.toDouble()` conversions on radius calculations.
- **Dead Code Removal** — Deleted unused legacy `ReminderItem` composable from `HomeScreen.kt`.

### Technical & Build Environment
- **Gradle Version** — 9.5.0 Wrapper
- **AGP (Android Gradle Plugin)** — 9.3.1
- **Kotlin Version** — 2.3.10
- **Build Target** — Target SDK 36 (Android 16), Min SDK 29 (Android 10), Java 17/21 compatibility

---

## [0.0.5] - 2026-03-16

### Added
- **Gemini 2.5 Flash** — Upgraded AI model from `gemini-2.0-flash` to faster, smarter `gemini-2.5-flash`
- **`parseReminderToJSON()`** — New Gemini-powered JSON extractor supporting Arabic and English, returning exact reminder time in milliseconds with auto-inferred category
- **OceanBlueBackground** — Pure Compose gradient backgrounds applied across all screens

### Removed
- **VideoBackgroundPlayer** — Removed ExoPlayer-based video backgrounds
- **ScreenBackground / CloudinaryConfig** — Removed entire asset-based background system
- **74MB video assets** — Deleted `assets/backgrounds/` folder
- **Media3 ExoPlayer dependencies** — Removed `media3-exoplayer`, `media3-ui`, `media3-common`

### Fixed
- **Scaffold `containerColor`** — Set to `Color.Transparent` across all screens
- **Deprecated `hiltViewModel` imports** — Updated all screens to `androidx.hilt.navigation.compose`
- **AppNavGraph** — Refactored to flat structure removing redundant background wrappers

### Enhanced
- **AiChatScreen** — Revamped UI with FAB send button, WhatsApp-style chat bubbles, LinearProgressIndicator
- **AiViewModel** — Integrated `parseReminderToJSON` replacing `NLPUtils`, added JSON Markdown cleanup and null-safe field extraction
- **ResponsiveDesignDemo** — Cleaned up unused references

---

## [0.0.4] - 2026-03-16

### Added
- **Real Active Sessions** — Replaced mock sessions with live Firestore-backed session tracking
  - Sessions stored under `users/{userId}/sessions/{sessionId}`
  - Current device highlighted with badge
  - Sign out individual devices or all other devices at once
- **Session Model** — New `Session.kt` with `deviceName`, `deviceModel`, `loginTime`, `lastActive`
- **SessionRepository** — Full CRUD with real-time Firestore Flow
- **ActiveSessionsViewModel** — Proper state management for sessions screen
- **Current Session ID** — Persisted in DataStore, saved on every login (email, Google, Facebook)

### Fixed
- **Biometric `FragmentActivity not found`** — `MainActivity` now inherits from `AppCompatActivity`
- **AccountSecurityScreen crash** — Removed nested scroll conflict
- **CategoriesScreen crash** — Replaced nested `LazyColumn` inside `AdaptiveColumn`
- **Deprecated `hiltViewModel` import** — Updated all occurrences
- **Firestore Rules** — Added missing `sessions` subcollection permission

### Technical
- **Migrated to AGP 9.1** — Removed deprecated plugins
- **Migrated from `kapt` to `KSP`**
- **AppCompat added** — `androidx.appcompat:appcompat:1.7.0`
- **compileSdk** updated to 36, **Java** to 17
- **Clean GitHub repository** at `https://github.com/kareem2099/DotReminder.git`

---

## [0.0.3] - 2025-11-05

### Added
- **Custom Categories** — Create, edit, and delete categories with custom colors and icons
- **Reminder Templates** — Save and reuse frequently used reminder configurations
- **Android Widgets** — Quick Add (1×1) and Upcoming Reminders (2×2)
- **Widget Deep Linking** — Seamless integration with app navigation

### Technical
- New `ReminderTemplate` data model
- `TemplateService` for template CRUD
- `AppWidgetProvider` classes with Hilt
- Extended navigation routing

---

## [0.0.2] - 2025-11-03

### Added
- **AI Assistant** — Google Gemini-powered chat for natural language reminder creation
- **Advanced Recurrence** — Weekly, monthly, custom interval patterns
- **Location-based Reminders** — Geofencing with Google Maps
- **Multi-theme Support** — Ocean Blue, Forest Green, Sunset Orange, Midnight Purple, Candy
- **Biometric Authentication** — Fingerprint and face unlock
- **Social Authentication** — Google and Facebook sign-in
- **Two-Factor Authentication (2FA)** — TOTP-based with backup codes
- **Dark Mode**, **Search & Filter**, **Swipe Actions**, **Pull-to-Refresh**, **Cloud Sync**

### Technical
- Jetpack Compose + Material Design 3
- MVVM + Repository pattern
- Hilt, Firebase Auth, Firestore, WorkManager, DataStore

---

## [0.0.1] - 2025-11-01

### Added
- Initial release — Basic reminder creation, Firebase Auth, Firestore sync, Material Design 3 UI

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/AmazingFeature`
3. Commit: `git commit -m 'feat: add AmazingFeature'`
4. Push: `git push origin feature/AmazingFeature`
5. Open a Pull Request