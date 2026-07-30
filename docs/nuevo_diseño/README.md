# Handoff: Rediseño Serviaux (app Android de control de taller)

## Overview
Rediseño completo de la interfaz de **Serviaux** (app Android nativa, Jetpack Compose + Material 3, offline-first, español). Cubre: login, tablero de inicio, recepción de vehículo en 3 pasos, detalle de orden, cobro, historial de vehículo, inventario de repuestos, pedido sugerido, turnos, formulario de vehículo, catálogos, reportes y comisiones — 17 pantallas.

El objetivo del rediseño: la placa como llave del historial, el dinero siempre visible, un solo patrón de tarjeta, tipografía legible de pie (≥14sp) y objetivos táctiles ≥44dp.

## About the Design Files
Los archivos de este paquete son **referencias de diseño hechas en HTML** — prototipos que muestran el aspecto y comportamiento previstos, NO código para copiar. La tarea es **recrear estas pantallas en el codebase Android existente** (Kotlin, Jetpack Compose, Material 3, minSdk 26) usando sus patrones: tema en `ui/theme/Color.kt` / `Type.kt` / `Theme.kt`, componentes compartidos en `ui/components/`, navegación existente en `ui/navigation/`.

Abrir `Serviaux App.dc.html` en un navegador para ver las 17 pantallas (requiere la carpeta `_ds/` incluida junto al HTML).

## Fidelity
**High-fidelity (hifi).** Colores, tipografía, espaciados, radios y copys son finales. Recrear con fidelidad usando Compose/M3. Las fuentes de marca (ver Tokens) requieren empaquetarse en `res/font/`.

## Decisiones de producto ya tomadas
1. **Apagar el color dinámico** (`dynamicColor = false` en `ui/theme/Theme.kt:79`): la app tiene identidad propia (índigo).
2. **Teléfono vertical** como único formato; fijar orientación en el manifest.
3. La barra inferior pasa a 5 destinos: **Taller · Órdenes · Autos · Refacciones · Más**. El dashboard de módulos desaparece; lo administrativo (comisiones, reportes, catálogos, usuarios, respaldos) vive en "Más" y solo lo ve `ADMIN`.
4. Moneda unificada: `$1.234,56` (separador de miles con punto, decimales con coma — formato Ecuador). Kilometraje `98.400 km`.
5. Repuestos sale de Catálogos: es pestaña propia con stock, movimientos y pedido sugerido.

## Design Tokens → `Color.kt` / `Type.kt`

### Color (esquema claro)
| Token | Hex | Uso |
|---|---|---|
| background | `#F1EFF7` | fondo de pantalla (lila muy claro) |
| surface | `#E4E2F0` | tarjetas y secciones |
| onSurface / text | `#1E1C26` | texto principal |
| divider | `rgba(30,28,38,.14)` | hairlines |
| **accent (índigo)** 100→900 | `#EEEEFF` `#DCDCFB` `#C3C3F5` `#A0A1EC` `#7C7EE0` `#6062CC` `#4A4BAB` `#363688` `#262560` | acción/actividad |
| **accent2 (verde-agua)** 100→900 | `#E3F6F2` `#CBEAE4` `#A9D8CF` `#7FBEB3` `#5DA296` `#47857A` `#35655D` `#244841` `#17302B` | terminado/confirmado |
| **neutral** 100→900 | `#F7F6FB` `#ECEBF3` `#D9D7E4` `#BAB7C8` `#9B98AB` `#7D7A8D` `#605D6F` `#43414F` `#2B2934` | texto secundario, fondos |

**Regla de contraste (obligatoria):** los rellenos sólidos con texto claro usan siempre el paso **700** de la rampa con texto `neutral-100` (`#F7F6FB`). Nunca el paso 500 con texto claro. Fondos tintados: paso 100/200 con texto paso 800.

### Chips de estado de orden (7)
| Estado | Fondo | Texto | Extra |
|---|---|---|---|
| Recibido | neutral-100 | neutral-800 | |
| En Diagnóstico | accent-200 | accent-800 | |
| En Proceso | accent-700 | neutral-100 | sólido = "pasando ahora" |
| En Espera de Repuesto | accent-100 | accent-800 | **borde punteado** accent-600 (único punteado del sistema = "esperando algo de fuera") |
| Listo | accent2-700 | neutral-100 | |
| Entregado | accent2-200 | accent2-800 | |
| Cerrado | neutral-300 | neutral-800 | |

### Semántica de saldo
- Saldado: texto accent2-800 (`#244841`), bold.
- Abonado / sin pagos: texto accent-800 (`#363688`), bold.
- El saldo es el ÚNICO número que se pinta de color.

### Tipografía
- **Display/headings y cifras grandes:** Caprasimo 400 (Google Fonts) — títulos de pantalla 24–27sp, placas 18–24sp, cifras de dinero 32–46sp.
- **Cuerpo:** Figtree 400/600/700 (Google Fonts).
- Mínimos: cuerpo operativo 14sp; secundario 12.5–13.5sp solo para metadatos; etiquetas de sección 12sp MAYÚSCULAS con letter-spacing .06em.
- Ambas fuentes deben empaquetarse en `res/font/`.

### Forma y espaciado
- Radios: tarjetas/secciones **28–32dp**, botones/chips/inputs **pill (999)**, thumbnails 20dp, teléfono de referencia 390×812.
- Padding de pantalla 20dp; interior de tarjeta 14–18dp; gap entre tarjetas 10–12dp.
- Objetivos táctiles ≥44dp (botones principales 48–52dp).
- Elevación: sombra suave (`0 1px 2px` a `0 12px 32px`, tinta 14–22%) — no usar color de contenedor como elevación.

### Iconografía
Lucide (https://lucide.dev) con **stroke-width 2.75**. Sustituye a Material Icons.

## Screens (en `Serviaux App.dc.html`, ids #s1–#s17)
| # | Pantalla | Claves |
|---|---|---|
| s1 | Login | huella primero (botón índigo 54dp), usuario/contraseña como respaldo |
| s2 | Inicio "El taller hoy" | carriles horizontales por estado; el siguiente paso es un BOTÓN en la tarjeta ("Pasar a Listo") — nunca drag; dots de paginación; FAB nueva orden |
| s3 | Más | lista administrativa con pendientes visibles (comisiones por pagar, estado del respaldo); solo ADMIN |
| s4 | Recepción 1/3 | input de placa pill grande; tarjeta "Ya lo conocemos" con historial, garantías vigentes y aceite recomendado |
| s5 | Recepción 2/3 | queja con dictado; AVISO de garantía si el trabajo se repite pronto; tipo de orden como chips; kilometraje/cómo llegó |
| s6 | Recepción 3/3 | fotos (máx 6), checklist "trae consigo" como chips, mecánico; botón "Abrir orden #n" |
| s7 | Detalle de orden | tarjeta índigo con SALDO arriba + acciones (Registrar pago, Pasar a Listo); pestañas Trabajo/Dinero/Fotos/Historial en vez de 10 secciones |
| s8 | Registrar pago | teclado numérico grande, método como chips, "con esto la orden queda saldada" |
| s9 | Historial del vehículo | dueño + 3 KPIs (visitas/gastado/última) + línea de tiempo de servicios + recordatorio de mantenimiento |
| s10 | Refacciones | abre filtrado en "Por acabarse"; alerta de bajo mínimo; tarjeta con stock/mínimo/precio |
| s11 | Ficha de refacción | stepper de stock −/+, precios con margen, "se usó en" (trazabilidad a órdenes) |
| s12 | Pedido sugerido | agrupado por proveedor, calculado con consumo 90 días, enviar por WhatsApp |
| s13 | Turnos | botón "Ya llegó — abrir orden" (prellena cliente+vehículo); confirmar/WhatsApp; los confirmados aparecen en el tablero |
| s14 | Nuevo vehículo | SOLO la placa obligatoria; marca/modelo/año/color arriba; ficha técnica, aceite y fotos plegados |
| s15 | Catálogos | lista con buscador global en vez de 9 pestañas |
| s16 | Reportes | facturado + comparativa, barras por semana (SVG), desglose mano de obra/repuestos/extras, top repuestos |
| s17 | Comisiones | agrupadas por mecánico, checks por línea, barra inferior de selección "Pagar y generar PDF" |

## Interactions & Behavior
- Cambio de estado: botón de siguiente paso en la tarjeta; menú "⋯" abre bottom sheet con los 7 estados (sugerido arriba, avisos como "cobra primero $X"). Sin drag & drop.
- Reglas de negocio a reflejar: CERRADO congela todo (mostrar candado/estado, no solo controles desactivados); Listo/Entregado exigen mecánico asignado; cambios de estado libres en ambos sentidos; máx 6 fotos; comisiones solo de órdenes Listo/Entregado y se pagan solo en Comisiones.
- Retroalimentación unificada: snackbar (eliminar Toasts). Autoguardado con indicación visible ("Guardado ✓").
- Carga: shimmer en todas las listas (generalizar el existente). Vacíos: distinguir "sin datos" de "sin resultados para «x»".
- Confirmación destructiva: UN solo diálogo genérico (mantener escribir nº de orden solo para eliminar orden).
- Placas siempre MAYÚSCULAS (patrón existente de `SearchableDropdown`).

## State Management
Sin cambios de arquitectura: Room + ViewModels existentes. Nuevos derivados: conteo por estado para carriles; "por acabarse" (stock < mínimo); pedido sugerido (consumo 90 días vs mínimo); alerta de garantía (mismo trabajo < 90 días); recordatorio por km.

## Assets
- Fuentes: Caprasimo, Figtree (Google Fonts, licencia OFL).
- Iconos: Lucide (exportar los vectores usados a drawables).
- Logo existente `servielecar_logo.png` se mantiene en Login (dentro del círculo índigo o reemplazando la "S").

## Files
- `Serviaux App.dc.html` — las 17 pantallas finales (índigo). Abrir en navegador.
- `Serviaux Rediseño.dc.html` — historial de exploración (opciones descartadas en terracota + variantes). Solo contexto.
- `_ds/organic-.../styles.css` — hoja base que los HTML referencian (tokens tipográficos/base). Los tokens índigo definitivos están en el `<style>` del propio `Serviaux App.dc.html` y en la tabla de arriba.
