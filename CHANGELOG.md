# Changelog

All notable changes to **RemindMe** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.0.4] - Unreleased

### Planned
- **Real Change Password** — Replace mock implementation with Firebase re-authentication and actual password update
- **Complete Calendar Feature** — Full integration of HorizontalCalendar library with reminder display
- **Responsive Design** — Apply AdaptiveContainer pattern to EditReminderScreen, TemplatesScreen, AiChatScreen, SupportScreen
- **Unit Tests** — Add unit tests for ViewModels and service layer
- **Two-pane Layouts** — Enhanced tablet experience with side-by-side panels

---

## [0.0.3] - 2026-03-16

### Added
- **Real Active Sessions** — Replaced mock sessions with live Firestore-backed session tracking
  - Sessions stored under `users/{userId}/sessions/{sessionId}`
  - Current device highlighted with badge
  - Sign out individual devices or all other devices at once
- **Session Model** — New `Session.kt` data model with `deviceName`, `deviceModel`, `loginTime`, `lastActive`
- **SessionRepository** — Full CRUD with real-time Firestore Flow
- **ActiveSessionsViewModel** — Proper state management for sessions screen
- **Current Session ID** — Persisted in DataStore, saved on every login (email, Google, Facebook)

### Fixed
- **Biometric `FragmentActivity not found`** — Root cause: `MainActivity` inherited from `ComponentActivity` which does not extend `FragmentActivity`. Fixed by changing to `AppCompatActivity`
- **AccountSecurityScreen crash** — Removed nested scroll conflict (`verticalScroll` inside `AdaptiveColumn` inside `AdaptiveFormContainer`). Replaced with single `Column` + `verticalScroll` inside `AdaptiveContainer`
- **CategoriesScreen crash** — Removed `LazyColumn` inside `AdaptiveColumn` with `verticalScroll`. Replaced with `LazyColumn` directly inside `AdaptiveContainer`
- **Deprecated `hiltViewModel` import** — Updated all occurrences from `androidx.hilt.lifecycle.viewmodel.compose` to `androidx.hilt.navigation.compose`
- **Firestore Rules** — Added missing `sessions` subcollection permission under `users/{userId}`

### Technical
- **Migrated to AGP 9.0.1** — Removed `kotlin-android`, `kotlin-compose`, `kotlin-kapt` plugins (built-in since AGP 9.0)
- **Migrated from `kapt` to `KSP`** — Better performance and AGP 9.0 compatibility
- **AppCompat dependency added** — `androidx.appcompat:appcompat:1.7.0`
- **KSP updated** to `2.3.1` (first version compatible with AGP 9.0)
- **compileSdk** updated to 36
- **Java version** updated to 17
- **Clean GitHub repository** initialized at `https://github.com/kareem2099/DotReminder.git`

---

## [0.0.2] - 2025-11-05

### Added
- **Custom Categories** — Create, edit, and delete categories with custom colors and icons
- **Reminder Templates** — Save and reuse frequently used reminder configurations
- **Android Widgets**
  - Quick Add Widget (1×1) — Instant reminder creation from home screen
  - Upcoming Reminders Widget (2×2) — View and edit next 3 reminders
- **Template Management Screen** — Dedicated interface for creating and organizing templates
- **Widget Deep Linking** — Widgets integrate seamlessly with app navigation

### Enhanced
- **Category System** — Improved filtering to include both predefined and custom categories
- **Add Reminder Flow** — Added "Use Template" and "Save as Template" options
- **Home Screen Menu** — Added Categories management option
- **MainActivity** — Enhanced intent handling for widget deep linking

### Technical
- New `ReminderTemplate` data model
- `TemplateService` for template CRUD operations
- `AppWidgetProvider` classes with Hilt dependency injection
- Extended navigation routing for templates and categories

---

## [0.0.1] - 2025-11-01

### Added
- **AI Assistant** — Chat interface powered by Google Gemini for natural language reminder creation
- **Advanced Recurrence** — Weekly, monthly, and custom interval patterns
- **Location-based Reminders** — Geofencing with Google Maps integration
- **Multi-theme Support** — Ocean Blue, Forest Green, Sunset Orange, Midnight Purple, Candy
- **Biometric Authentication** — Fingerprint and face unlock
- **Social Authentication** — Google and Facebook sign-in
- **Two-Factor Authentication (2FA)** — TOTP-based with backup codes
- **Active Sessions UI** — View active login sessions (mock data, replaced in v0.0.3)
- **Dark Mode** — Automatic and manual dark mode
- **Notification System** — Custom sounds and priority levels
- **Search & Filter** — Search reminders with category filtering
- **Swipe Actions** — Swipe-to-delete and swipe-to-edit gestures
- **Pull-to-Refresh** — Manual data refresh
- **Cloud Sync** — Real-time synchronization via Firebase Firestore

### Technical
- Full Jetpack Compose + Material Design 3 UI
- MVVM architecture with Repository pattern
- Hilt dependency injection
- Firebase Auth, Firestore, Cloud Messaging
- WorkManager for background tasks
- DataStore Preferences for local storage

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/AmazingFeature`
3. Commit: `git commit -m 'feat: add AmazingFeature'`
4. Push: `git push origin feature/AmazingFeature`
5. Open a Pull Request

See [CONTRIBUTING.md](CONTRIBUTING.md) for full guidelines.