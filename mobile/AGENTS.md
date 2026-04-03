# MOBILE KNOWLEDGE

## OVERVIEW
Kotlin Multiplatform mobile app (`:app`, `:shared`) with Compose UI and shared domain/network layers.

## STRUCTURE
```text
mobile/
├── app/                                # Android application module
│   └── src/androidMain/kotlin/com/aura/app/
│       ├── AuraApplication.kt
│       └── MainActivity.kt
├── shared/                             # Shared KMP logic and feature screens
│   └── src/commonMain/kotlin/com/aura/
│       ├── core/**
│       └── feature/**
├── settings.gradle.kts                 # includes :app, :shared
└── app/build.gradle.kts                # Android target + Compose + dependencies
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| App startup / Android entry | `app/src/androidMain/.../AuraApplication.kt`, `MainActivity.kt` | Android bootstrap layer |
| Shared API client / DI | `shared/src/commonMain/kotlin/com/aura/core/data/api/AuraApiClient.kt`, `core/di/AppModule.kt` | Cross-feature dependencies |
| Navigation or screen behavior | `shared/src/commonMain/kotlin/com/aura/core/navigation/Navigation.kt`, `feature/*` screens | Main UI flows |
| Theme/UI primitives | `shared/src/commonMain/kotlin/com/aura/core/ui/theme/*`, `core/ui/components/*` | Shared styling/components |

## CONVENTIONS
- Most feature/domain code lives in `shared/commonMain`; Android module is thinner bootstrap shell.
- Kotlin/JVM target 17; Compose and Ktor are configured in `app/build.gradle.kts`.

## ANTI-PATTERNS
- Do not move shared business logic into Android-specific source sets unless platform-specific APIs require it.
- Do not change module names (`:app`, `:shared`) without updating `settings.gradle.kts` and dependent project references.

## COMMANDS
```bash
cd mobile
./gradlew assembleDebug
./gradlew test
```

## NOTES
- Repo root README may describe broader architecture; use actual Gradle/module files here as source of truth for mobile structure.
- `shared/src/commonMain/kotlin/com/aura/core/di/AppModule.kt` currently hardcodes `AuraApiClient("http://10.0.2.2:8000")`; validate alignment with active backend/API ports before mobile integration changes.
