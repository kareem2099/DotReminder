# Changelog

All notable changes to **RemindMe** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.0.6] - Unreleased

### Planned
- **Real Change Password** — Replace mock implementation with Firebase re-authentication and actual password update
- **Complete Calendar Feature** — Full integration of HorizontalCalendar library with reminder display
- **AI Parsing in AddReminderViewModel** — Integrate `parseReminderToJSON` with offline NLPUtils fallback
- **Unit Tests** — Add unit tests for ViewModels and service layer
- **Two-pane Layouts** — Enhanced tablet experience with side-by-side panels

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