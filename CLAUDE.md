# Serviaux - Instrucciones del Proyecto

## Descripción General

Serviaux es una app Android para gestión de talleres automotrices, construida con Kotlin, Jetpack Compose, Room y arquitectura MVVM.

## Stack Tecnológico

- Kotlin 2.1.20 + Jetpack Compose + Material 3
- Room Database con KSP (NO kapt)
- Navigation Compose para enrutamiento
- DI manual via AppContainer (NO Hilt - por compatibilidad con AGP 9.x)
- MVVM con AndroidViewModel + StateFlow
- Coil 3 para carga de imágenes
- Dropbox SDK (dropbox-core-sdk + dropbox-android-sdk 7.0.0) para respaldos en la nube
- AGP 9.0.1, compileSdk 36, minSdk 26

## Convenciones Clave

- Todo estado de UI usa `data class XxxUiState` con `MutableStateFlow` en ViewModels
- Los ViewModels extienden `AndroidViewModel` para acceder al contexto de Application
- Patrón repositorio: los DAOs retornan `Flow<List<T>>`, los repos envuelven DAOs
- Campos de formulario en UiState prefijados con `form` (ej: `formName`, `formCode`)
- La navegación usa constantes de ruta tipo string en `Routes.kt`
- Interfaz en español (etiquetas, mensajes de error, etc.)
- Hash de contraseñas: SHA-256 + salt aleatorio via `SecurityUtils`

## Estructura del Proyecto

- `data/entity/` - Entidades Room y enums (incluye CatalogService para servicios predefinidos)
- `data/dao/` - DAOs de Room
- `data/ServiauxDatabase.kt` - Singleton de la BD con callback de seed (versión actual 1, reiniciada tras reestructuración)
- `repository/` - Repositorios con lógica de negocio
- `di/AppContainer.kt` - Inyección de dependencias manual
- `util/` - SecurityUtils, SessionManager, PhotoUtils, PdfReportGenerator, CommissionPdfGenerator, ShareUtils, DropboxHelper
- `ui/` - Pantallas Compose organizadas por módulo de funcionalidad

## Patrones Importantes

- Las operaciones de guardado son asíncronas (coroutine). Las pantallas de formulario usan flag `savedSuccessfully` en UiState + LaunchedEffect para navegar después de guardar.
- Cada módulo tiene: ListScreen, DetailScreen (si aplica), FormScreen, ViewModel
- Estados de orden: RECIBIDO, EN_DIAGNOSTICO, EN_PROCESO, EN_ESPERA_REPUESTO, LISTO, ENTREGADO, CANCELADO
- Cambio de estado en detalle de orden usa FilterChips inline (no un diálogo)
- Validación de mecánico: las órdenes no pueden marcarse como LISTO o ENTREGADO sin al menos un mecánico asignado
- Múltiples mecánicos por orden con comisión personalizable (tipo: PORCENTAJE/FIJA, valor por mecánico)
- Estado de comisión mostrado como badges en detalle de orden (Sin comisión / Pagada / Pendiente) — sin edición inline
- Pagos de comisiones gestionados en pantalla dedicada solo-admin "Comisiones" con pago por lotes + reporte PDF
- Solo las comisiones de órdenes en estado LISTO o ENTREGADO aparecen en la pantalla de pago de comisiones
- Validación de descuento: descuento de servicio no puede exceder laborCost, descuento de repuesto no puede exceder subtotal
- Cambios de estado permitidos incluso desde ENTREGADO (solo admin) para soportar correcciones
- Ajustes de stock automáticos al agregar/eliminar WorkOrderParts
- Gestión de sesión usa singleton `SessionManager` con `StateFlow<User?>` para el estado del usuario actual
- Fotos almacenadas como archivos en almacenamiento interno (`vehicle_photos/`), rutas guardadas como string separado por comas en campo `photoPaths` en Vehicle y WorkOrder (máx 6 por entidad)
- Patrón UI de fotos: thumbnails clickeables en LazyRow que abren diálogo con vista previa/reemplazar/eliminar; iconos de cámara+galería en un Box al final de la fila para agregar nuevas
- Acceso a cámara via `ActivityResultContracts.TakePicture()` con FileProvider
- Catálogo de servicios predefinidos (`CatalogService`) con categorías, precios por defecto y variantes por tipo de vehículo
- Componente SearchableDropdown filtra por nombre y subtítulo (ej: buscar clientes por nombre o cédula)
- Campos de formulario de vehículo: tipo vehículo, combustible (default Gasolina, FilterChips), tipo aceite (autocomplete desde catálogo), capacidad aceite (dropdown en pasos de 1/2 galón hasta 10)
- Reportes PDF incluyen datos completos del vehículo (tipo, versión, combustible, transmisión, tracción, motor) e info de la orden (tipo, fecha admisión, nota de entrega, factura, notas)
- Columnas de tablas PDF usan alineación vertical consistente via helper `rightAlignAt`
- Integración Dropbox: OAuth2 PKCE (app key, sin secret), dependencias `dropbox-core-sdk` y `dropbox-android-sdk` 7.0.0
- Token Dropbox guardado en SharedPreferences (`dropbox_prefs`) como JSON serializado de `DbxCredential`
- Estructura de carpetas Dropbox: la app sandbox crea `/Aplicaciones/serviaux/` automáticamente; dentro se crean subcarpetas por dispositivo (`/{nombreDispositivo}/`) con archivos ZIP nombrados por fecha (`serviaux_backup_YYYY-MM-dd.zip`) que se sobrescriben si se sube el mismo día
- `DropboxHelper` es singleton en `util/` con métodos: `startAuth`, `handleAuthResult`, `isLinked`, `logout`, `uploadFile`, `listBackups`, `downloadFile`, `deleteFile`
- Flujo Dropbox en BackupScreen: vincular cuenta abre navegador para OAuth2, onResume captura credencial; subir exporta ZIP y lo sube; descargar lista backups de todos los dispositivos y reutiliza flujo de importación normal

## Datos Semilla

- `assets/seed/seed_data.sql` — Siempre se carga al crear la BD: usuario admin (servielecar), CONSUMIDOR FINAL (id=1), y todos los catálogos (marcas, modelos, colores, tipos vehículo, aceites, servicios, marcas repuestos, accesorios, motivos, diagnósticos)
- `assets/seed/sample_data.sql` — Datos de ejemplo opcionales cargados en el primer arranque si el usuario elige "Cargar ejemplos": usuarios, clientes, vehículos, repuestos y órdenes de prueba
- En el primer arranque después de crear la BD, DashboardScreen muestra un diálogo preguntando si cargar datos de ejemplo o empezar vacío
- CONSUMIDOR FINAL (id=1) es el fallback para vehículos sin cliente asignado

## Compilar y Ejecutar

- Compilar: `./gradlew assembleDebug`
- No hay tests configurados actualmente
- Ejecutar en dispositivo/emulador API 26+
- Admin por defecto: `servielecar` / `f4d3s2a1`
