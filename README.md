# Serviaux - Sistema de Gestion para Taller Automotriz

## Descripcion

App Android para gestion integral de talleres mecanicos automotrices. Permite administrar clientes, vehiculos, ordenes de trabajo, repuestos, pagos y reportes con soporte multi-usuario y roles.

## Caracteristicas principales

- Login con roles (Administrador, Recepcionista, Mecanico)
- CRUD completo de Clientes, Vehiculos, Repuestos, Usuarios
- Ordenes de trabajo con lineas de servicio, repuestos y pagos
- Fotos de vehiculos y ordenes de trabajo (max 6 por entidad, tomadas con camara)
- Catalogo de servicios predefinidos con precios por defecto
- Flujo de estados de ordenes (Recibido -> Diagnostico -> Cotizacion -> Aprobado -> En Progreso -> Completado -> Entregado)
- Control de inventario de repuestos con ajuste automatico de stock
- Generacion de reportes PDF (ordenes de trabajo) con compartir
- Reportes de ingresos por rango de fechas y repuestos mas usados
- Respaldo completo exportar/importar (ZIP con JSON + fotos) para migrar entre dispositivos
- Auditoria de cambios de estado con historial
- Soporte offline (Room database local)
- Tema mecanico/industrial con logo personalizado

## Tecnologias

- **Kotlin** 2.1.20
- **Jetpack Compose** + Material 3
- **Room Database** (SQLite) con KSP
- **Navigation Compose**
- **Coil 3** para carga de imagenes
- **Arquitectura MVVM**
- **Manual DI** (AppContainer pattern)
- **AGP** 9.0.1, compileSdk 36, minSdk 26

## Arquitectura

```
app/src/main/java/com/example/serviaux/
├── data/
│   ├── entity/              # Entidades Room (User, Customer, Vehicle, WorkOrder, Part, CatalogService, etc.)
│   ├── dao/                 # Data Access Objects
│   ├── Converters.kt        # TypeConverters para enums
│   └── ServiauxDatabase.kt  # Base de datos con seed data (version 5)
├── repository/              # Repositorios (Auth, Customer, Vehicle, Part, WorkOrder, Catalog, Backup)
├── di/
│   └── AppContainer.kt      # Inyeccion de dependencias manual
├── util/
│   ├── SecurityUtils.kt     # Hash SHA-256 + salt para contraseñas
│   ├── SessionManager.kt    # Manejo de sesion con StateFlow
│   ├── PhotoUtils.kt        # Utilidades para fotos (crear archivo, URI, parsear rutas)
│   ├── PdfReportGenerator.kt # Generacion de PDF de ordenes de trabajo
│   └── ShareUtils.kt        # Compartir archivos via Intent
├── ui/
│   ├── theme/               # Tema mecanico (colores industriales, logo)
│   ├── components/          # Componentes reutilizables
│   ├── navigation/          # Routes + NavGraph
│   ├── auth/                # LoginScreen
│   ├── dashboard/           # Dashboard con accesos rapidos
│   ├── customers/           # CRUD Clientes
│   ├── vehicles/            # CRUD Vehiculos (con fotos)
│   ├── workorders/          # Ordenes de trabajo (con fotos y servicios predefinidos)
│   ├── parts/               # CRUD Repuestos
│   ├── users/               # Gestion de usuarios (solo Admin)
│   ├── reports/             # Reportes e ingresos
│   ├── settings/            # Configuracion de catalogos
│   └── backup/              # Exportar/importar respaldos
├── ServiauxApp.kt           # Application class
└── MainActivity.kt          # Activity principal
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
| Respaldos | Si | No | No |

## Datos de prueba (Seed)

La app viene con datos precargados:

- **Usuarios:** servielecar/f4d3s2a1 (Admin), maria/f4d3s2a1 (Recepcionista), jose/f4d3s2a1 (Mecanico), pedro/f4d3s2a1 (Mecanico)
- **Clientes:** 5 clientes con datos realistas
- **Vehiculos:** 8 vehiculos (Toyota, Hyundai, Nissan, Mitsubishi, etc.)
- **Repuestos:** 15 repuestos con codigos, marcas y precios
- **Ordenes:** 3 ordenes de ejemplo en diferentes estados
- **Servicios predefinidos:** ~40 servicios organizados por categoria con precio base de $10.00

## Catalogo de Servicios Predefinidos

- Mantenimiento Preventivo (9 servicios)
- Sistema de Frenos (5 servicios)
- Motor y Sistema de Combustible (7 servicios)
- Suspension y Direccion (5 servicios)
- Sistema Electrico y Aire Acondicionado (5 servicios)
- Otros Servicios (7 servicios)

## Modelo de datos

Las entidades principales y sus relaciones:

- **Customer** -> tiene muchos **Vehicle** (CASCADE on delete)
- **Vehicle** -> tiene muchas **WorkOrder**, campo `photoPaths` para fotos
- **WorkOrder** -> tiene muchas **ServiceLine**, **WorkOrderPart**, **Payment**, **StatusLog**, campo `photoPaths` para fotos
- **Part** -> referenciado por **WorkOrderPart** (gestion de stock automatica)
- **CatalogService** -> servicios predefinidos con categoria y precio por defecto

## Como ejecutar

1. Abrir en Android Studio (Ladybug o superior)
2. Sincronizar Gradle
3. Ejecutar en emulador o dispositivo (API 26+)
4. Login con: `servielecar` / `f4d3s2a1`

## Flujo de estados de ordenes

```
RECIBIDO → EN_DIAGNOSTICO → COTIZADO → APROBADO → EN_PROGRESO → COMPLETADO → ENTREGADO
```

Cada transicion de estado queda registrada en la tabla `StatusLog` con fecha, usuario y comentario opcional, proporcionando un historial de auditoria completo para cada orden de trabajo.
