# Serviaux — Especificación de interfaz actual (brief para rediseño)

**Versión del documento:** 1.0 · 29/07/2026
**Fuente:** código de la app en `main` (commit `1553ac3`). Todo lo descrito aquí está verificado contra el código, no es aspiracional.
**Destinatario:** equipo de diseño que va a reformatear la interfaz gráfica.

---

## 1. Qué es el producto

Serviaux es una app **Android nativa** de gestión integral para un taller automotriz (Servielecar). Cubre el ciclo completo: turno → recepción del vehículo → orden de trabajo → servicios/repuestos/extras → pagos → entrega → comisiones de mecánicos → reportes y respaldos.

**No es una app de consumidor final.** Los usuarios son el personal del taller, en tres roles:

| Rol | Nombre visible | Uso previsto |
|---|---|---|
| `ADMIN` | Administrador | Acceso total: usuarios, comisiones, reportes, catálogos, respaldos |
| `RECEPCIONISTA` | Recepcionista | Clientes, vehículos, turnos, órdenes |
| `MECANICO` | Mecánico | Consulta de órdenes |

**Contexto de uso real (importante para el rediseño):**
- Teléfono en mano, de pie, dentro del taller. Manos posiblemente sucias o con guantes.
- Sesiones cortas e interrumpidas: se abre una orden, se agrega un repuesto, se cierra.
- Mucha entrada de datos numéricos (montos, cantidades, kilometraje) y mucha lectura de listas.
- Un mismo dispositivo puede ser compartido por varias personas del taller.
- Trabaja **100 % offline** (base de datos local Room). Lo único que usa red es el respaldo a Dropbox.
- Idioma único: **español**. No hay soporte multiidioma ni está previsto.

---

## 2. Restricciones técnicas que condicionan el diseño

Esto define qué se puede pedir sin reescribir la app.

| Aspecto | Situación actual | Implicación para diseño |
|---|---|---|
| Framework UI | Jetpack Compose + **Material 3** | Los componentes M3 (Card, Chip, TopAppBar, FAB, BottomSheet, TabRow…) son "gratis"; salir de M3 cuesta código propio |
| SDK | `minSdk 26` (Android 8.0), `compileSdk 36` | Blur, efectos avanzados y algunas APIs modernas no están disponibles en toda la base |
| Tamaños de pantalla | **Sin ninguna adaptación**: no se usa `WindowSizeClass`, no hay layouts alternativos ni `values-sw600dp` | Hoy la app está diseñada solo para teléfono en vertical. Si se quiere tablet/horizontal, es trabajo nuevo (decisión abierta, ver §11) |
| Orientación | Libre (el manifest no la fija) | En horizontal la UI actual simplemente se estira; nadie la diseñó así |
| Tema oscuro | Existe esquema claro y oscuro completo en Compose, se elige por preferencia del sistema. Pero el tema de la **ventana** (`res/values/themes.xml`) hereda de `Theme.Material.Light` y no hay `values-night/`: el fondo de ventana es blanco siempre → destello blanco al abrir la app en modo oscuro | **Cualquier propuesta debe entregarse en ambos modos**, incluido el color de arranque |
| Color dinámico | `dynamicColor = true` (`ui/theme/Theme.kt:79`) | Ver §3.1 — es el punto más importante del sistema visual |
| Textos | **Hardcodeados en el código**, no en `strings.xml` (solo contiene `app_name`) | Cambiar cualquier texto es cambiar código Kotlin. Un rediseño de copy es viable pero toca ~22 archivos |
| Tipografía | Escala M3 personalizada, **sin familia propia**: usa la del sistema (Roboto) | Una fuente de marca requiere empaquetar el archivo de fuente (viable, +peso APK) |
| Iconografía | 100 % Material Icons (`Icons.Default.*`) | Un set de iconos propio implica exportar vectores; es viable pero es trabajo nuevo |
| Imágenes | Coil 3. Fotos de vehículos/órdenes se guardan como archivos locales | — |
| Assets de marca | Solo el logo `servielecar_logo.png` (5 densidades) y `ic_serviaux_logo.xml` | No hay manual de marca, ni paleta oficial documentada, ni ilustraciones |
| Edge-to-edge | Activado (`MainActivity.kt:23`) | La UI dibuja bajo las barras del sistema; ojo con los márgenes seguros |

---

## 3. Sistema visual actual

### 3.1 Color — atención, hay un problema de fondo

Existe una paleta de marca "taller mecánico" definida en `ui/theme/Color.kt`:

| Rol | Claro | Oscuro | Concepto |
|---|---|---|---|
| Primary | `#1B3A4B` azul acero | `#8EAEBF` | acero de herramienta |
| Secondary | `#E67E22` ámbar | `#F5C77E` | chispa / señalización |
| Tertiary | `#7F8C8D` gris metálico | `#B8C4C5` | metal cepillado |
| Error | `#E74C3C` rojo freno | `#F5A8A0` | — |
| Background | `#F5F5F5` | `#1A1A2E` | — |
| Surface | `#FFFFFF` | `#16213E` | — |

**Pero esa paleta casi nunca se ve.** `ServiauxTheme` tiene `dynamicColor = true`, así que en **Android 12 y superior** (la mayoría de los dispositivos) el esquema se genera desde el fondo de pantalla del usuario y la paleta de marca queda ignorada por completo. Solo se aplica en Android 8–11.

> **Decisión requerida de diseño:** ¿la app tiene identidad visual propia (desactivar color dinámico) o se adapta al sistema del usuario (mantenerlo)? Todo lo demás del sistema de color depende de esta respuesta. Cambiarlo es una línea de código.

**Colores semánticos fijos** (definidos fuera del esquema M3, no cambian con tema ni con color dinámico):

| Estado de orden | Color | | Prioridad | Color |
|---|---|---|---|---|
| Recibido | `#2196F3` azul | | Alta | `#E74C3C` |
| En Diagnóstico | `#E67E22` naranja | | Media | `#E67E22` |
| En Proceso | `#F1C40F` amarillo | | Baja | `#27AE60` |
| En Espera de Repuesto | `#E91E63` rosa | | | |
| Listo | `#27AE60` verde | | | |
| Entregado | `#2ECC71` verde claro | | | |
| Cerrado | `#95A5A6` gris | | | |

Se usan como fondo al 15 % de opacidad + borde al 30 % + texto al 100 % del mismo color (`ui/components/CommonComponents.kt:82-130`). **Dos de estos chips no cumplen contraste AA**, medido: "En Proceso" (amarillo `#F1C40F`) da ≈ **1.7:1** y "Entregado" (`#2ECC71`) ≈ **1.9:1**, frente al 4.5:1 exigido. El patrón en sí —texto del mismo color que el fondo al 15 %— es la causa, así que conviene rediseñar el chip, no solo retocar los colores.

Además, estos 10 colores **son idénticos en tema claro y oscuro** (no tienen variante), lo que agrava el contraste en modo oscuro.

También hay color con significado en el saldo de las órdenes: verde = saldado, ámbar = abonado parcialmente, rojo = sin pagos (`ui/workorders/WorkOrderListScreen.kt:254-259`).

### 3.2 Tipografía

Escala M3 completa personalizada en `ui/theme/Type.kt`, familia del sistema:

| Estilo | Tamaño / peso | Uso predominante |
|---|---|---|
| headlineLarge | 32sp Bold | números de KPI en el dashboard (forzado a 36sp) |
| headlineSmall | 24sp SemiBold | saludo "Hola, {usuario}" |
| titleLarge | 22sp SemiBold | poco usado |
| titleMedium | 16sp Medium | títulos de sección, títulos de tarjeta |
| titleSmall | 14sp Medium | nombre de orden/mecánico en tarjetas |
| bodyMedium | 14sp | cuerpo, filas etiqueta-valor |
| bodySmall | 12sp | metadatos: fechas, subtítulos, quejas |
| labelMedium | 12sp Medium | texto de chips y etiquetas de tarjeta |
| labelSmall | 11sp Medium | chips de cambio de estado en el detalle |

Observación: gran parte de la información operativa (fecha, cliente, placa, queja del cliente, abono, saldo) se muestra en **12sp**, que es pequeño para lectura de pie en un taller.

### 3.3 Forma, espaciado y elevación

- **Sin tokens definidos.** No hay `dimens.xml`, ni objeto de espaciado en Compose. Los valores están escritos a mano en cada pantalla.
- Espaciados usados de hecho: `4 / 6 / 8 / 12 / 16 / 24 / 32 dp`. El padding de pantalla es casi siempre `16dp`, el interior de tarjeta `16dp`, la separación entre tarjetas `4dp` u `8dp` o `12dp` según pantalla.
- Radios: los de M3 por defecto para Card; `RoundedCornerShape(16.dp)` en chips de estado; `8dp` en thumbnails de fotos; `12dp` en la barra lateral de color de las tarjetas de orden; `20-24dp` en el logo de login.
- Elevación: se usa color de contenedor en lugar de sombra (`surfaceContainerLow`, `surfaceContainerHigh`, `surfaceVariant`, `primaryContainer`, `secondaryContainer`, `tertiaryContainer`). No hay una regla de cuál va dónde: la elección varía por pantalla.
- Un patrón visual propio y reconocible: **barra vertical de 4dp con el color del estado** en el borde izquierdo de las tarjetas de orden (dashboard y lista de órdenes).

### 3.4 Componentes compartidos (todo lo que hoy está centralizado)

`ui/components/`:

| Componente | Qué es |
|---|---|
| `ServiauxSearchBar` | `OutlinedTextField` con lupa, placeholder "Buscar…" |
| `StatusChip` | badge de estado de orden (color semántico, 16dp de radio) |
| `PriorityChip` | badge de prioridad |
| `SectionTitle` | título 16sp + divisor horizontal |
| `InfoRow` | fila etiqueta (40 %) / valor (60 %), con icono opcional |
| `EmptyState` | icono 64dp al 50 % de opacidad + mensaje |
| `ConfirmDialog` | diálogo genérico Confirmar / Cancelar |
| `SearchableDropdown` | campo con autocompletado, máx. 3 sugerencias, fuerza MAYÚSCULAS, filtra por nombre y subtítulo |
| `ShimmerLoadingList` | placeholder animado de carga de listas |

**Todo lo demás es local a cada pantalla.** Las tarjetas de lista, las cabeceras, los diálogos de formulario, las tarjetas de KPI y de módulo están reimplementadas dentro de cada archivo. De ahí buena parte de las inconsistencias del §10.

---

## 4. Arquitectura de información

### 4.1 Mapa de navegación

```
Login  ──(éxito)──►  Dashboard  ◄── raíz de todo, con barra inferior
                        │
   ┌────────────────────┼───────────────────────────────────────┐
   │  Barra inferior (5 destinos, visible solo en estas 5)      │
   │  Inicio · Órdenes · Turnos · Clientes · Vehículos          │
   └────────────────────┼───────────────────────────────────────┘
                        │
  Clientes ──► Detalle cliente ──► Formulario cliente
                    ├──► Vehículo (detalle)
                    └──► Historial de servicios (del cliente)
  Vehículos ──► Detalle vehículo ──► Formulario vehículo
                    ├──► Detalle de orden
                    └──► Detalle de cliente (propietario)
  Órdenes  ──► Detalle de orden ──► Editar orden
                    └──► 6 diálogos: estado, mecánico, servicio, repuesto, extra, pago
  Turnos   ──► Formulario de turno
                    └──► Convertir en orden (prellena cliente + vehículo)
  Repuestos ──► Formulario de repuesto
  Historial de servicios ──► Detalle de orden

  Solo ADMIN:  Usuarios ──► Formulario usuario
               Comisiones (pestañas Pendientes / Historial → resumen de pago + PDF)
               Reportes
               Catálogos (9 pestañas)
               Respaldos (exportar / restaurar / Dropbox)
```

- **24 rutas registradas, 22 pantallas distintas** (el formulario de orden se reutiliza para crear, editar y convertir turno).
- Transiciones: deslizamiento horizontal + fundido, 300 ms, en todas las pantallas.
- La barra inferior aparece **solo** en Inicio, Órdenes, Turnos, Clientes y Vehículos. En las otras 17 pantallas se navega con la flecha de retroceso de la barra superior.
- El Dashboard es el hub real: contiene atajos a los 11 módulos, y 6 de ellos no tienen otra entrada.

### 4.2 Visibilidad por rol

El filtro por rol se aplica **solo ocultando elementos del Dashboard** (`ui/dashboard/DashboardScreen.kt:435-441`): los módulos Usuarios, Comisiones, Reportes, Catálogos y Respaldos y el icono de configuración solo se muestran a `ADMIN`. Las rutas en sí no verifican el rol.

> Para diseño: si el rediseño introduce otra forma de acceso (buscador global, menú lateral, accesos recientes), hay que decidir explícitamente qué ve cada rol.

---

## 5. Inventario de pantallas

Resumen y luego detalle de las que tienen más carga.

| # | Pantalla | Rol | Barra superior | Acción principal | Complejidad |
|---|---|---|---|---|---|
| 1 | Login | todos | ninguna | Iniciar sesión / biométrico | baja |
| 2 | Dashboard | todos | `primaryContainer`, 40dp | ir a módulo | **alta** |
| 3 | Lista de clientes | todos | estándar | FAB nuevo | media |
| 4 | Detalle de cliente | todos | título = nombre | editar | media |
| 5 | Formulario de cliente | todos | estándar | guardar | media |
| 6 | Lista de vehículos | todos | estándar | FAB nuevo | media |
| 7 | Detalle de vehículo | todos | título = placa | editar | media |
| 8 | Formulario de vehículo | todos | estándar | guardar | **alta** (717 líneas) |
| 9 | Lista de órdenes | todos | `LargeTopAppBar` colapsable | FAB nueva | **alta** |
| 10 | Detalle de orden | todos | estándar | 10 secciones + 6 diálogos | **la más compleja (2319 líneas)** |
| 11 | Formulario de orden | todos | estándar | crear/editar | alta |
| 12 | Lista de turnos | todos | estándar | FAB nuevo | media |
| 13 | Formulario de turno | todos | estándar | guardar | media |
| 14 | Lista de repuestos | todos | estándar | FAB nuevo | media |
| 15 | Formulario de repuesto | todos | estándar | guardar | media |
| 16 | Historial de servicios | todos | estándar | buscar/filtrar | media |
| 17 | Lista de usuarios | ADMIN | estándar | FAB nuevo | baja |
| 18 | Formulario de usuario | ADMIN | estándar | guardar | media |
| 19 | Comisiones | ADMIN | `primaryContainer` | pagar seleccionadas | **alta** |
| 20 | Reportes | ADMIN | estándar | elegir rango | baja |
| 21 | Catálogos | ADMIN | estándar | FAB contextual por pestaña | **alta** (9 pestañas) |
| 22 | Respaldos | ADMIN | `primaryContainer` | exportar / restaurar | alta |

### 5.1 Login

Columna centrada, sin barra superior: logo 180dp con esquinas redondeadas (24dp) → subtítulo "Gestión de Taller Automotriz" → campo Usuario → campo Contraseña con ojo de visibilidad → botón "Iniciar Sesión" de ancho completo (muestra spinner al cargar) → botón secundario "Usar biométrico" (solo si el dispositivo lo soporta) → mensaje de error en rojo debajo.

Estado especial: al arrancar con sesión guardada muestra logo 150dp + spinner + "Verificando identidad…" y lanza el prompt biométrico del sistema.

### 5.2 Dashboard

La pantalla con más superficie de diseño. De arriba a abajo, todo en una columna con scroll vertical:

1. **Cabecera de usuario**: avatar circular de 48dp con iniciales sobre color primario + "Hola, {nombre}" (24sp Bold) + badge de rol + fecha del día ("29 jul 2026").
2. **"Órdenes por Estado"**: 6 tarjetas de KPI en 3 filas de 2. Cada una: número gigante (36sp Bold) + etiqueta, con **degradado lineal del color del estado** (15 % → 5 %). Cada tarjeta navega a la lista filtrada por ese estado. *Nota: no incluye "Cerrado".*
3. **"Acciones Rápidas"**: tarjeta de "Turnos del día" (solo si hay turnos; `tertiaryContainer`, con conteo grande a la derecha) + 3 tarjetas cuadradas: Cliente, Vehículo, Orden (`secondaryContainer`).
4. **"Órdenes Activas Recientes"** (si hay): tarjetas con barra lateral de color de estado, "Orden #id", "cliente - placa" y chip de estado.
5. **"Módulos"**: cuadrícula de 3 columnas con 6 u 11 tarjetas según el rol (icono 32dp + etiqueta, `surfaceContainerHigh`).

Barra superior: título "Serviaux", 40dp de alto, `primaryContainer`; acciones: engranaje (solo admin) y cerrar sesión.

Diálogo de primer arranque: "Datos iniciales" → "Cargar ejemplos" / "Empezar de cero".

**Problema de diseño evidente:** hay 4 estilos de tarjeta distintos apilados en una sola pantalla (degradado, `tertiaryContainer`, `secondaryContainer`, `surfaceContainerHigh`) y 20 elementos tocables sin jerarquía clara. Es el mejor candidato a rediseño de alto impacto.

### 5.3 Lista de órdenes

- `LargeTopAppBar` colapsable (la única de la app), colapsa a 40dp.
- Buscador "Buscar por cliente o placa…" con botón de limpiar.
- **Fila de filtros compacta:** botón tonal "Filtros (n)" + chips de filtros activos con scroll horizontal (año siempre visible; estado y estado de pago se pueden quitar con la X del chip).
- Los filtros completos viven en un **bottom sheet**: año (chips de años disponibles), estado (Todas + los 7 estados), estado de pago, y "Limpiar todo".
- Tarjeta de orden: barra lateral de estado 4dp · "Orden #id" + chip de estado · chip de prioridad + tipo de orden + fecha · queja del cliente (máx. 2 líneas) · "Total: $x" · "Abono: $x" y "Saldo: $x" (saldo coloreado según pago).
- Estados: shimmer mientras carga · `EmptyState` "No se encontraron órdenes" / "Sin resultados para «x»".
- FAB circular para nueva orden. Snackbar para mensajes.

### 5.4 Detalle de orden — la pantalla crítica

2319 líneas, 10 secciones en scroll y 6 diálogos modales. Es donde el personal pasa la mayor parte del tiempo.

Barra superior: "Orden #id" + acciones: editar, eliminar, compartir reporte (PDF).

Secciones en orden:

1. **Estado y prioridad**: chips de estado y prioridad + fila de `FilterChip` con los 7 estados para cambiar inline (etiquetas en 11sp).
2. **"Información General"**: filas etiqueta-valor — fecha de ingreso, cliente, vehículo, tipo de orden, queja del cliente, condición de llegada, mecánico asignado.
3. **"Checklist"**: accesorios marcados en la recepción ("Sin items marcados" si vacío).
4. **"Datos del Proceso"** (campos editables con autoguardado): kilometraje de entrada, nivel de combustible (Vacío / ¼ / ½ / ¾ / Lleno), nota de entrega, factura, notas.
5. **"Fotos (n)"**: `LazyRow` de miniaturas + botones de cámara y galería. Máx. 6. Al tocar una: diálogo con vista previa / reemplazar / eliminar.
6. **Archivos adjuntos**: lista con abrir y eliminar; "No hay archivos adjuntos".
7. **"Mecánicos"**: cada uno con su tipo de comisión (Fija / % / Sin comisión), monto, y badge de estado de comisión (Pagada / Pendiente / Sin comisión). Botón de agregar y de eliminar.
8. **"Servicios / Mano de Obra"**, **"Repuestos Utilizados"**, **"Extras"**: tres listas de estructura casi idéntica — descripción, cantidad × precio, descuento ("Desc: -$x"), subtotal, y acciones Editar / Eliminar. Cada una con su botón de agregar y su estado vacío.
9. **"Resumen"**: Mano de Obra, Repuestos, Extras → **TOTAL** destacado → Pagado, Descuentos → **SALDO**.
10. **"Pagos"**: lista de pagos con método, monto, descuento y notas.

Diálogos: Cambiar Estado · Agregar Mecánico (con tipo y valor de comisión) · Agregar/Editar Servicio (descripción con contador 200 caracteres, costo, casilla "Aplicar descuento") · Agregar/Editar Repuesto (búsqueda mín. 3 caracteres, cantidad, precio unitario, descuento) · Agregar/Editar Extra (categorías: Ferretería, Tercerizado, Repuesto externo, Herramienta, Otro) · Registrar Pago (monto, descuento, método, notas).

**Cuando la orden está en CERRADO, absolutamente todo se bloquea** (ver §8). Hoy eso se comunica solo desactivando controles.

**Problemas de diseño:** scroll interminable sin índice ni anclas; tres secciones visualmente idénticas que se confunden entre sí; el resumen económico —lo que el usuario más consulta— está al final; densidad muy alta con textos de 11–12sp; seis diálogos con maquetación distinta entre sí.

### 5.5 Comisiones (solo admin)

Pestañas **Pendientes / Historial**. En Pendientes, las comisiones se agrupan por mecánico con una tarjeta-cabecera que tiene casilla de "seleccionar todo" del mecánico y su total. Al seleccionar aparece una **barra inferior** con "n seleccionadas", el total y el botón "Pagar seleccionados". Tras pagar, la pantalla se reemplaza por un **resumen de pago** con opción de generar y compartir el PDF.

### 5.6 Catálogos (solo admin)

`ScrollableTabRow` con **9 pestañas**: Marcas, Colores, Repuestos, Servicios, Tipos Veh., Accesorios, Aceites, Motivos, Diagnósticos. El FAB agrega según la pestaña activa. Marcas y Motivos son jerárquicos (marca → modelos, motivo → diagnósticos); Servicios tiene precio y variantes por tipo de vehículo; el resto son listas planas de nombre con editar/eliminar.

**Problema:** 9 pestañas es demasiado para descubrir contenido en un teléfono; las dos primeras y la última quedan fuera de vista.

### 5.7 Respaldos (solo admin)

Columna con: tarjeta "Datos actuales" (conteo de registros por tabla) → tarjeta **Exportar** (icono de nube 48dp, casillas de categorías, botón "Exportar Todo"/"Exportar Selección", botón "Exportar por Año") → tarjeta **Restaurar** (selector de archivo .zip, con diálogo de selección de categorías) → sección de Dropbox (vincular cuenta, subir, listar y descargar respaldos por dispositivo).

### 5.8 Reportes (solo admin)

Rango de fechas con dos campos de solo lectura que abren el selector de fecha nativo de Android → tarjeta "Total Facturado" (`primaryContainer`, cifra en 32sp) → tarjeta "Órdenes en el Período" → tarjeta "Top Repuestos" (lista numerada con unidades).

**Sin ningún gráfico.** Es la pantalla con más espacio para aportar valor visual.

### 5.9 Listas y formularios (patrón repetido)

Las 6 listas simples (clientes, vehículos, repuestos, usuarios, turnos, historial) comparten estructura: barra superior con flecha + buscador + lista de tarjetas + FAB + estado vacío. Difieren en detalles menores (espaciado, si el borrado pide confirmación, si hay chips de filtro), y esas diferencias son accidentales, no intencionadas.

Los formularios comparten: barra superior "Nuevo X"/"Editar X" + columna de campos con scroll + botón de guardar. Campos obligatorios marcados con `*`. Ejemplos de carga:
- **Cliente:** nombre completo\*, teléfono\*, tipo de documento + número, email, dirección, notas.
- **Vehículo (el más pesado):** placa\*, marca, modelo, versión, año, color, cilindraje, número de motor, VIN, tracción (4x2/4x4 como chips), transmisión (Manual/Automático), combustible (chips, por defecto Gasolina), tipo de aceite (autocompletado), capacidad de aceite (medios galones hasta 10), kilometraje, notas y hasta 6 fotos.
- **Usuario:** nombre completo\*, usuario\*, rol, tipo de comisión + valor, contraseña\*.
- **Repuesto:** nombre\*, descripción, código, marca, costo unitario, precio de venta, stock actual.
- **Turno:** vehículo\*, fecha programada\*, hora, notas, estado.

---

## 6. Patrones de interacción actuales

| Patrón | Dónde | Consistencia |
|---|---|---|
| FAB circular para "crear" | 6 listas | consistente |
| Barra superior de 40dp de alto | 14 pantallas (`expandedHeight = 40.dp`) | consistente pero **no estándar M3** (64dp) |
| Barra superior teñida de `primaryContainer` | solo Dashboard, Comisiones, Respaldos | **inconsistente** |
| `LargeTopAppBar` colapsable | solo Órdenes | **inconsistente** |
| Bottom sheet de filtros | solo Órdenes | el resto usa chips inline |
| Chips de filtro inline | Turnos, Historial | — |
| Diálogo modal para agregar/editar sublíneas | Detalle de orden, Catálogos | consistente en concepto, distinto en maquetación |
| Confirmación de borrado | Clientes, Vehículos, Turnos, Catálogos, Órdenes (esta pide **escribir el número de orden**) | 5 variantes distintas |
| Retroalimentación de éxito/error | **Snackbar** en 8 pantallas y **Toast** en otras | **inconsistente** |
| Carga | `ShimmerLoadingList` en Órdenes; `CircularProgressIndicator` en 20 pantallas; texto "Cargando…" en algunas | **inconsistente** |
| Autoguardado | campos de proceso en Detalle de orden | sin indicación visual de "guardado" |
| Selectores de fecha/hora | diálogos nativos de Android, no M3 Compose | visualmente ajenos al resto |
| Entrada forzada a MAYÚSCULAS | `SearchableDropdown` (placas, marcas, modelos) | intencional |

---

## 7. Diccionario de dominio (etiquetas visibles exactas)

Estos textos aparecen en la interfaz y deben mantenerse o cambiarse con criterio explícito.

**Estados de orden** (7): Recibido · En Diagnóstico · En Proceso · En Espera de Repuesto · Listo · Entregado · Cerrado
**Prioridad** (3): Baja · Media · Alta
**Tipo de orden** (6): Servicio Nuevo · Correctivo · Preventivo · Garantía · Diagnóstico · Revisión
**Condición de llegada** (5): Llegó rodando · Llegó en grúa · Encendido / No rueda · No enciende · Llegó empujado
**Estado de turno** (4): Pendiente · Confirmado · Cancelado · Convertido
**Método de pago** (6): Efectivo · Transferencia · Tarjeta de Crédito · Tarjeta de Débito · Mixta · Otro
**Tipo de comisión** (3): No comisiona · Por trabajo ($) · Porcentaje (%)
**Nivel de combustible** (5): Vacío · ¼ · ½ · ¾ · Lleno
**Categorías de extra** (5): Ferretería · Tercerizado · Repuesto externo · Herramienta · Otro

Formatos actuales:
- **Moneda:** `$1234.56` — sin separador de miles, símbolo `$` pegado, formateado con `Locale.US` en unos sitios y sin locale en otros. Candidato claro a unificar en el rediseño.
- **Fecha:** `dd/MM/yyyy`, y `dd/MM/yyyy HH:mm` en el detalle de orden. En el dashboard `dd MMM yyyy` en español.
- **Kilometraje:** `{n} km` · **Cilindraje:** `{n} L` · **Cantidad:** `{n} uds`

---

## 8. Reglas de negocio que la interfaz debe reflejar

Son restricciones reales del sistema; cualquier propuesta debe seguir comunicándolas.

1. **CERRADO congela la orden.** Es el único estado que bloquea ediciones, y bloquea *todo*: servicios, repuestos, extras, pagos, fotos, archivos, mecánicos y campos del formulario. Todos los demás estados —incluido ENTREGADO— permiten editar libremente.
2. **No se puede marcar LISTO ni ENTREGADO sin al menos un mecánico asignado.**
3. **Los cambios de estado son libres en ambos sentidos** (incluso retroceder desde ENTREGADO) para permitir correcciones.
4. **Descuentos con techo:** el de un servicio no puede superar su costo de mano de obra; el de un repuesto no puede superar su subtotal.
5. **Máximo 6 fotos** por vehículo y 6 por orden.
6. **Comisiones:** cada mecánico de una orden puede tener comisión fija o porcentual; el estado se muestra como badge y **no se edita desde el detalle** — el pago se hace exclusivamente en la pantalla Comisiones, por lotes y con PDF.
7. Solo las comisiones de órdenes en LISTO o ENTREGADO aparecen para pago.
8. El historial de cambios de estado **se registra pero ya no se muestra** en la interfaz. (Si el rediseño lo quiere exponer, el dato existe.)
9. Los módulos administrativos solo se ofrecen al rol ADMIN.

---

## 9. Estados de datos a diseñar

Para cada lista y pantalla el rediseño necesita cubrir:

| Estado | Situación actual |
|---|---|
| Cargando | shimmer solo en Órdenes; spinner centrado en el resto; a veces texto "Cargando…" |
| Vacío por falta de datos | `EmptyState` con icono e mensaje en algunas listas; simple texto gris en otras |
| Vacío por búsqueda/filtro | solo Órdenes lo distingue ("Sin resultados para «x»") |
| Error | Snackbar o Toast efímeros; no hay estado de error persistente en pantalla |
| Operación en curso | botón con spinner interno (exportar, login, pagar comisiones) |
| Sin permiso (rol) | no existe: simplemente no se muestra el acceso |
| Sin conexión | no existe (la app es offline; solo Dropbox necesita red y falla con mensaje) |
| Primer arranque | diálogo "Datos iniciales": cargar ejemplos o empezar de cero |

---

## 10. Deuda de diseño identificada (insumo para el rediseño)

Ordenada por impacto. Esto es lo que en la práctica hace que la app se sienta inconsistente.

**Alto impacto**
1. **La identidad visual no se ve** en la mayoría de dispositivos por el color dinámico activado (§3.1). Decisión pendiente y de una línea de código.
2. **Cuatro estilos de tarjeta compitiendo** en el Dashboard, y sin jerarquía entre 20 elementos tocables.
3. **El Detalle de orden es un scroll de 10 secciones** sin navegación interna, con el resumen económico al final y tres bloques visualmente indistinguibles (servicios / repuestos / extras).
4. **Densidad tipográfica excesiva**: información operativa clave en 11–12sp, en un contexto de uso de pie y con poca luz.
5. **Contraste insuficiente** en al menos el estado "En Proceso" (amarillo) y en general en el patrón de chip con texto del mismo color del fondo al 15 %.
6. **Barras superiores forzadas a 40dp**, por debajo del estándar M3, con tres variantes distintas de estilo entre pantallas.

**Impacto medio**

6b. **Doble margen superior por insets duplicados.** La app dibuja bajo las barras del sistema y anida dos `Scaffold` (el de `MainActivity` y el de cada pantalla) sin consumir los insets, así que el espacio sobre la barra superior es el doble del que debería, y sobra aire encima de la barra inferior. Es un defecto de implementación ya identificado, pero explica parte de la sensación de "maquetación descuadrada" en las capturas actuales: conviene verlo corregido antes de tomar la app como referencia visual.

6c. **El campo con autocompletado no reabre sus sugerencias.** En `SearchableDropdown` (placas, marcas, modelos, clientes) el menú solo aparece mientras se teclea: si el usuario vuelve a tocar un campo que ya tiene texto, no ve opciones. Afecta a los formularios de vehículo y orden, que son los de mayor uso.

7. **Retroalimentación mezclada**: Snackbar y Toast conviviendo; sin un patrón único de éxito/error.
8. **Estados de carga y vacío distintos** en cada pantalla; el shimmer existe pero solo se usa en una.
9. **Cinco variantes de confirmación de borrado**, incluyendo una que exige escribir el número de orden.
10. **9 pestañas en Catálogos**, con descubrimiento pobre.
11. **Selectores de fecha y hora nativos** rompiendo el lenguaje visual de Compose.
12. **Sin tokens de espaciado, radio ni elevación**: cada pantalla decide, y por eso las listas "casi iguales" no lo son.
13. **Reportes sin visualización de datos**: tres tarjetas de números y una lista.
14. **Sin indicación de guardado** en los campos con autoguardado del detalle de orden.

**Impacto menor / a decidir**
15. **Sin diseño para tablet ni horizontal** (la app rota pero nadie la diseñó así).
16. Formato de moneda inconsistente y sin separadores de miles.
17. El chip de estado "Cerrado" reutiliza el color gris que en el código todavía se llama "Cancelado".
18. Los KPI del Dashboard no cubren el estado Cerrado.
19. Todos los textos viven en el código, sin `strings.xml`; muchos escritos con secuencias `\uXXXX`. No bloquea el rediseño visual, pero encarece cualquier revisión de copy.

---

## 11. Qué necesitamos de diseño

**Decisiones que solo diseño puede tomar**
1. ¿Identidad de marca propia o color dinámico del sistema? (§3.1)
2. Escala de color de estados accesible (7 estados de orden + 3 prioridades + semántica de saldo), válida en claro y oscuro.
3. Densidad y escala tipográfica objetivo para uso de pie en taller: ¿subimos el cuerpo a 14sp mínimo?
4. Un único patrón de tarjeta de lista, con variantes documentadas (orden / cliente / vehículo / repuesto / turno).
5. Reestructuración del Detalle de orden: ¿pestañas, secciones colapsables, o una hoja de resumen fija?
6. Reestructuración del Dashboard: qué se prioriza y qué se elimina.
7. ¿Se diseña para tablet/horizontal, o se declara explícitamente teléfono-vertical y se fija la orientación?

**Entregables útiles, en orden de utilidad para implementación**
1. **Tokens** — color (roles M3: primary, secondary, tertiary, surface, containers, outline + los semánticos de estado), tipografía (mapeada a la escala M3 de §3.2), espaciado, radios, elevación. Con nombres, para traducirlos directamente a `Color.kt` / `Type.kt` / un objeto de espaciado.
2. **Biblioteca de componentes** — los 9 componentes compartidos del §3.4 rediseñados, más los que hoy están duplicados por pantalla: tarjeta de lista, cabecera de sección, fila etiqueta-valor, chip de estado, diálogo de formulario, barra superior, estado vacío, estado de carga.
3. **Pantallas prioritarias en alta fidelidad** (claro y oscuro): Dashboard, Lista de órdenes, Detalle de orden, un formulario representativo (Vehículo), Comisiones.
4. **Patrones transversales**: confirmación destructiva, retroalimentación de éxito/error, carga, vacío, guardado automático, bloqueo por orden cerrada.
5. **Las 17 pantallas restantes** pueden quedar como aplicación de los patrones anteriores, sin maqueta individual.

**Formato**: Figma con componentes y variantes, y los tokens como estilos/variables. Material 3 como base ahorra la mayor parte del trabajo de implementación; cualquier componente fuera de M3 conviene marcarlo como tal para poder estimarlo.

---

## Anexo A — Rutas y archivos

| Pantalla | Ruta | Archivo |
|---|---|---|
| Login | `login` | `ui/auth/LoginScreen.kt` |
| Dashboard | `dashboard` | `ui/dashboard/DashboardScreen.kt` |
| Clientes | `customers` / `customers/{id}` / `customers/form` | `ui/customers/` |
| Vehículos | `vehicles` / `vehicles/{id}` / `vehicles/form` | `ui/vehicles/` |
| Órdenes | `workorders?status=` / `workorders/{id}` / `workorders/form` / `workorders/edit/{id}` | `ui/workorders/` |
| Turnos | `appointments` / `appointments/form` | `ui/appointments/` |
| Repuestos | `parts` / `parts/form` | `ui/parts/` |
| Usuarios | `users` / `users/form` | `ui/users/` |
| Comisiones | `commissions` | `ui/commissions/CommissionScreen.kt` |
| Reportes | `reports` | `ui/reports/ReportsScreen.kt` |
| Catálogos | `catalog_settings` | `ui/settings/CatalogSettingsScreen.kt` |
| Respaldos | `backup` | `ui/backup/BackupScreen.kt` |
| Historial | `service_history?customerId=` | `ui/history/ServiceHistoryScreen.kt` |

Sistema visual: `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` · Componentes: `ui/components/` · Navegación: `ui/navigation/`

## Anexo B — Assets existentes

- `res/drawable-{m,h,xh,xxh,xxxh}dpi/servielecar_logo.png` — logo del taller, usado solo en Login.
- `res/drawable/ic_serviaux_logo.xml` — vector, no usado en la interfaz.
- `res/mipmap-*/ic_launcher.png` + `ic_launcher_round.png` — icono de la app (plantilla por defecto de Android Studio, candidato a rediseño).
- `res/values/colors.xml` — colores de la plantilla original (purple/teal), **no se usan**.
- `res/values/themes.xml` — `Theme.Serviaux` hereda de `Theme.Material.Light.NoActionBar`; solo se usa para la ventana previa a Compose.
- Sin ilustraciones, sin fuentes propias, sin iconos personalizados.