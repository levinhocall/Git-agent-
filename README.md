# Git Agent Android AI Assistant

A small native Android chat assistant built with Kotlin and Jetpack Compose. It runs safely in demo mode without credentials and supports OpenAI-compatible chat completion APIs.

## Run

1. Open the repository in Android Studio Hedgehog or newer.
2. Copy `local.properties.example` values into your local configuration as needed. Never commit API keys.
3. Sync Gradle and run the `app` configuration on an Android 8.0+ device or emulator.

Without a key, the app uses demo mode. For a live provider, add these values to `app/build.gradle.kts` through your preferred local-only Gradle mechanism, or replace the generated `BuildConfig` fields in a private build flavor:

- `ASSISTANT_API_KEY`
- `ASSISTANT_BASE_URL` (default pattern: `https://api.openai.com/v1`)
- `ASSISTANT_MODEL`

The app intentionally does not include a real key. For production, put provider calls behind your own authenticated backend rather than shipping a provider secret in an APK.

## Features

- Compose chat UI with loading, error, clear, and demo states.
- OpenAI-compatible JSON request/response handling.
- Background networking with timeouts.
- Conversation state held in a ViewModel across configuration changes.
- Accessible labels and responsive layout.
