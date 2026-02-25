# Serviaux - Project Instructions

## Project Overview

Serviaux is an Android app for automotive workshop management built with Kotlin, Jetpack Compose, Room, and MVVM architecture.

## Tech Stack

- Kotlin 2.0.21 + Jetpack Compose + Material 3
- Room Database with KSP (NOT kapt)
- Navigation Compose for routing
- Manual DI via AppContainer (NOT Hilt - due to AGP 9.x compatibility)
- MVVM with AndroidViewModel + StateFlow
- AGP 9.0.1, compileSdk 35, minSdk 26

## Key Conventions

- All UI state uses `data class XxxUiState` with `MutableStateFlow` in ViewModels
- ViewModels extend `AndroidViewModel` to access Application context
- Repository pattern: DAOs return `Flow<List<T>>`, repos wrap DAOs
- Form fields in UiState prefixed with `form` (e.g., `formName`, `formCode`)
- Navigation uses string route constants in `Routes.kt`
- Spanish language UI (labels, error messages, etc.)
- Password hashing: SHA-256 + random salt via `SecurityUtils`

## Project Structure

- `data/entity/` - Room entities and enums
- `data/dao/` - Room DAOs
- `data/ServiauxDatabase.kt` - Database singleton with seed callback
- `repository/` - Business logic repositories
- `di/AppContainer.kt` - Manual dependency injection
- `util/` - SecurityUtils, SessionManager
- `ui/` - Compose screens organized by feature module

## Important Patterns

- Save operations are async (coroutine). Form screens use `savedSuccessfully` flag in UiState + LaunchedEffect to navigate after save.
- Each feature module has: ListScreen, DetailScreen (if applicable), FormScreen, ViewModel
- Status transitions for work orders follow strict flow: RECIBIDO -> EN_DIAGNOSTICO -> COTIZADO -> APROBADO -> EN_PROGRESO -> COMPLETADO -> ENTREGADO
- Stock adjustments happen automatically when adding/removing WorkOrderParts
- Session management uses `SessionManager` singleton with `StateFlow<User?>` for current user state

## Build & Run

- Build: `./gradlew assembleDebug`
- No tests currently configured
- Run on API 26+ device/emulator
