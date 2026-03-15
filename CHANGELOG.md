# Changelog

All notable changes to **RemindMe** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.2] - 2025-11-05

### Added
- **Custom Categories** - Users can now create, edit, and delete custom categories with personalized colors and icons
- **Reminder Templates** - Save frequently used reminder configurations for quick reuse
- **Android Widgets** - Two new home screen widgets for enhanced user experience:
  - Quick Add Widget (1x1) - Instant reminder creation
  - Upcoming Reminders Widget (2x2) - View and edit next 3 reminders
- **Template Management Screen** - Dedicated interface for creating, editing, and organizing reminder templates
- **Widget Deep Linking** - Widgets integrate seamlessly with app navigation for instant access to features

### Enhanced
- **Category System** - Improved category filtering to include both predefined and custom categories
- **Add Reminder Flow** - Added "Use Template" and "Save as Template" options
- **Home Screen Menu** - Added Categories management option
- **MainActivity** - Enhanced intent handling for widget deep linking

### Technical Improvements
- **New Data Models** - Added `ReminderTemplate` model for template functionality
- **Service Layer** - Created `TemplateService` for template CRUD operations
- **Widget Architecture** - Implemented `AppWidgetProvider` classes with dependency injection
- **Navigation System** - Extended routing to support templates and categories screens

## [0.0.1] - 2025-11-01

### Added
- **AI Assistant Integration** - Chat interface powered by Google Gemini for natural language reminder creation
- **Advanced Recurrence Patterns** - Support for complex recurring reminders (weekly, monthly, custom intervals)
- **Location-based Reminders** - Geofencing capabilities with Google Maps integration
- **Multi-theme Support** - Ocean Blue, Forest Green, Sunset Orange, Midnight Purple, and Candy themes
- **Biometric Authentication** - Fingerprint and face unlock support
- **Social Authentication** - Sign in with Google and Facebook
- **Active Sessions Management** - View and revoke active login sessions

### Enhanced
- **User Interface** - Complete redesign using Jetpack Compose with Material Design 3
- **Notification System** - Enhanced with custom sounds and priority levels
- **Search Functionality** - Improved search with category filtering
- **Data Synchronization** - Real-time sync across devices via Firebase

### Technical Improvements
- **Architecture Migration** - Migrated from XML layouts to Jetpack Compose
- **Dependency Injection** - Implemented Hilt for better dependency management
- **Background Processing** - WorkManager integration for reliable background tasks
- **Firebase Integration** - Comprehensive Firebase services (Auth, Firestore, Cloud Messaging)

## Contributing

To contribute to RemindMe, please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

For more information, see [CONTRIBUTING.md](CONTRIBUTING.md).
