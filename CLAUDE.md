# Serviaux - Instrucciones del Proyecto

## Descripción General

Serviaux es una app Android para gestión de talleres automotrices, construida con Kotlin, Jetpack Compose, Room y arquitectura MVVM.

## Stack Tecnológico

- Kotlin 2.2.10 + Jetpack Compose + Material 3
- Room Database con KSP (NO kapt)
- Navigation Compose para enrutamiento
- DI manual via AppContainer (NO Hilt - por compatibilidad con AGP 9.x)
- MVVM con AndroidViewModel + StateFlow
- Coil 3 para carga de imágenes
- Dropbox SDK (dropbox-core-sdk + dropbox-android-sdk 7.0.0) para respaldos en la nube
- AGP 9.3.1, Gradle 9.5.0, KSP 2.3.2, compileSdk 36, targetSdk 36, minSdk 26
- Release con R8: `isMinifyEnabled` + `isShrinkResources` activos (el APK baja de ~26 MB a ~6 MB). Las reglas de lo que no se puede ofuscar están en `app/proguard-rules.pro` (Dropbox/Jackson usan reflexión). **Falta definir `signingConfig`** con el keystore del taller

## Convenciones Clave

- Todo estado de UI usa `data class XxxUiState` con `MutableStateFlow` en ViewModels
- Los ViewModels extienden `AndroidViewModel` para acceder al contexto de Application
- Patrón repositorio: los DAOs retornan `Flow<List<T>>`, los repos envuelven DAOs
- Campos de formulario en UiState prefijados con `form` (ej: `formName`, `formCode`)
- La navegación usa constantes de ruta tipo string en `Routes.kt`
- Interfaz en español (etiquetas, mensajes de error, etc.)
- **Rediseño índigo (fase 1 aplicada)**: tema con rampas índigo/verde-agua/neutral en `ui/theme/` (tokens del handoff en `docs/nuevo_diseño/`, alcance en `docs/rediseno-fase1.md`). Color dinámico apagado y **tema claro forzado** (el modo oscuro no está diseñado). Fuentes Caprasimo (títulos/cifras) y Figtree (cuerpo) en `res/font/`. Orientación vertical fija en el manifest
- Navegación por 5 pestañas: **Taller** (`ui/taller/TallerScreen`, home "Órdenes activas" con secciones por estado — los estados vacíos no se muestran, secciones separadas por divisor) · Órdenes · Autos · Clientes · **Más** (`ui/more/MoreScreen`: Repuestos, Turnos, Historial y, solo para ADMIN, Comisiones/Reportes/Catálogos/Usuarios/Respaldos). El dashboard de módulos ya no existe; la ruta `dashboard` apunta al tablero
- Moneda SIEMPRE con `formatMoney()` de `util/Money.kt` (`$1.234,56`, formato Ecuador) en pantallas; kilometraje con `formatKm()`. Excepciones: los prefills de campos editables usan `%.2f` con punto (se parsean con `toDouble()`) y los PDF conservan su formato propio
- `StatusChip` implementa la semántica fija de estados: índigo sólido = en proceso, verde-agua = terminado, borde punteado = espera repuesto, neutro = inerte. El saldo es el único número coloreado (`SaldoSaldado`/`SaldoPendiente`)
- Hash de contraseñas: **PBKDF2-HMAC-SHA256** (120.000 iteraciones) + salt aleatorio via `SecurityUtils`, formato `pbkdf2$iteraciones$salt$hash`. Los hashes del formato antiguo (SHA-256 de una pasada, como el sembrado en `seed_data.sql`) se siguen aceptando y se migran al iniciar sesión, que es el único momento en que se conoce la contraseña en claro

## Estructura del Proyecto

- `data/entity/` - Entidades Room y enums (incluye CatalogService para servicios predefinidos)
- `data/dao/` - DAOs de Room
- `data/ServiauxDatabase.kt` - Singleton de la BD con callback de seed. **Versión actual 2**, con `exportSchema = true` (el esquema se versiona en `app/schemas/`) y **sin `fallbackToDestructiveMigration`**: al cambiar cualquier entidad hay que subir `version` y añadir su `Migration` en `buildDatabase`. Si falta la ruta de migración la app falla al abrir, en lugar de borrar en silencio todos los datos del taller
- `repository/` - Repositorios con lógica de negocio
- `di/AppContainer.kt` - Inyección de dependencias manual
- `util/` - SecurityUtils, SessionManager, PhotoUtils, Money (formatMoney/formatKm), PdfReportGenerator, CommissionPdfGenerator, ShareUtils, DropboxHelper
- `ui/` - Pantallas Compose organizadas por módulo de funcionalidad (`ui/taller/` es el home; `ui/more/` la pestaña Más; ya no existe `ui/dashboard/`)

## Patrones Importantes

- Las operaciones de guardado son asíncronas (coroutine). Las pantallas de formulario usan flag `savedSuccessfully` en UiState + LaunchedEffect para navegar después de guardar.
- Cada módulo tiene: ListScreen, DetailScreen (si aplica), FormScreen, ViewModel
- Estados de orden: RECIBIDO, EN_DIAGNOSTICO, EN_PROCESO, EN_ESPERA_REPUESTO, LISTO, ENTREGADO, CERRADO
- Cambio de estado en detalle de orden usa FilterChips inline (no un diálogo)
- CERRADO es el único estado que bloquea ediciones (servicios, repuestos, extras, pagos, fotos, archivos, mecánicos, campos del formulario). Todos los demás estados permiten edición libre incluso después de ENTREGADO.
- Validación de mecánico: las órdenes no pueden marcarse como LISTO o ENTREGADO sin al menos un mecánico asignado
- Múltiples mecánicos por orden con comisión personalizable (tipo: PORCENTAJE/FIJA, valor por mecánico)
- Estado de comisión mostrado como badges en detalle de orden (Sin comisión / Pagada / Pendiente) — sin edición inline
- Pagos de comisiones gestionados en pantalla dedicada solo-admin "Comisiones" con pago por lotes + reporte PDF
- Solo las comisiones de órdenes en estado LISTO o ENTREGADO aparecen en la pantalla de pago de comisiones
- Validación de descuento: descuento de servicio no puede exceder laborCost, descuento de repuesto no puede exceder subtotal
- El historial de cambios de estado se sigue registrando en `work_order_status_log`, pero ya no se muestra en el detalle de la orden
- Cambios de estado permitidos incluso desde ENTREGADO (solo admin) para soportar correcciones
- Ajustes de stock automáticos al agregar/eliminar/editar WorkOrderParts. Si no hay existencia suficiente la línea se registra igual, el stock queda negativo y la UI avisa del descubierto (no se bloquea el mostrador por un inventario mal contado)
- Las operaciones multi-tabla van en `database.withTransaction { }` (líneas de orden, cambio de estado, borrado de orden, restauración de respaldos, importación de catálogos)
- `WorkOrderRepository.recalculateTotals` redondea los importes a 2 decimales y **recalcula las comisiones** de los mecánicos no pagados: es el único punto por el que pasan todos los cambios de mano de obra. Las comisiones ya pagadas nunca se modifican
- Las comisiones pendientes de órdenes CERRADO también aparecen en la pantalla de pago (antes desaparecían al archivar la orden)
- Gestión de sesión usa singleton `SessionManager` con `StateFlow<User?>` para el estado del usuario actual
- Fotos almacenadas como archivos en almacenamiento interno (`vehicle_photos/`), rutas guardadas como string separado por comas en campo `photoPaths` en Vehicle y WorkOrder (máx 6 por entidad)
- Toda foto nueva se comprime al guardarse: `PhotoUtils.compressPhotoInPlace` la reduce a 1600 px de lado mayor con JPEG 80 y aplica la rotación EXIF a los píxeles (las cámaras entregan 3-5 MB y eran el 98% del peso de los respaldos). `PhotoUtils.optimizeExistingPhotos` recomprime las ya guardadas desde la pantalla de Respaldos
- Las copias de fotos y adjuntos son `suspend` sobre `Dispatchers.IO`: no deben invocarse desde el hilo de UI
- Patrón UI de fotos: thumbnails clickeables en LazyRow que abren diálogo con vista previa/reemplazar/eliminar; iconos de cámara+galería en un Box al final de la fila para agregar nuevas
- Acceso a cámara via `ActivityResultContracts.TakePicture()` con FileProvider
- Catálogo de servicios predefinidos (`CatalogService`) con categorías, precios por defecto y variantes por tipo de vehículo
- Componente SearchableDropdown filtra por nombre y subtítulo (ej: buscar clientes por nombre o cédula)
- Campos de formulario de vehículo: tipo vehículo, combustible (default Gasolina, FilterChips), tipo aceite (autocomplete desde catálogo), capacidad aceite (dropdown en pasos de 1/2 galón hasta 10)
- Reportes PDF incluyen datos completos del vehículo (tipo, versión, combustible, transmisión, tracción, motor) e info de la orden (tipo, fecha admisión, nota de entrega, factura, notas)
- Columnas de tablas PDF usan alineación vertical consistente via helper `rightAlignAt`
- Integración Dropbox: OAuth2 PKCE (app key, sin secret), dependencias `dropbox-core-sdk` y `dropbox-android-sdk` 7.0.0
- Token Dropbox guardado en SharedPreferences (`dropbox_prefs`) como JSON serializado de `DbxCredential`
- Estructura de carpetas Dropbox: la app sandbox crea `/Aplicaciones/serviaux/` automáticamente; dentro se crean subcarpetas por dispositivo (`/{nombreDispositivo}/`)
- Tres modos de respaldo (`BackupContent`), elegibles en BackupScreen:
  - `DATA_ONLY` — solo los JSON de datos (pesa muy poco, apto para respaldar a diario). Sube como `serviaux_datos_YYYY-MM-DD.zip`, se sobrescribe el del día
  - `MEDIA_ONLY` — todas las fotos, sin datos; es el respaldo base de imágenes. Sube como `serviaux_fotos_YYYY-MM-DD.zip`
  - `ALL_INCREMENTAL` (por defecto) — datos completos + solo las fotos nuevas desde el último respaldo. Sube como `serviaux_backup_YYYY-MM-DD_HHmm.zip` con hora, porque **no debe sobrescribirse**: cada incremental contiene fotos distintas
- El marcador del incremental vive en SharedPreferences `serviaux_backup` (`last_photo_backup_at`) y solo avanza cuando el respaldo incluyó fotos. `resetPhotoBackupMarker` lo olvida para volver a incluirlas todas
- El manifiesto declara `content`, `photoMode` (`full`/`incremental`/`none`), `photosSince`, `photoCount` y `photosSkipped`. Los respaldos generados antes de esta versión no los traen y `inspectBackup` deduce el modo
- Al restaurar, las fotos se agregan **sin borrar** las existentes, así se puede restaurar un respaldo base y luego los incrementales en cadena; las rutas absolutas se reapuntan al directorio de este dispositivo
- `DropboxHelper` es singleton en `util/` con métodos: `startAuth`, `handleAuthResult`, `isLinked`, `logout`, `uploadFile`, `listBackups`, `downloadFile`, `deleteFile`
- Flujo Dropbox en BackupScreen: vincular cuenta abre navegador para OAuth2, onResume captura credencial; subir exporta ZIP y lo sube; descargar lista backups de todos los dispositivos y reutiliza flujo de importación normal

## Datos Semilla

- `assets/seed/seed_data.sql` — Siempre se carga al crear la BD: usuario admin (servielecar), CONSUMIDOR FINAL (id=1), y todos los catálogos (marcas, modelos, colores, tipos vehículo, aceites, servicios, marcas repuestos, accesorios, motivos, diagnósticos)
- `assets/seed/sample_data.sql` — Datos de ejemplo opcionales cargados en el primer arranque si el usuario elige "Cargar ejemplos": usuarios, clientes, vehículos, repuestos y órdenes de prueba
- En el primer arranque después de crear la BD, DashboardScreen muestra un diálogo preguntando si cargar datos de ejemplo o empezar vacío
- CONSUMIDOR FINAL (id=1) es el fallback para vehículos sin cliente asignado

## Compilar y Ejecutar

- Compilar: `./gradlew assembleDebug`
- Tests unitarios: `./gradlew test` (`SecurityUtilsTest`, `RoutesTest` y `MoneyTest`; corren en la JVM, sin emulador)
- Release: `./gradlew assembleRelease` (pasa por R8; el APK sale sin firmar hasta que se configure el keystore)
- Ejecutar en dispositivo/emulador API 26+
- Admin por defecto: `servielecar` / `f4d3s2a1`
