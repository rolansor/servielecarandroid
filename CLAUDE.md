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
- `data/ServiauxDatabase.kt` - Database singleton with seed callback and migrations (current version 8)
- `repository/` - Business logic repositories
- `di/AppContainer.kt` - Manual dependency injection
- `util/` - SecurityUtils, SessionManager, PhotoUtils, PdfReportGenerator, ShareUtils
- `ui/` - Compose screens organized by feature module
- `data/` - Excel source files (Clientes.xlsx, Vehiculos.xlsx, Ordenes de trabajo.xlsx, Catalogos.xlsx, etc.)
- `generate_seed.py` - Python script to generate seed_data.sql from Excel files

## Important Patterns

- Save operations are async (coroutine). Form screens use `savedSuccessfully` flag in UiState + LaunchedEffect to navigate after save.
- Each feature module has: ListScreen, DetailScreen (if applicable), FormScreen, ViewModel
- Order statuses: RECIBIDO, EN_DIAGNOSTICO, EN_PROCESO, EN_ESPERA_REPUESTO, LISTO, ENTREGADO, CANCELADO
- Status change in order detail uses inline FilterChips (not a dialog)
- Mechanic validation: orders cannot be marked as LISTO or ENTREGADO without at least one mechanic assigned
- Multiple mechanics per order with customizable commission (type: PORCENTAJE/FIJA, value per mechanic)
- Stock adjustments happen automatically when adding/removing WorkOrderParts
- Session management uses `SessionManager` singleton with `StateFlow<User?>` for current user state
- Photos stored as files in app internal storage (`vehicle_photos/` dir), paths saved as comma-separated string in `photoPaths` field on Vehicle and WorkOrder entities (max 6 per entity)
- Photo UI pattern: clickable thumbnails in LazyRow that open a dialog with preview/replace/delete options; camera+gallery icons in a Box at the end of the row for adding new photos
- Camera access via `ActivityResultContracts.TakePicture()` with FileProvider
- Predefined service catalog (`CatalogService`) with categories, default prices, and vehicle type variants
- SearchableDropdown component filters by both name and subtitle (e.g., search customers by name or cédula)
- Vehicle form fields: tipo vehículo, combustible (default Gasolina, FilterChips), tipo aceite (autocomplete from catalog), capacidad aceite (dropdown 1/2 galón steps up to 10)

## Seed Data Generation

- Source: Excel files in `data/` directory + `data/productos.json`
- Script: `python generate_seed.py` → outputs `app/src/main/assets/seed/seed_data.sql`
- Clientes.xlsx columns: IdCliente, Nombre, ID (cédula), Telefono, Fecha, Cuenta
- Vehiculos.xlsx columns: TIPO, MARCA, MODELO, AÑO, VERSION, COLOR, PATENTE, TRANSMISION, MOTOR, TRACCION, KMS, VIN, NUM_MOTOR, DATE_CREATED, ID_CLIENTE
- Vehicle-customer linking uses ID_CLIENTE column (references IdCliente from Clientes.xlsx)
- CONSUMIDOR FINAL (id=1) is the fallback for vehicles without assigned customer

## Build & Run

- Build: `./gradlew assembleDebug`
- No tests currently configured
- Run on API 26+ device/emulator
- Default admin: `servielecar` / `f4d3s2a1`
