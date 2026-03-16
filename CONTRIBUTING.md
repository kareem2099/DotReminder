# Contributing to RemindMe

Thank you for your interest in contributing to RemindMe! We welcome contributions from the community. This document provides guidelines and information on how to contribute to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Contribute](#how-to-contribute)
- [Development Setup](#development-setup)
- [Submitting Changes](#submitting-changes)
- [Reporting Issues](#reporting-issues)
- [Pull Request Process](#pull-request-process)

---

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior via [GitHub Issues](https://github.com/kareem2099/DotReminder/issues).

---

## How to Contribute

There are many ways to contribute to RemindMe:

- **Report bugs** and request features
- **Fix bugs** and implement features
- **Improve documentation**
- **Translate** the app
- **Test** the app and report issues

---

## Development Setup

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK API 29+

### Setup Steps

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/kareem2099/DotReminder.git
   cd DotReminder
   ```
3. Open the project in Android Studio
4. Add `google-services.json` to the `app/` folder (from Firebase Console)
5. Configure `local.properties` with your API keys:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key
   GOOGLE_MAPS_API_KEY=your_maps_api_key
   MYAPP_RELEASE_STORE_FILE=/path/to/keystore.keystore
   MYAPP_RELEASE_STORE_PASSWORD=your_password
   MYAPP_RELEASE_KEY_ALIAS=your_alias
   MYAPP_RELEASE_KEY_PASSWORD=your_password
   ```
6. Sync Gradle and build the project:
   ```bash
   ./gradlew build
   ```
7. Run the app:
   ```bash
   ./gradlew installDebug
   ```

### Project Structure

```
app/src/main/java/com/FreeRave/remindme/
├── data/       # DataStore preferences & repositories
├── di/         # Hilt dependency injection modules
├── model/      # Data models
├── navigation/ # NavGraph & Screen routes
├── screens/    # UI screens & ViewModels
├── service/    # Business logic & Firebase services
├── ui/         # Shared components, themes, responsive utils
├── utils/      # Notifications, location, NLP utils
└── widget/     # Home screen widgets
```

---

## Submitting Changes

### Git Workflow

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes and commit them:
   ```bash
   git add .
   git commit -m "feat: add your feature"
   ```

3. Push your changes:
   ```bash
   git push origin feature/your-feature-name
   ```

4. Create a Pull Request on GitHub

### Commit Message Format

Follow the [Conventional Commits](https://www.conventionalcommits.org/) style:

```
feat: add biometric login support
fix: resolve nested scroll crash in CategoriesScreen
docs: update README with Firestore rules
chore: upgrade AGP to 9.1
```

- `feat` — New feature
- `fix` — Bug fix
- `docs` — Documentation only
- `chore` — Build, config, or tooling changes
- `refactor` — Code change that neither fixes a bug nor adds a feature
- `test` — Adding or updating tests

### Code Style

- Follow existing code style in the project
- Use Kotlin coding conventions
- Ensure code is properly formatted (`Ctrl+Alt+L` in Android Studio)
- Add comments for complex logic
- Avoid leaving unused imports or dead code

---

## Reporting Issues

When reporting issues, please include:

- **Clear title** describing the issue
- **Steps to reproduce** the issue
- **Expected behavior**
- **Actual behavior**
- **Environment** (Android version, device model, app version)
- **Logcat output** if applicable
- **Screenshots** if applicable

---

## Pull Request Process

1. Ensure your code follows the project's coding standards
2. Update documentation if necessary
3. Add tests for new features when possible
4. Ensure all existing tests pass
5. Update `CHANGELOG.md` if applicable
6. Request review from maintainers

### Review Process

- Maintainers will review your PR within a reasonable time
- Address any feedback or requested changes
- Once approved, your PR will be merged into `main`

---

## Testing

- Write unit tests for new functionality when possible
- Test on multiple devices and Android versions (min API 29)
- Ensure UI tests pass before submitting PR

---

## Documentation

- Update `README.md` for significant changes
- Add KDoc comments for public functions and classes
- Update `CHANGELOG.md` for every notable change

---

## Questions?

If you have questions about contributing, feel free to:

- Open an issue on [GitHub Issues](https://github.com/kareem2099/DotReminder/issues)
- Join our community [GitHub Discussions](https://github.com/kareem2099/DotReminder/discussions)

Thank you for contributing to RemindMe! 🎉