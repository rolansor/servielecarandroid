# Serviaux - Project Instructions

## Project Overview

Serviaux is an Android app for automotive workshop management built with Kotlin, Jetpack Compose, Room, and MVVM architecture.

## Tech Stack

- Kotlin 2.1.20 + Jetpack Compose + Material 3
- Room Database with KSP (NOT kapt)
- Navigation Compose for routing
- Manual DI via AppContainer (NOT Hilt - due to AGP 9.x compatibility)
- MVVM with AndroidViewModel + StateFlow
- Coil 3 for image loading
- AGP 9.0.1, compileSdk 36, minSdk 26

## Key Conventions

- All UI state uses `data class XxxUiState` with `MutableStateFlow` in ViewModels
- ViewModels extend `AndroidViewModel` to access Application context
- Repository pattern: DAOs return `Flow<List<T>>`, repos wrap DAOs
- Form fields in UiState prefixed with `form` (e.g., `formName`, `formCode`)
- Navigation uses string route constants in `Routes.kt`
- Spanish language UI (labels, error messages, etc.)
- Password hashing: SHA-256 + random salt via `SecurityUtils`

## Project Structure

- `data/entity/` - Room entities and enums (includes CatalogService for predefined services)
- `data/dao/` - Room DAOs
- `data/ServiauxDatabase.kt` - Database singleton with seed callback (version 6)
- `repository/` - Business logic repositories
- `di/AppContainer.kt` - Manual dependency injection
- `util/` - SecurityUtils, SessionManager, PhotoUtils
- `ui/` - Compose screens organized by feature module

## Important Patterns

- Save operations are async (coroutine). Form screens use `savedSuccessfully` flag in UiState + LaunchedEffect to navigate after save.
- Each feature module has: ListScreen, DetailScreen (if applicable), FormScreen, ViewModel
- Status transitions for work orders follow strict flow: RECIBIDO -> EN_DIAGNOSTICO -> COTIZADO -> APROBADO -> EN_PROGRESO -> COMPLETADO -> ENTREGADO
- Stock adjustments happen automatically when adding/removing WorkOrderParts
- Session management uses `SessionManager` singleton with `StateFlow<User?>` for current user state
- Photos stored as files in app internal storage (`vehicle_photos/` dir), paths saved as comma-separated string in `photoPaths` field on Vehicle and WorkOrder entities (max 6 per entity)
- Camera access via `ActivityResultContracts.TakePicture()` with FileProvider
- Predefined service catalog (`CatalogService`) with categories and default prices, selectable when adding service lines to work orders

## Build & Run

- Build: `./gradlew assembleDebug`
- No tests currently configured
- Run on API 26+ device/emulator
- Default admin: `servielecar` / `f4d3s2a1`
