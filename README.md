# DotReminder

A reminder app for Android built with Kotlin, Jetpack Compose and Firebase.

[![Android 16](https://img.shields.io/badge/Target-Android_16_API_36-green)](https://developer.android.com/about/versions/16)
[![Version](https://img.shields.io/badge/Version-0.0.7-blue)](https://github.com/kareem2099/DotReminder)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

## Current release

| Setting | Value |
| --- | --- |
| Version name | **0.0.7** |
| Version code | **8** |
| Compile / target SDK | **36 — Android 16** |
| Minimum SDK | **29 — Android 10** |
| Application ID | `com.FreeRave.remindme` |

The Settings screen displays the version name and code from the build configuration.
Targeting Android 16 does not raise the minimum supported Android version.

## Features

- Time-based and recurring reminders, custom sounds, vibration and snooze.
- Contextual notification/exact-alarm permission requests, device-local alarm restoration and collision-safe reminder notifications.
- Location-based reminders, categories, reusable templates and calendar views.
- Glass surfaces and shared Compose components across the app, with light/dark themes and layouts reviewed for large text and RTL direction.
- AI conversation and reminder drafts in Arabic and English through the authenticated DotSuite gateway. Suggestions must be reviewed and saved in the regular reminder form.
- Email/password, Google Credential Manager and Facebook sign-in.
- Firebase SMS verification during registration and authenticator-based TOTP verification for enrolled accounts.
- Biometric **app lock** for an existing session. It complements provider sign-in and account MFA.
- Device-session records and account-security settings. Removing a session record is not the same as revoking an external provider's access token.
- Privacy links on login/registration, and **Account security → Privacy and your data** entries for support and account/data-deletion requests.
- Home-screen widgets for quick reminder entry and upcoming reminders.

## Privacy and support

- Support: [support@dotsuite.dev](mailto:support@dotsuite.dev).
- [Privacy policy](https://dotsuite.dev/en/privacy).
- [Request DotReminder account and associated data deletion](https://dotsuite.dev/en/privacy#delete-dotreminder).
- The web policy/request page is available in English, Arabic, German, French and Russian using `/en/`, `/ar/`, `/de/`, `/fr/` and `/ru/`.

The deletion pathway prepares an email request; opening a page or mail draft does not delete anything. Support verifies account ownership and processes verified requests within 30 days. The scope includes the Firebase account, profile, reminders and saved locations, categories, templates and device-session records. See [the fulfillment procedure](docs/account-deletion.md) for local copies and provider-retention details.

The website source lives in the separate `kareem2099/dotsuite` repository. Its connected GitHub deployment publishes the policy; Android bundles do not deploy the website.

## Build

Use an Android Studio version compatible with the project's Android Gradle Plugin, an installed Android 16 SDK, and JDK 17 or 21. Use the checked-in Gradle wrapper. Build-tool and dependency versions are declared in [gradle/libs.versions.toml](gradle/libs.versions.toml).

```bash
git clone https://github.com/kareem2099/DotReminder.git
cd DotReminder
```

Configure your Firebase Android application and place its downloaded `google-services.json` in `app/`. Enable the providers you use, register the correct signing fingerprints and configure TOTP in Firebase/Identity Platform when offering authenticator enrollment. Use [firestore.rules](firestore.rules) as the source for database rules; validate them before deploying to a real project.

Keep machine settings, API configuration and signing credentials in ignored `local.properties`. The build expects these property names as applicable:

```properties
sdk.dir=/path/to/Android/Sdk
GOOGLE_MAPS_API_KEY=your_android_restricted_maps_key
MYAPP_RELEASE_STORE_FILE=/path/to/upload-keystore.jks
MYAPP_RELEASE_STORE_PASSWORD=your_store_password
MYAPP_RELEASE_KEY_ALIAS=your_upload_key_alias
MYAPP_RELEASE_KEY_PASSWORD=your_key_password
```

Optional configuration includes `LLM_API_URL`, `LLM_MODEL_NAME`, `BANNER_AD_UNIT_ID`, `INTERSTITIAL_AD_UNIT_ID`, `APP_OPEN_AD_UNIT_ID` and `REWARDED_AD_UNIT_ID`.

AI requests use `https://dotsuite.dev/api/dotreminder/ai` with the signed-in account’s Firebase ID token. The server holds `DOTREMINDER_GEMINI_API_KEY`; Android does not read or embed a Gemini key and no longer includes the direct Gemini client SDK. The backend validates tokens, limits requests and returns reminder drafts for review. Do not commit API keys, signing files or passwords. See [the gateway notes](docs/protected-ai-gateway.md) and [release review](docs/auth-release-review.md) for deployment and verification details.

```bash
# Development APK
./gradlew :app:assembleDebug

# Unit tests and release lint
./gradlew :app:testDebugUnitTest :app:lintRelease

# Signed, minified Android App Bundle
./gradlew :app:bundleRelease
```

The AAB is generated at `app/build/outputs/bundle/release/app-release.aab`. A configured release upload key is required. Google Play App Signing, provider certificates and Play Console validation are separate from local signing.

## Architecture and review notes

Compose screens use ViewModels/controllers and service/repository boundaries, with Hilt injection, Firestore persistence and DataStore preferences. Authentication challenges and biometric locking are isolated from screen rendering. Alarm scheduling persists the device-local information needed for restoration.

- [Changelog](CHANGELOG.md)
- [Authentication release review](docs/auth-release-review.md)
- [Biometric lock and registration](docs/biometric-lock-and-registration.md)
- [Two-step verification](docs/two-step-verification.md)
- [Reminder delivery](docs/reminder-delivery.md)
- [Account deletion](docs/account-deletion.md)

The native interface currently uses English labels; multilingual web policies and RTL layout support do not imply a fully translated native app.

## Contributing

Open an issue or a pull request against this repository. Keep production credentials and generated APK/AAB files out of source commits.

## License

Copyright 2026 FreeRave(kareem).

Licensed under the [Apache License, Version 2.0](LICENSE).
