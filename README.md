# RemindMe 📱

> A powerful, feature-rich reminder app built with **Kotlin** and **Jetpack Compose**. Stay organized with smart reminders, location-based notifications, AI assistance, and full account security.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-purple.svg)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com)
[![Version](https://img.shields.io/badge/Version-0.0.6-brightgreen.svg)](https://github.com/kareem2099/DotReminder)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📸 Screenshots

> Coming soon

---

## ✨ Features

### 🔔 Core Reminder Features
- **Time-based Reminders** — Set reminders for specific dates and times
- **Recurring Reminders** — Daily, weekly, monthly, and custom recurrence patterns
- **Location-based Reminders** — Get notified when you arrive at or leave specific locations
- **Smart Notifications** — Customizable notification sounds and priority levels
- **Snooze & Dismiss** — Flexible snooze durations and swipe-to-dismiss actions

### 🎨 Customization & Organization
- **Custom Categories** — Create personalized categories with custom colors and icons
- **Multiple Themes** — Ocean Blue, Forest Green, Sunset Orange, Midnight Purple, and Candy
- **Dark Mode** — Automatic and manual dark mode support
- **Reminder Templates** — Save frequently used reminder configurations for quick reuse

### 🤖 AI-Powered Features
- **AI Assistant Chat** — Natural language processing in Arabic & English powered by Gemini 2.5 Flash
- **Smart JSON Parsing** — Gemini extracts reminder title, exact time, and category automatically
- **Smart Suggestions** — AI-powered recommendations based on your reminder patterns

### 📱 Widgets & Quick Access
- **Quick Add Widget** (1×1) — Instant reminder creation from home screen
- **Upcoming Reminders Widget** (2×2) — View your next reminders with quick edit access

### 🔐 Security & Authentication
- **Multi-provider Authentication** — Email, Google (Android Credential Manager API), and Facebook sign-in
- **Biometric Authentication** — Fingerprint and face unlock support
- **Two-Factor Authentication (2FA)** — TOTP-based with backup codes
- **Active Session Management** — View and sign out devices remotely via real-time Firestore
- **Account Security Settings** — Full control over your account security

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Gradle: **9.5.0**
- AGP (Android Gradle Plugin): **9.3.1**
- Kotlin: **2.3.10**
- Minimum Android SDK: **API 29** (Android 10)
- Target Android SDK: **API 36** (Android 16)
- JDK 17 / 21

### Installation

**1. Clone the repository**
```bash
git clone https://github.com/kareem2099/DotReminder.git
cd DotReminder
```

**2. Configure Firebase**
- Create a project at [Firebase Console](https://console.firebase.google.com/)
- Add your Android app (`com.FreeRave.remindme`)
- Download `google-services.json` → place in `app/`
- Enable **Authentication** (Email, Google, Facebook) and **Firestore**

**3. Configure API Keys**

Create `local.properties` and add:
```properties
GEMINI_API_KEY=your_gemini_api_key
GOOGLE_MAPS_API_KEY=your_maps_api_key
LLM_API_URL=your_llm_url
LLM_MODEL_NAME=your_model_name
BANNER_AD_UNIT_ID=your_banner_id
INTERSTITIAL_AD_UNIT_ID=your_interstitial_id
APP_OPEN_AD_UNIT_ID=your_app_open_id
REWARDED_AD_UNIT_ID=your_rewarded_id
MYAPP_RELEASE_STORE_FILE=/path/to/your/keystore.keystore
MYAPP_RELEASE_STORE_PASSWORD=your_store_password
MYAPP_RELEASE_KEY_ALIAS=your_key_alias
MYAPP_RELEASE_KEY_PASSWORD=your_key_password
```

**4. Configure Firestore Rules**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      match /sessions/{sessionId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
    match /reminders/{reminderId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update, delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }
    match /categories/{categoryId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update, delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }
    match /reminder_templates/{templateId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update, delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }
  }
}
```

**5. Build and Run**
```bash
./gradlew build
./gradlew installDebug
```

---

## 🏗️ Architecture

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository Pattern |
| DI | Hilt (Dagger) + KSP |
| Database | Firebase Firestore |
| Auth | Firebase Auth + Google + Facebook |
| Local Storage | DataStore Preferences |
| Location | Google Play Services Location + Geofencing |
| AI | Google Gemini 2.5 Flash |
| Build | AGP 9.1 + KSP |

### Project Structure
```
app/src/main/java/com/FreeRave/remindme/
├── data/           # DataStore preferences & repositories
├── di/             # Hilt dependency injection modules
├── model/          # Data models (Reminder, User, Session, etc.)
├── navigation/     # NavGraph & Screen routes
├── screens/        # UI screens & ViewModels
│   ├── auth/       # Login, SignUp, VerifyEmail
│   ├── home/       # Home screen
│   ├── add/        # Add reminder
│   ├── edit/       # Edit reminder
│   ├── calendar/   # Calendar view
│   ├── category/   # Categories management
│   ├── templates/  # Reminder templates
│   ├── ai/         # AI chat assistant
│   ├── settings/   # Settings & security screens
│   └── support/    # Support screen
├── service/        # Business logic & Firebase services
├── ui/             # Shared components, themes, responsive utils
├── utils/          # Notifications, location, NLP utils
└── widget/         # Home screen widgets
```

---

## 📝 Changelog

See [CHANGELOG.md](CHANGELOG.md) for full version history.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'feat: add amazing feature'`
4. Push: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/kareem2099/DotReminder/issues)
- **Discussions**: [GitHub Discussions](https://github.com/kareem2099/DotReminder/discussions)

---

<p align="center">Built with ❤️ using modern Android development • <strong>Stay organized, stay reminded! 🎯</strong></p>