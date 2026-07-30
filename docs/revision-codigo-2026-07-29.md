# Serviaux — Revisión de código consolidada

**Fecha:** 29/07/2026 · **Base:** `main` @ `1553ac3` · **Alcance:** 23.850 líneas Kotlin, 7 revisiones paralelas (datos, repositorios, órdenes, resto de UI, respaldos/seguridad, PDF/fotos, navegación/build)
**Compilación:** `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (1m 25s, Gradle 9.5.0). 22 warnings del compilador.

> Los hallazgos fueron verificados por los revisores contra el código real, con archivo y línea. Antes de corregir cualquiera conviene reconfirmarlo puntualmente: los agentes pueden equivocarse en la interpretación aunque el código citado sea correcto.

---

## Resumen

- **21 defectos críticos** (pérdida de datos, bypass de seguridad, descuadre de dinero/inventario, ANR).
- **~45 importantes** y **~35 menores**, tras fusionar los que varias revisiones reportaron por separado.
- Tres causas estructurales explican la mayoría: **(a)** no existe una sola transacción en todo el proyecto, **(b)** las reglas de negocio viven solo en la UI y no en los repositorios, **(c)** los repositorios escriben la entidad completa (`@Update`) a partir de snapshots obsoletos.
- Lo que **sí** está bien: `seed_data.sql` es íntegro, los `SUM()` usan `COALESCE`, ningún `JOIN` duplica filas, ningún log filtra credenciales, todos los streams usan `use {}`, el `FileProvider` está correctamente cerrado, y los PDF se generan fuera del hilo principal.

---

## A. Crítico

### A1 · Seguridad y control de acceso

**1. Bypass total de la autenticación biométrica**
`ui/auth/LoginScreen.kt:231-235` — si `canAuthenticate() != BIOMETRIC_SUCCESS`, `showBiometricPrompt` llama **`onSuccess()`** y retorna. Combinado con el `LaunchedEffect` que lo dispara al arrancar (`:75-83`) y `tryRestoreSession()`.
*Escenario:* dispositivo sin huella registrada, sensor deshabilitado o degradado → cualquiera que abra la app entra con la sesión del último usuario, incluido `servielecar`/ADMIN, sin contraseña.
*Arreglo:* llamar `onFailed()` (mostrar formulario) cuando no haya biométrico; el único camino a `tryRestoreSession()` debe ser `onAuthenticationSucceeded`.

**2. Zip Slip al restaurar respaldos → escritura arbitraria en el sandbox**
`repository/BackupRepository.kt:644-650` — `File(photosDir, name.removePrefix("photos/"))` sin canonicalizar.
*Escenario:* un ZIP con una entrada `photos/../../databases/serviaux` y un `manifest.json` válido sobrescribe la base de datos **mientras está abierta**, o `shared_prefs/dropbox_prefs.xml` / `serviaux_session.xml`. Permite inyectar un usuario ADMIN propio.
*Arreglo:* validar `destFile.canonicalPath.startsWith(photosDir.canonicalPath + separator)` y usar solo `File(name).name`.

**3. Cualquier rol puede eliminar una orden completa**
`ui/workorders/WorkOrderDetailScreen.kt:532-541` — el botón solo comprueba `!isLocked`, nunca `isAdmin`; `WorkOrderViewModel.deleteOrder:1604` tampoco. `SessionManager.canDeleteOrders():68` existe y **no se usa en ningún lugar del proyecto**.
*Escenario:* un mecánico abre cualquier orden no cerrada, escribe el número y borra servicios, repuestos, pagos, historial, comisiones y fotos, sin recuperación.

**4. Pantallas de administración alcanzables sin rol y sin sesión**
`ui/navigation/NavGraph.kt:323-370` (Usuarios, Reportes, Comisiones, Catálogos, Respaldos) no comprueban rol; el filtro está solo en el Dashboard. `util/SessionManager.kt:31` mantiene el usuario **solo en memoria**, mientras `rememberNavController` restaura el back stack tras muerte de proceso.
*Escenario:* admin en "Usuarios" → Android mata el proceso → al volver, la app restaura esa pantalla con `currentUser == null`, plenamente funcional y sin login.

**5. Refresh token de Dropbox en claro y extraíble del dispositivo**
`util/DropboxHelper.kt:223-228` guarda `DbxCredential.toString()` (incluye `refresh_token` y `app_secret`) en SharedPreferences. `AndroidManifest.xml:11` tiene `allowBackup="true"` y `res/xml/backup_rules.xml` excluye **solo** `domain="database"`: `shared_prefs/` (token + sesión) y `files/` (fotos de clientes, PDFs) sí viajan a Google Cloud Backup y a la transferencia entre dispositivos.
*Escenario:* quien tenga acceso a la cuenta Google del taller obtiene un refresh token vivo con acceso a todos los respaldos, es decir a la base completa.
*Arreglo:* excluir `shared_prefs` y `files` en ambos XML (o `allowBackup="false"`) y cifrar la credencial con el Keystore.

### A2 · Pérdida o corrupción de datos

**6. `fallbackToDestructiveMigration(dropAllTables = true)` sin ninguna migración**
`data/ServiauxDatabase.kt:136`, con `version = 2` y cero clases `Migration`.
*Escenario:* el próximo cambio de esquema borra todas las órdenes, clientes y vehículos del taller al actualizar el APK. El commit `1b13fa3` ya subió 1→2 con esta configuración.

**7. La restauración de respaldos borra antes de validar y sin transacción**
`repository/BackupRepository.kt:436-467` (borrado) vs `474-638` (inserción). Sin `withTransaction`.
*Escenario:* restaurar solo "Órdenes de Trabajo" → se vacían las 7 tablas; al reinsertar, la FK `work_orders.vehicleId` (sin `onDelete`) falla si los ids del respaldo no existen localmente → **todas las órdenes destruidas y nada restaurado**.

**8. Cuatro `valueOf` sin fallback en la importación**
`BackupRepository.kt:969, 1103, 1207, 1277` (`AppointmentStatus`, `UserRole`, `Priority`, `PaymentMethod`). Solo `status`, `orderType` y `arrivalCondition` tienen protección.
*Escenario:* un respaldo con un valor renombrado (ya pasó con `CANCELADO`) lanza excepción **después** de vaciar `users` → no queda ningún admin y `onCreate` no vuelve a dispararse. Acceso a la app perdido.

**9. `importFromJson` de catálogos borra los 10 catálogos antes de validar**
`repository/CatalogRepository.kt:169-265` — `deleteAll*` en las líneas 173-182, `getString("nombre")` puede lanzar en la 222.
*Escenario:* un JSON con un servicio sin campo `nombre` deja el taller sin marcas, modelos, colores, quejas ni diagnósticos, sin rollback.

**10. Los datos de ejemplo fallan siempre y en silencio**
`assets/seed/sample_data.sql:67,70,73` — los 3 `INSERT INTO work_orders` omiten `totalExtras`, declarada `REAL NOT NULL` sin DEFAULT.
*Escenario:* primer arranque → "Cargar ejemplos" → `NOT NULL constraint failed` → rollback total → el usuario se queda sin ningún dato de ejemplo (ni usuarios, ni clientes). Confirmado por git: es la tercera vez que ocurre el mismo desfase (`98c47b7`, `d79648d`).

**11. Un fallo del seed deja la base sin admin, sin recuperación**
`data/ServiauxDatabase.kt:158-169` — `seed()` captura todo y solo hace `Log.e`. Si falla, la transacción revierte, `onCreate` no vuelve a ejecutarse y no hay usuario `servielecar` ni catálogos: login imposible, único remedio borrar los datos de la app.

**12. Se pierden fotos y adjuntos por escribir la entidad completa desde un snapshot obsoleto**
`ui/workorders/WorkOrderViewModel.kt:1462-1480`, `:1540-1549`, `:1482-1491` — capturan `selectedOrder` fuera de la corrutina y hacen `copy()` sobre la entidad entera.
*Escenario:* el usuario toma una foto (guardado en vuelo) y pulsa "Volver", que llama `saveDetailFields()` con el snapshot viejo → `photoPaths` vuelve atrás, la foto queda huérfana en disco y desaparece de la orden.

**13. `deleteOrder` no es atómico y borra comisiones pagadas**
`repository/WorkOrderRepository.kt:184-201` — restaura stock **antes** de los 7 DELETE, sin transacción; `deleteByWorkOrder` elimina también comisiones ya pagadas; deja turnos CONVERTIDO apuntando a una orden inexistente.
*Escenario:* si falla a mitad, al reintentar el stock se devuelve **dos veces**; y el historial de pagos de comisiones pierde filas que ya se pagaron con PDF emitido.

### A3 · Dinero e inventario

**14. Eliminar un repuesto de una orden no devuelve el stock**
`repository/WorkOrderRepository.kt:127-130` — sin `increaseStock`. Contradice `CLAUDE.md` y el propio `deleteOrder`, que sí lo hace. *(Reportado por 3 de las 7 revisiones.)*
*Escenario:* agregar 5 filtros por error y borrar la línea → 5 unidades perdidas del inventario para siempre.

**15. Editar la cantidad de un repuesto no ajusta el stock**
`repository/WorkOrderRepository.kt:122-125` — `update` + recálculo, sin delta.
*Escenario:* línea creada con 1 ud (stock 10→9), editada a 8 uds: se factura 8 y el stock sigue en 9 en vez de 2.

**16. Sobreventa silenciosa: se ignora el resultado de `decreaseStock`**
`repository/WorkOrderRepository.kt:112-117` + `data/dao/PartDao.kt:56-57` (el SQL solo actúa `WHERE currentStock >= :qty` y devuelve las filas afectadas).
*Escenario:* stock 2, se agregan 10 uds. La línea se inserta, el total sube, el stock se queda en 2 y no hay ningún error. El diálogo muestra el stock pero no lo valida.

**17. Las comisiones porcentuales quedan congeladas en $0**
`ui/workorders/WorkOrderViewModel.kt:747-762` — `recalculateCommissions()` existe y **no tiene ningún llamador** en todo el proyecto.
*Escenario:* se asigna un mecánico al 10 % cuando `totalLabor = 0` (flujo normal: sin mecánico no se puede pasar a LISTO) y luego se agregan $400 de mano de obra. La comisión sigue en $0.00, se muestra así, se paga así y se imprime así en el PDF. **No hay forma de corregirlo desde la UI**, porque el detalle no permite edición inline.

**18. Cerrar una orden hace desaparecer sus comisiones impagas**
`data/dao/WorkOrderMechanicDao.kt:43-53` filtra `status IN ('LISTO','ENTREGADO')`; `CERRADO` no está y nada impide cerrar con comisiones pendientes.
*Escenario:* orden ENTREGADO con $150 pendientes que un admin archiva → la comisión desaparece de Comisiones y del historial. El mecánico no cobra y el pasivo es invisible.

**19. Se pueden registrar pagos en una orden CERRADA**
`ui/workorders/WorkOrderDetailScreen.kt:1428-1432` — es el **único** botón de la pantalla sin `enabled = !isLocked`; `addPayment` tampoco valida el estado. Viola la regla explícita de `CLAUDE.md`.

### A4 · Estabilidad

**20. ANR al agregar fotos o adjuntos**
`util/PhotoUtils.kt:68-83` y `:93-114` copian de forma síncrona, y se invocan desde funciones de ViewModel que no son `suspend` (`WorkOrderViewModel.kt:1352,1382,1397,1417,1522`; `VehicleViewModel.kt:744,789`), además **en bucle** sobre la selección múltiple.
*Escenario:* 8 fotos de 6 MB o un PDF de 30 MB desde Drive (que además descarga por red) → 10-30 s de hilo principal bloqueado → ANR.

**21. Fuga de colectores en el historial de servicios**
`ui/history/ServiceHistoryViewModel.kt:97-163` — dentro de un `collect` se lanzan **dos colectores infinitos por cada orden**, sobre `mutableMapOf` compartidos mutados desde N corrutinas.
*Escenario:* cliente con 40 órdenes → 80 colectores permanentes; cualquier escritura en `work_orders` añade otros 80 sin cancelar los previos → crecimiento sin límite y `ConcurrentModificationException`.

---

## B. Importante

### Datos y esquema
| # | Hallazgo | Ubicación |
|---|---|---|
| B1 | **Cero `@Transaction`/`withTransaction` en todo el proyecto**: `addWorkOrderPart` hace 3 escrituras, `deleteOrder` 8 | global |
| B2 | FKs de `appointments` con `ON DELETE NO ACTION` → rompen borrados y restauración parcial | `Appointment.kt:16-19` |
| B3 | Igual en `work_orders` → un cliente con órdenes no se puede borrar (cascada aborta) | `WorkOrder.kt:51-54` |
| B4 | TypeConverters con `Enum.valueOf` sin fallback: un typo en el `.sql` no falla al escribir sino al leer | `Converters.kt:19-32` |
| B5 | `commissionType` persistido como `String` crudo pese a existir el enum `CommissionType`; SQL compara con literal `'NINGUNA'` | `User.kt:34`, `WorkOrderMechanic.kt:34` |
| B6 | Faltan índices en columnas de filtrado/orden intensivo (`work_orders.createdAt`, `entryDate`, `customers.createdAt`, `parts.name`, `commissionPaid`) | varias entidades |
| B7 | `WorkOrderMechanic.mechanicId` con CASCADE a `users`: borrar un mecánico elimina su historial de comisiones **pagadas** | `WorkOrderMechanic.kt:22-27` |

### Lógica de negocio y dinero
| # | Hallazgo | Ubicación |
|---|---|---|
| B8 | `recalculateTotals` hace read-modify-write de la fila completa → pisa cambios concurrentes (mismo patrón en `changeStatus` y `assignMechanic`) | `WorkOrderRepository.kt:210-224` |
| B9 | Ninguna regla de negocio se aplica en el repositorio: `changeStatus` acepta cualquier transición, sin validar mecánico ni bloqueo por CERRADO | `WorkOrderRepository.kt:69-82` |
| B10 | Dinero en `Double` sin redondeo al persistir: el PDF de comisiones imprime filas redondeadas y suma valores crudos → el total no cuadra | varias entidades |
| B11 | `PartRepository.update` pisa `currentStock` con el valor del formulario (obsoleto, o 0 si el campo quedó vacío) | `PartRepository.kt:24` |
| B12 | Se puede desactivar al último ADMIN (o a uno mismo) y quedar bloqueado sin recuperación | `AuthRepository.kt:84-88` |
| B13 | `getTotalByDateRange` usa `SUM(total)` e ignora los descuentos de pago → reportes inflados frente a caja | `WorkOrderDao.kt:61` |
| B14 | `batchMarkAsPaid` no revalida `commissionPaid = 0` y `IN (:ids)` puede exceder el límite de 999 variables de SQLite | `WorkOrderMechanicDao.kt:55-56` |

### UI y flujos
| # | Hallazgo | Ubicación |
|---|---|---|
| B15 | **Los diálogos deciden si cerrarse leyendo `uiState` dentro del propio `onClick`**, valor que aún no refleja el `update` recién hecho → se cierran perdiendo lo escrito (repuestos, extras) o quedan abiertos permitiendo inserción duplicada (servicios) | `WorkOrderDetailScreen.kt:249-256, 283-292, 374-377` |
| B16 | El campo Precio se **vacía al enfocarlo** y el diálogo no valida el precio antes de cerrar → edición perdida con solo un snackbar | `WorkOrderDetailScreen.kt:2003-2010, 2034-2047` |
| B17 | Duplicados no bloqueados al guardar (placa, cédula, username): la comprobación es asíncrona por foco y `validateForm()` la sobreescribe. `customers` además no tiene índice único → duplicados silenciosos; `AuthRepository.updateUser` no valida unicidad | `VehicleViewModel.kt:526-558`, `CustomerViewModel.kt:262-290`, `UserViewModel.kt:169-196` |
| B18 | `savedSuccessfully` nunca se resetea en 4 de 5 módulos → una rotación tras guardar dispara `onSaved()` otra vez y retrocede dos pantallas | `Customer:329`, `Vehicle:618`, `Part:291`, `User:238` |
| B19 | Formularios de edición que se auto-resetean: `getById(id).collect { prepareEdit(it) }` reescribe los campos en cada emisión de Room | `PartViewModel.kt:298-304`, `UserViewModel.kt:245-251` |
| B20 | Turnos guardados **con un día de desfase**: el picker devuelve medianoche UTC y se compone en zona local (en UTC-5, elegir 30/07 guarda 29/07) | `AppointmentFormScreen.kt:282-289` + `AppointmentViewModel.kt:196-203` |
| B21 | El límite de 6 fotos no se aplica: solo oculta el botón, el picker es múltiple y el detalle no tiene tope alguno | `WorkOrderFormScreen.kt:470`, `WorkOrderDetailScreen.kt:811-844` |
| B22 | `replacingPhoto` no se resetea si se cancela el selector → la siguiente foto **reemplaza** en lugar de agregar, borrando la original | `WorkOrderFormScreen.kt:165-174` |
| B23 | Errores de SQLite mostrados al usuario: `UNIQUE constraint failed: vehicles.plate (code 2067...)` en una UI en español | 4 ViewModels |
| B24 | El diálogo de datos de ejemplo puede quedar bloqueado para siempre (sin `try/catch`, `onDismissRequest` vacío, botones deshabilitados) → primer arranque inutilizable | `DashboardViewModel.kt:130-139` |
| B25 | Acciones irreversibles sin confirmación: pagar comisiones en lote, desactivar usuario, restablecer contraseña (sin feedback) | `CommissionScreen.kt:123-128`, `UserListScreen.kt:157-160` |
| B26 | Reglas ausentes en turnos (fecha pasada y solapamiento permitidos) y en repuestos (`formCode` solo numérico atrapa los códigos alfanuméricos existentes: campo en error permanente que solo se puede borrar) | `AppointmentViewModel.kt:185-193`, `PartViewModel.kt:141-187` |
| B27 | Colecciones de Flow sin `Job` de cancelación (patrón correcto ya existe en el mismo código, pero no se aplica uniformemente) | `Vehicle:302-317,687-701`, `Customer:125-131`, `Appointment:95-101`, `WorkOrder:422-481` |
| B28 | ViewModels de lista instanciados en formularios y detalles: abrir "Nuevo Cliente" dispara la paginación completa; el detalle de cliente arranca 6 consultas | `Customer:69-72`, `Vehicle:125-130`, `Part:64-68` |
| B29 | Cada instancia de `WorkOrderViewModel` arranca 9 colectores; el detalle ejecuta la consulta de la lista completa y el agregado de pagos que nunca lee | `WorkOrderViewModel.kt:193-204` |
| B30 | `serviceDescriptionsMap` y `partNamesMap` escanean dos tablas completas **en cada cambio de filtro** y nunca se leen | `WorkOrderViewModel.kt:304-316` |
| B31 | Reportes: excepciones silenciadas (sin campo `error`) y rango inicial sin normalizar → "Total $0.00" indistinguible de "no hubo trabajo" | `ReportsViewModel.kt:29-30, 72-74` |

### Respaldos, PDF y build
| # | Hallazgo | Ubicación |
|---|---|---|
| B32 | El ZIP es texto plano con **hashes de contraseñas** y PII completa, y se comparte por Intent y se sube a la nube. `exportByYear` exporta todos los usuarios ignorando la selección | `BackupRepository.kt:717`, `:244,287` |
| B33 | `handleAuthResult()` en cada `ON_RESUME` reprocesa una credencial estática: "Desvincular" se revierte solo, y el mensaje "Dropbox vinculado" pisa el resultado real de exportar. `logout` tampoco revoca el token en el servidor | `BackupScreen.kt:93-102`, `DropboxHelper.kt:81-85` |
| B34 | El ZIP completo se carga en memoria **dos veces**, sin límite de tamaño ni de entradas (zip bomb → OOM, y el error se traga como "No se encontraron datos") | `BackupRepository.kt:360-369, 400-409` |
| B35 | SHA-256 de **una sola iteración** para contraseñas (el salt sí usa `SecureRandom`). Agravado por B32 y por la contraseña del admin sembrada y documentada | `SecurityUtils.kt:20-45` |
| B36 | Subida a Dropbox en una sola petición (límite 150 MB), sin reintento, progreso ni cancelación | `DropboxHelper.kt:111-116` |
| B37 | Los adjuntos y las fotos de matrícula **no se respaldan**, pero sus rutas absolutas sí → tras restaurar en otro dispositivo se pierden en silencio | `BackupRepository.kt:174-184, 311-321` |
| B38 | Exportación y conteos cargan la base entera en memoria (`getRecordCounts` trae todas las filas de 8 tablas para hacer `.size`) | `BackupRepository.kt:86-107, 688-699` |
| B39 | `PdfDocument` y la página no se cierran ante excepción → fuga de memoria nativa acumulativa | 3 generadores |
| B40 | Las cabeceras de tabla no se repiten al saltar de página; el zebra-striping se descuadra | 3 generadores |
| B41 | El PDF ignora el EXIF y deforma las miniaturas a 1:1 → fotos giradas 90° y achatadas en el documento que se entrega al cliente | `PdfReportGenerator.kt:701-715` |
| B42 | El PDF de la orden **nunca muestra los mecánicos**: usa `assignedMechanicId`, que el ViewModel no escribe nunca (el modelo real es multi-mecánico) | `WorkOrderViewModel.kt:1556` |
| B43 | `generateSingle` de turnos no pagina y el resumen por estado se dibuja fuera de página | `AppointmentPdfGenerator.kt:254-266, 308-441` |
| B44 | Archivos huérfanos: `deleteVehicle` no borra sus fotos (hasta 8 por vehículo) y `files/reports/` nunca se purga | `VehicleViewModel.kt:832-840` |
| B45 | Insets duplicados en edge-to-edge: `Modifier.padding(innerPadding)` no los consume y cada pantalla tiene su propio Scaffold → doble margen superior | `MainActivity.kt:65` |
| B46 | Release sin R8 ni firma: `isMinifyEnabled = false`, sin `shrinkResources`, sin `signingConfig`, `proguard-rules.pro` vacío, `material-icons-extended` completo empaquetado | `app/build.gradle.kts:22-28` |
| B47 | La paleta de marca es código muerto: `dynamicColor = true` la ignora en todo Android 12+ | `Theme.kt:78` |
| B48 | Sin tema de ventana oscuro (`themes.xml` hereda de `Theme.Material.Light`, no hay `values-night/`) → destello blanco al arrancar en modo oscuro | `res/values/themes.xml:3` |
| B49 | Contraste AA incumplido en `StatusChip`: "En Proceso" ≈ 1.7:1, "Entregado" ≈ 1.9:1 (mínimo 4.5:1); los 10 colores semánticos no tienen variante de tema | `CommonComponents.kt:92-129`, `Color.kt:67-78` |
| B50 | Ninguna navegación usa `launchSingleTop`: un doble toque en una fila apila dos detalles | `NavGraph.kt` (todas) |
| B51 | Dos destinos comparten el path `workorders/form` (con y sin params) → la conversión turno→orden depende del "best match" interno y puede perder los ids preseleccionados | `Routes.kt:29,38` |
| B52 | `SearchableDropdown`: `onExpandedChange` es no-op (el menú no reabre al reenfocar), `maxSuggestions` no es key del `remember`, y fuerza mayúsculas sin poder desactivarlo | `SearchableDropdown.kt:68-107` |
| B53 | El shimmer recompone en cada frame y crea un `rememberInfiniteTransition` por ítem (6 relojes, ~18 brushes/frame) | `ShimmerEffect.kt:39-54` |
| B54 | Versiones desalineadas: el proyecto usa AGP 9.3.1 / Kotlin 2.2.10 y `CLAUDE.md` documenta AGP 9.0.1 / Kotlin 2.1.20; `targetSdk 35` con `compileSdk 36`; hay bumps sin commitear | `libs.versions.toml`, `CLAUDE.md` |
| B55 | Sin tests reales (solo las dos clases de plantilla), pese a declarar junit, espresso y compose-ui-test | `app/src/test`, `androidTest` |

---

## C. Menor (agrupado)

- **Duplicación:** ~200 líneas triplicadas entre los 3 generadores de PDF (constantes, colores, paints, encabezado, pie, `newPage/ensureSpace/hLine/rightAlignAt`, dos truncados divergentes) → propuesta: `PdfPageWriter` + `PdfTheme` + `PdfFormat`. En UI: diálogo de borrado reimplementado 3 veces pese a existir `ConfirmDialog`, bloque de paginación triplicado, `LaunchedEffect(error){snackbar}` repetido en 8 pantallas, 4 sanitizadores decimales idénticos, bloque de descuento copiado 3 veces.
- **Código muerto con consecuencias reales:** en `WorkOrderFormScreen` los launchers y estados de archivos, kilometraje y nivel de combustible existen pero no se usan → **el formulario de orden no permite adjuntar archivos ni editar kilometraje ni combustible**, aunque esos campos se persisten. `StatusChangeDialog` (44 líneas) y `showStatusDialog` nunca se invocan. `availableYears` arranca fijo en 2024, ocultando órdenes anteriores. Además: `statusLog`, `catalogDiagnoses`, `assignMechanic`, `addServiceLine`, `SERVICE_HISTORY_BASE`, `appContext`, `wrapAndDrawText`.
- **Formato:** `String.format("$%.2f")` sin `Locale` conviviendo con `Locale.US` **en la misma tarjeta** (`1234.50` junto a `1.234,50`); saldo negativo impreso como `"Pagado: $-15.00"`; nombres de PDF sin sanear (una placa con `/` lanza `FileNotFoundException`) y comprobantes de comisiones que se sobreescriben si se pagan dos lotes en el mismo minuto.
- **Concurrencia leve:** `SimpleDateFormat` compartido y mutable en los 3 generadores (no es thread-safe y corren en IO); lecturas `_uiState.value` seguidas de `update` en ~7 puntos; `filteredOrders` no se recalcula al cambiar los mapas de cliente/vehículo.
- **Accesibilidad y detalles UX:** tarjetas del Dashboard `clickable` sin rol semántico (TalkBack no las anuncia como botones); `isBiometricAvailable` consultado en cada recomposición; el botón "Usar biométrico" aparece sin sesión guardada y responde "La sesión expiró"; `LazyRow` de fotos sin `key` estable (parpadeo al eliminar); estados de carga inconsistentes entre módulos; `SavedStateHandle` solo en vehículos.
- **Warnings del compilador (22):** 17 × constructor `Locale(String)` deprecado, 3 × iconos no `AutoMirrored`, 1 × `optString(key, null)` (contrato de nulabilidad), 1 × condición siempre verdadera en `UserFormScreen.kt:264`. Además `android.disallowKotlinSourceSets=false` es experimental y romperá al retirarse.
- **Otros:** `LIKE` sin `ESCAPE` ni plegado de acentos ("josé" no encuentra "JOSÉ"); `executeSqlFile` parte sentencias por línea, no con un parser; el diálogo de datos de ejemplo reaparece tras cada actualización de versión; `BackupRepository.DB_VERSION = 4` cuando la base es 2, y nunca se valida al importar; `ServiceHistoryViewModel` duplica los comodines de `LIKE`; ZIPs con PII acumulándose en `cacheDir`; catálogos sin `UNIQUE` en `name`; `scope = emptyList()` en el OAuth de Dropbox en lugar de scopes mínimos; KDoc que dice "21 entidades" (son 22).

---

## D. Orden de trabajo sugerido

**Fase 1 — parar el sangrado (1-2 días)**
1. Bypass biométrico (#1) y Zip Slip (#2) — dos arreglos pequeños y localizados.
2. `allowBackup` / exclusión de `shared_prefs` y `files` (#5) — cambio de XML.
3. `totalExtras` en `sample_data.sql` (#10) — una línea, arregla el primer arranque.
4. Rol en el borrado de órdenes (#3) y `enabled = !isLocked` en pagos (#19).
5. Stock: `increaseStock` al borrar, delta al editar, propagar el fallo de `decreaseStock` (#14, #15, #16).

**Fase 2 — integridad (3-5 días)**
6. Transacciones en las operaciones multi-tabla y en la importación de respaldos (#7, #9, #13, B1).
7. `valueOf` con fallback en importación y converters (#8, B4).
8. Migraciones Room reales y retirar el fallback destructivo (#6).
9. Comisiones: invocar el recálculo e incluir CERRADO en el filtro de pago (#17, #18).
10. Escrituras parciales por `@Query` en lugar de `@Update` de fila completa (#12, B8, B11).

**Fase 3 — reglas y robustez (1 semana)**
11. Mover las invariantes de negocio de la UI a los repositorios (B9), incluido el guard de sesión/rol en el grafo de navegación (#4).
12. Unificar el ciclo de vida de los formularios: carga puntual, `validateForm()` suspend con duplicados, reset de `savedSuccessfully`, flag de éxito para cerrar diálogos (B15-B19).
13. Fotos y adjuntos fuera del hilo principal, con tope real de 6 (#20, B21, B22).
14. Colectores con `Job` y separación de ViewModels de lista (#21, B27-B30).

**Fase 4 — antes de publicar**
15. R8 + firma + reglas ProGuard (B46), `targetSdk 36`, PBKDF2 para contraseñas (B35), respaldo cifrado sin `passwordHash` (B32).
16. Base común de PDF y sus correcciones de paginación/EXIF (B39-B44).
17. Actualizar `CLAUDE.md` (versión de BD, AGP, Kotlin) y añadir los primeros tests (B54, B55).