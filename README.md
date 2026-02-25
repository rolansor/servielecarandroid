# Serviaux - Sistema de Gestion para Taller Automotriz

## Descripcion

App Android para gestion integral de talleres mecanicos automotrices. Permite administrar clientes, vehiculos, ordenes de trabajo, repuestos, pagos y reportes con soporte multi-usuario y roles.

## Caracteristicas principales

- Login con roles (Administrador, Recepcionista, Mecanico)
- CRUD completo de Clientes, Vehiculos, Repuestos, Usuarios
- Ordenes de trabajo con lineas de servicio, repuestos y pagos
- Flujo de estados de ordenes (Recibido -> Diagnostico -> Cotizacion -> Aprobado -> En Progreso -> Completado -> Entregado)
- Control de inventario de repuestos con ajuste automatico de stock
- Reportes de ingresos por rango de fechas y repuestos mas usados
- Auditoria de cambios de estado con historial
- Soporte offline (Room database local)
- Tema mecanico/industrial con logo personalizado

## Tecnologias

- **Kotlin** 2.0.21
- **Jetpack Compose** + Material 3
- **Room Database** (SQLite) con KSP
- **Navigation Compose**
- **Arquitectura MVVM**
- **Manual DI** (AppContainer pattern)
- **AGP** 9.0.1, compileSdk 35, minSdk 26

## Arquitectura

```
app/src/main/java/com/example/serviaux/
├── data/
│   ├── entity/              # Entidades Room (User, Customer, Vehicle, WorkOrder, Part, etc.)
│   ├── dao/                 # Data Access Objects
│   ├── Converters.kt        # TypeConverters para enums
│   └── ServiauxDatabase.kt  # Base de datos con seed data
├── repository/              # Repositorios (Auth, Customer, Vehicle, Part, WorkOrder)
├── di/
│   └── AppContainer.kt      # Inyeccion de dependencias manual
├── util/
│   ├── SecurityUtils.kt     # Hash SHA-256 + salt para contraseñas
│   └── SessionManager.kt    # Manejo de sesion con StateFlow
├── ui/
│   ├── theme/               # Tema mecanico (colores industriales, logo)
│   ├── components/          # Componentes reutilizables
│   ├── navigation/          # Routes + NavGraph
│   ├── auth/                # LoginScreen
│   ├── dashboard/           # Dashboard con accesos rapidos
│   ├── customers/           # CRUD Clientes
│   ├── vehicles/            # CRUD Vehiculos
│   ├── workorders/          # Ordenes de trabajo (el modulo mas complejo)
│   ├── parts/               # CRUD Repuestos
│   ├── users/               # Gestion de usuarios (solo Admin)
│   └── reports/             # Reportes e ingresos
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

## Datos de prueba (Seed)

La app viene con datos precargados:

- **Usuarios:** admin/serviaux2024 (Admin), maria/serviaux2024 (Recepcionista), jose/serviaux2024 (Mecanico), pedro/serviaux2024 (Mecanico)
- **Clientes:** 5 clientes con datos realistas
- **Vehiculos:** 8 vehiculos (Toyota, Hyundai, Nissan, Mitsubishi, etc.)
- **Repuestos:** 15 repuestos con codigos, marcas y precios
- **Ordenes:** 3 ordenes de ejemplo en diferentes estados

## Modelo de datos

Las entidades principales y sus relaciones:

- **Customer** -> tiene muchos **Vehicle** (CASCADE on delete)
- **Vehicle** -> tiene muchas **WorkOrder**
- **WorkOrder** -> tiene muchas **ServiceLine**, **WorkOrderPart**, **Payment**, **StatusLog**
- **Part** -> referenciado por **WorkOrderPart** (gestion de stock automatica)

## Como ejecutar

1. Abrir en Android Studio (Ladybug o superior)
2. Sincronizar Gradle
3. Ejecutar en emulador o dispositivo (API 26+)
4. Login con: `admin` / `serviaux2024`

## Flujo de estados de ordenes

```
RECIBIDO → EN_DIAGNOSTICO → COTIZADO → APROBADO → EN_PROGRESO → COMPLETADO → ENTREGADO
```

Cada transicion de estado queda registrada en la tabla `StatusLog` con fecha, usuario y comentario opcional, proporcionando un historial de auditoria completo para cada orden de trabajo.
