# Serviaux - Sistema de Gestion para Taller Automotriz

## Descripcion

App Android para gestion integral de talleres mecanicos automotrices. Permite administrar clientes, vehiculos, ordenes de trabajo, repuestos, pagos y reportes con soporte multi-usuario y roles.

## Caracteristicas principales

- Login con roles (Administrador, Recepcionista, Mecanico)
- CRUD completo de Clientes, Vehiculos, Repuestos, Usuarios
- Ordenes de trabajo con lineas de servicio, repuestos y pagos
- Asignacion de multiples mecanicos por orden con comisiones personalizables (porcentaje o fija)
- Validacion de mecanico asignado para cerrar ordenes (Listo/Entregado)
- Cambio de estado de ordenes mediante chips interactivos (FilterChips)
- Fotos de vehiculos, matriculas y recepcion de ordenes (max 6 por entidad, con preview/reemplazar/eliminar)
- Catalogo de servicios predefinidos con precios por defecto y variantes por tipo de vehiculo
- Flujo de estados de ordenes (Recibido -> En Diagnostico -> En Proceso -> En Espera Repuesto -> Listo -> Entregado)
- Tipo de orden (Servicio Nuevo, Garantia, Retrabajo) y condicion de llegada (Rodando, Grua, Empujado)
- Campos de vehiculo: tipo, combustible, tipo aceite, capacidad aceite, traccion, transmision
- Control de inventario de repuestos con ajuste automatico de stock
- Busqueda de clientes por nombre o cedula en formularios
- Gestion de comisiones: pantalla dedicada (admin) para pago en lote con PDF de resumen
- Estado de comision visible en detalle de orden (badges: Sin comision / Pagada / Pendiente)
- Validacion de descuentos (no puede exceder el costo del servicio o subtotal del repuesto)
- Generacion de reportes PDF (ordenes de trabajo y comisiones) con compartir
- Reportes de ingresos por rango de fechas y repuestos mas usados
- Respaldo completo exportar/importar (ZIP con JSON + fotos) para migrar entre dispositivos
- Respaldo en la nube con Dropbox (OAuth2 PKCE, carpeta por dispositivo, sobrescritura diaria)
- Auditoria de cambios de estado con historial
- Soporte offline (Room database local)
- Tema mecanico/industrial con logo personalizado
- Descuentos en pagos con soporte de multiples metodos de pago

## Tecnologias

- **Kotlin** 2.1.20
- **Jetpack Compose** + Material 3
- **Room Database** (SQLite) con KSP
- **Navigation Compose**
- **Coil 3** para carga de imagenes
- **Dropbox SDK** (dropbox-core-sdk + dropbox-android-sdk 7.0.0) para respaldos en la nube
- **Arquitectura MVVM**
- **Manual DI** (AppContainer pattern)
- **AGP** 9.0.1, compileSdk 36, minSdk 26

## Arquitectura

```
app/src/main/java/com/example/serviaux/
├── data/
│   ├── entity/              # Entidades Room (User, Customer, Vehicle, WorkOrder, Part, CatalogService, WorkOrderMechanic, etc.)
│   ├── dao/                 # Data Access Objects
│   ├── Converters.kt        # TypeConverters para enums
│   └── ServiauxDatabase.kt  # Base de datos con seed data (version 1)
├── repository/              # Repositorios (Auth, Customer, Vehicle, Part, WorkOrder, Catalog, Commission, Backup)
├── di/
│   └── AppContainer.kt      # Inyeccion de dependencias manual
├── util/
│   ├── SecurityUtils.kt     # Hash SHA-256 + salt para contraseñas
│   ├── SessionManager.kt    # Manejo de sesion con StateFlow
│   ├── PhotoUtils.kt        # Utilidades para fotos (crear archivo, URI, parsear rutas)
│   ├── PdfReportGenerator.kt    # Generacion de PDF de ordenes de trabajo
│   ├── CommissionPdfGenerator.kt # Generacion de PDF de comisiones pagadas
│   ├── ShareUtils.kt           # Compartir archivos via Intent
│   └── DropboxHelper.kt       # Integracion con Dropbox (OAuth2, upload, download, list)
├── ui/
│   ├── theme/               # Tema mecanico (colores industriales, logo)
│   ├── components/          # Componentes reutilizables (SearchableDropdown, StatusChip, etc.)
│   ├── navigation/          # Routes + NavGraph
│   ├── auth/                # LoginScreen
│   ├── dashboard/           # Dashboard con accesos rapidos
│   ├── customers/           # CRUD Clientes
│   ├── vehicles/            # CRUD Vehiculos (con fotos, tipo aceite, combustible)
│   ├── workorders/          # Ordenes de trabajo (fotos recepcion, mecanicos, comisiones)
│   ├── commissions/         # Gestion de comisiones (pago en lote, resumen, PDF)
│   ├── parts/               # CRUD Repuestos
│   ├── users/               # Gestion de usuarios (solo Admin)
│   ├── reports/             # Reportes e ingresos
│   ├── settings/            # Configuracion de catalogos
│   └── backup/              # Exportar/importar respaldos
├── ServiauxApp.kt           # Application class
└── MainActivity.kt          # Activity principal

app/src/main/assets/seed/
├── seed_data.sql            # Datos esenciales (admin + catalogos) - siempre se carga
└── sample_data.sql          # Datos de ejemplo opcionales (usuarios, clientes, vehiculos, repuestos, ordenes)
```

## Roles y Permisos

| Funcionalidad | Admin | Recepcionista | Mecanico |
|---|---|---|---|
| Dashboard | Si | Si | Si |
| Clientes (CRUD) | Si | Si | Solo lectura |
| Vehiculos (CRUD) | Si | Si | Solo lectura |
| Ordenes de trabajo | Si | Si | Ver asignadas |
| Repuestos | Si | Si | Solo lectura |
| Usuarios | Si | No | No |
| Reportes | Si | Si | No |
| Comisiones | Si | No | No |
| Respaldos | Si | No | No |

## Datos de prueba (Seed)

La app usa dos archivos SQL en `assets/seed/`:

### seed_data.sql (siempre se carga al crear la BD)
- **Admin:** servielecar/f4d3s2a1
- **Cliente fallback:** CONSUMIDOR FINAL (id=1)
- **Catalogos:** 92 marcas, 1020 modelos, 32 colores, 11 tipos vehiculo, 34 tipos aceite, 135 servicios, 54 diagnosticos, 26 motivos, 36 marcas repuestos, 13 accesorios

### sample_data.sql (opcional, cargado si el usuario elige "Cargar ejemplos" en el primer arranque)
- **Usuarios:** nicopla/f4d3s2a1 (Recepcionista), smimos/f4d3s2a1 (Mecanico), marciano/f4d3s2a1 (Mecanico)
- **Clientes:** 10 clientes de ejemplo
- **Vehiculos:** 10 vehiculos vinculados a clientes
- **Repuestos:** 20 productos basicos de taller
- **Ordenes:** 3 ordenes de trabajo con servicios, repuestos y pagos

## Catalogo de Servicios Predefinidos

- Mantenimiento Preventivo
- Sistema de Frenos
- Motor y Sistema de Combustible
- Suspension y Direccion
- Sistema Electrico y Aire Acondicionado
- Otros Servicios

Cada servicio se duplica para los tipos de vehiculo SEDAN, SUV y CAMIONETA.

## Modelo de datos

Las entidades principales y sus relaciones:

- **Customer** -> tiene muchos **Vehicle** (CASCADE on delete)
- **Vehicle** -> tiene muchas **WorkOrder**, campos: photoPaths, vehicleType, fuelType, oilType, oilCapacity
- **WorkOrder** -> tiene muchas **ServiceLine**, **WorkOrderPart**, **Payment**, **StatusLog**, **WorkOrderMechanic**, campos: photoPaths, orderType, arrivalCondition
- **WorkOrderMechanic** -> vincula mecanico a orden con commissionType, commissionValue, commissionAmount, commissionPaid, paidAt
- **Part** -> referenciado por **WorkOrderPart** (gestion de stock automatica)
- **CatalogService** -> servicios predefinidos con categoria, precio y tipo vehiculo

## Como ejecutar

1. Abrir en Android Studio (Ladybug o superior)
2. Sincronizar Gradle
3. Ejecutar en emulador o dispositivo (API 26+)
4. Login con: `servielecar` / `f4d3s2a1`

## Flujo de estados de ordenes

```
RECIBIDO → EN_DIAGNOSTICO → EN_PROCESO → EN_ESPERA_REPUESTO → LISTO → ENTREGADO
                                                                ↘ CANCELADO
```

Cada transicion de estado queda registrada en la tabla `StatusLog` con fecha, usuario y comentario opcional. Las ordenes no pueden marcarse como LISTO o ENTREGADO sin al menos un mecanico asignado.

## Respaldo en Dropbox

La app permite vincular una cuenta de Dropbox para subir y descargar respaldos en la nube, complementando la exportacion/importacion local por ZIP.

### Autenticacion

- Se usa **OAuth2 PKCE** (solo app key, sin app secret), por lo que no se requiere un servidor backend.
- Al vincular, se abre el navegador del sistema para autorizar la app. Al regresar, la credencial se almacena como JSON serializado (`DbxCredential`) en `SharedPreferences` (`dropbox_prefs`).

### Estructura de carpetas

La app de Dropbox crea automaticamente la carpeta sandbox `/Aplicaciones/serviaux/`. Dentro de ella:

```
/Aplicaciones/serviaux/
├── Samsung Galaxy S24/
│   └── serviaux_backup_2026-03-03.zip
├── Xiaomi Redmi Note 12/
│   └── serviaux_backup_2026-03-02.zip
└── ...
```

- Cada dispositivo crea una subcarpeta con su nombre (fabricante + modelo).
- Los archivos ZIP se nombran por fecha (`serviaux_backup_YYYY-MM-dd.zip`).
- Si se sube mas de una vez el mismo dia, el archivo anterior se **sobrescribe** (`WriteMode.OVERWRITE`).

### Funcionalidad

- **Subir:** Exporta las categorias seleccionadas a ZIP y lo sube a la carpeta del dispositivo.
- **Descargar:** Lista todos los respaldos de todos los dispositivos y permite seleccionar uno para restaurar (reutiliza el flujo de importacion con checklist de categorias).
- **Desvincular:** Elimina la credencial almacenada localmente.
