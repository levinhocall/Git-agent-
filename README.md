# AURIX Android (Phase 1 Foundation)

AURIX is an Android AI assistant app built with Kotlin + Jetpack Compose + Material 3 in package `com.levinhocall.gitagent`.

## Implemented in this phase

- Provider-agnostic AI domain layer:
  - `AiProvider`, `AiModel`, `AiMessage`, `AiResult`, `ProviderError`, `TokenUsage`
- Provider abstraction and selection flow:
  - OpenRouter provider (real HTTP)
  - Gemini provider (real HTTP)
  - Model switching per provider
  - Provider health/config state
- Error handling:
  - auth, rate-limit, timeout, network, cancellation, service, unknown mapping
- Security:
  - Android Keystore-backed key storage via `EncryptedSharedPreferences` + `MasterKey`
  - No API keys in source, BuildConfig, or repository files
- Persistence:
  - lightweight local conversation history in shared preferences
  - simple relevance retrieval from prior messages
- UI (Compose, dark futuristic Phase 1):
  - chat history
  - loading/thinking state
  - animated AI core/orb
  - provider/model selectors
  - settings input for provider API key
  - microphone affordance placeholder (explicitly not implemented)
  - tool/status placeholders (explicitly not implemented)
- Tool registry foundation:
  - tool metadata, permission requirements, risk, approval state
  - safe `app.status` implementation + unsupported placeholder tools
- Unit tests:
  - provider error mapping
  - OpenRouter provider HTTP behavior using `MockWebServer`
  - credential storage behavior (`InMemoryCredentialStore`)
  - ViewModel state transitions

## Not yet implemented (roadmap)

- Streaming token-by-token UI rendering
- Real web search/tool execution
- Android device action tools
- Mission engine orchestration
- STT/TTS lifecycle integration
- Advanced long-term memory indexing

## Setup

1. Open in Android Studio (or use local Gradle).
2. Add API keys at runtime from the app settings area (keys are stored in Android Keystore-backed encrypted storage).
3. Select provider + model and start chatting.

### Provider notes

- **OpenRouter** endpoint default: `https://openrouter.ai/api/v1`
- **Gemini** endpoint default: `https://generativelanguage.googleapis.com/v1beta`

If a key is missing, the app shows a configuration error state. It does not fake success responses.

## Build and test

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

If your environment lacks Android plugin resolution/network access, run these commands in Android Studio with a configured Android SDK.
