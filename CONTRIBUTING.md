# Contributing to RemindMe

Thank you for your interest in contributing to RemindMe! We welcome contributions from the community. This document provides guidelines and information on how to contribute to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Contribute](#how-to-contribute)
- [Development Setup](#development-setup)
- [Submitting Changes](#submitting-changes)
- [Reporting Issues](#reporting-issues)
- [Pull Request Process](#pull-request-process)

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to [project maintainers].

## How to Contribute

There are many ways to contribute to RemindMe:

- **Report bugs** and request features
- **Fix bugs** and implement features
- **Improve documentation**
- **Translate** the app
- **Test** the app and report issues

## Development Setup

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK API 21+

### Setup Steps

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/your-username/remindme.git
   cd remindme
   ```
3. Open the project in Android Studio
4. Build and run the app

### Project Structure

- `app/src/main/java/com/FreeRave/remindme/` - Main application code
- `app/src/main/res/` - Resources (layouts, drawables, etc.)
- `app/src/test/` - Unit tests
- `app/src/androidTest/` - Instrumentation tests

## Submitting Changes

### Git Workflow

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes and commit them:
   ```bash
   git add .
   git commit -m "Add your commit message"
   ```

3. Push your changes:
   ```bash
   git push origin feature/your-feature-name
   ```

4. Create a Pull Request on GitHub

### Commit Messages

Please follow these guidelines for commit messages:

- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests liberally

### Code Style

- Follow the existing code style in the project
- Use Kotlin coding conventions
- Ensure code is properly formatted
- Add comments for complex logic

## Reporting Issues

When reporting issues, please include:

- **Clear title** describing the issue
- **Steps to reproduce** the issue
- **Expected behavior**
- **Actual behavior**
- **Environment** (Android version, device, etc.)
- **Screenshots** if applicable

## Pull Request Process

1. Ensure your code follows the project's coding standards
2. Update documentation if necessary
3. Add tests for new features
4. Ensure all tests pass
5. Update the CHANGELOG.md if applicable
6. Request review from maintainers

### Review Process

- Maintainers will review your PR
- Address any feedback or requested changes
- Once approved, your PR will be merged

## Testing

- Write unit tests for new functionality
- Test on multiple devices and Android versions
- Ensure UI tests pass

## Documentation

- Update README.md for significant changes
- Add code comments for complex logic
- Update API documentation if applicable

## Questions?

If you have questions about contributing, feel free to:

- Open an issue on GitHub
- Join our community discussions
- Contact the maintainers

Thank you for contributing to RemindMe! 🎉
