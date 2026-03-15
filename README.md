# RemindMe 📱

A powerful, feature-rich reminder app built with Kotlin and Jetpack Compose for Android. Stay organized with smart reminders, location-based notifications, AI assistance, and customizable templates.

## ✨ Features

### 🔔 Core Reminder Features
- **Time-based Reminders** - Set reminders for specific dates and times
- **Recurring Reminders** - Daily, weekly, monthly, and custom recurrence patterns
- **Location-based Reminders** - Get notified when you arrive at or leave specific locations
- **Smart Notifications** - Customizable notification sounds and priority levels

### 🎨 Customization & Organization
- **Custom Categories** - Create personalized categories with custom colors and icons
- **Multiple Themes** - Choose from various themes including Ocean Blue, Forest Green, Sunset Orange, Midnight Purple, and Candy themes
- **Reminder Templates** - Save frequently used reminder configurations for quick reuse

### 🤖 AI-Powered Features
- **AI Assistant Chat** - Natural language processing for creating and managing reminders
- **Smart Suggestions** - AI-powered recommendations based on your reminder patterns
- **Voice Commands** - Hands-free reminder creation and management

### 📱 Widgets & Quick Access
- **Quick Add Widget** - 1x1 widget for instant reminder creation
- **Upcoming Reminders Widget** - 2x2 widget showing your next reminders with quick edit access
- **Home Screen Integration** - Seamless integration with Android home screen

### 🔐 Security & Authentication
- **Multi-provider Authentication** - Sign in with Email, Google, or Facebook
- **Biometric Authentication** - Fingerprint and face unlock support
- **Account Security** - Active session management and security settings

### 🌟 Additional Features
- **Search & Filter** - Quickly find reminders with powerful search and category filtering
- **Pull-to-Refresh** - Stay up-to-date with manual refresh capability
- **Swipe Actions** - Quick delete and edit with intuitive swipe gestures
- **Backup & Sync** - Cloud synchronization across devices
- **Dark Mode Support** - Automatic and manual dark mode options

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Minimum Android SDK: API 24 (Android 7.0)
- Target Android SDK: API 34 (Android 14)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/RemindMe.git
   cd RemindMe
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned directory and select it

3. **Configure Firebase**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Add your Android app to the project
   - Download `google-services.json` and place it in `app/src/main/`
   - Enable Authentication and Firestore in Firebase Console

4. **Configure API Keys**
   - Add your Facebook App ID and Client Token to `strings.xml`
   - Configure Google Gemini API key for AI features
   - Set up AdMob for monetization (optional)

5. **Build and Run**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

## 📱 Usage

### Creating Reminders
1. Tap the **"Add Reminder"** button on the home screen
2. Fill in reminder details:
   - Title and description
   - Date and time
   - Category selection
   - Recurrence options
   - Location settings (optional)
3. Save your reminder

### Using Templates
1. Access templates from the **"Use Template"** button in Add Reminder
2. Browse your saved templates
3. Select a template to pre-fill reminder fields
4. Customize as needed and save

### Managing Categories
1. Go to **Menu → Categories** from the home screen
2. Create new categories with custom colors and icons
3. Edit or delete existing categories
4. Categories appear in reminder filters and creation

### Widgets Setup
1. Long-press on your home screen
2. Select **"Widgets"**
3. Find **"RemindMe"** widgets
4. Drag widgets to your home screen:
   - **Quick Add Widget**: Instant reminder creation
   - **Upcoming Reminders Widget**: View and edit next reminders

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt (Dagger)
- **Database**: Firebase Firestore
- **Authentication**: Firebase Auth + Social providers
- **Background Tasks**: WorkManager
- **Location Services**: Google Play Services Location

### Project Structure
```
app/src/main/java/com/FreeRave/remindme/
├── data/           # Data layer (repositories, preferences)
├── di/             # Dependency injection modules
├── model/          # Data models
├── navigation/     # Navigation components
├── screens/        # UI screens and ViewModels
├── service/        # Business logic services
├── ui/             # UI components and themes
├── utils/          # Utility classes
└── widget/         # Android widgets
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Run tests: `./gradlew test`
5. Submit a pull request

## 📝 Changelog

See [CHANGELOG.md](CHANGELOG.md) for a detailed list of changes and version history.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with ❤️ using modern Android development practices
- Special thanks to the Android and Kotlin communities
- Icons and assets from Material Design Icons

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/RemindMe/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/RemindMe/discussions)
- **Email**: support@remindme.app

---

**Stay organized, stay reminded!** 🎯
