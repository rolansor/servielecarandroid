# Rediseño Serviaux — Fase 1: tema global y navegación

Implementación parcial del rediseño entregado por diseño en `docs/nuevo_diseño/`
(prototipo `Serviaux App.dc.html`, 17 pantallas). Esta fase cubre **solo estilo y
navegación**: nada que exija tocar el modelo de datos (cero migraciones de Room) y
nada que sea funcionalidad nueva.

## Qué SÍ se hace en esta fase

### 1. Tema índigo (tokens de diseño)
- `ui/theme/Color.kt` reescrito con las rampas del prototipo:
  - **Accent (índigo)** 100→900: `#EEEEFF` … `#262560` — acción / actividad.
  - **Accent2 (verde-agua)** 100→900: `#E3F6F2` … `#17302B` — terminado / confirmado.
  - **Neutral** 100→900: `#F7F6FB` … `#2B2934` — texto secundario, fondos.
  - Fondo `#F1EFF7` (lila muy claro), superficie `#E4E2F0`, texto `#1E1C26`.
- Regla de contraste del prototipo: rellenos sólidos = paso **700** de la rampa con
  texto `neutral-100`; fondos tintados = paso 100/200 con texto paso 800.
- `dynamicColor = false`: la app tiene identidad propia (decisión de producto del brief).
- **Tema claro forzado**: el modo oscuro del rediseño no está diseñado aún (el propio
  prototipo lo lista como pendiente), así que se fuerza claro para no mostrar una
  mezcla vieja/nueva.

### 2. Tipografía
- Fuentes empaquetadas en `res/font/`: `caprasimo.ttf` (Caprasimo 400, títulos y
  cifras) y `figtree.ttf` (Figtree variable 300–900, cuerpo). Licencia OFL.
- `Type.kt`: display/headline/titleLarge en Caprasimo; body/label en Figtree
  400/600/700 vía ejes variables. Cuerpo operativo mínimo 14sp.

### 3. Formas y componentes
- `Shapes.kt` nuevo: tarjetas/secciones 28dp, diálogos 28dp, chips/botones píldora.
- `StatusChip` rediseñado con la semántica fija de los 7 estados:
  índigo sólido = pasando ahora (EN_PROCESO); verde-agua = terminado (LISTO sólido,
  ENTREGADO tintado); borde punteado = esperando algo de fuera (EN_ESPERA_REPUESTO,
  único punteado del sistema); neutro = inerte (RECIBIDO, CERRADO); tintado índigo =
  EN_DIAGNOSTICO.
- `PriorityChip` alineado a las rampas (sin rojos/verdes saturados sueltos).

### 4. Moneda formato Ecuador
- Nueva utilidad `util/Money.kt`: `formatMoney(3450.0)` → `$3.450,00` (miles con
  punto, decimales con coma). Reemplaza los ~53 `String.format("%.2f")` dispersos
  en las pantallas. **Los PDF no se tocan en esta fase** (mantienen su formato para
  no mezclar cambios con los generadores).

### 5. Muere el dashboard de módulos
- `DashboardScreen` (grid de ~20 accesos) se elimina. La ruta `dashboard` pasa a
  mostrar **"El taller hoy"** (`ui/taller/TallerScreen.kt`): cada estado de orden es
  una **sección** vertical con su chip, el conteo y las tarjetas de las órdenes en
  ese estado (placa grande, vehículo, cliente, mecánico). Tocar una tarjeta abre el
  detalle. FAB para nueva orden.
- El diálogo de primer arranque ("¿cargar datos de ejemplo?") se muda tal cual al
  nuevo tablero.

### 6. Navegación de 5 pestañas
- Barra inferior fija: **Taller · Órdenes · Autos · Repuestos · Más**.
- **Taller** = el tablero nuevo. **Órdenes** = lista existente. **Autos** = lista de
  vehículos. **Repuestos** = lista de repuestos (sale de "solo desde dashboard"; el
  handoff la llamaba "Refacciones", pero en Ecuador se dice repuestos y además la
  etiqueta larga se partía en dos líneas en la barra).
- **Más** (`ui/more/MoreScreen.kt`): perfil del usuario + accesos que perdieron su
  botón de dashboard — para todos: Clientes, Turnos, Historial de servicios; solo
  ADMIN: Comisiones, Reportes, Catálogos, Usuarios, Respaldos. Cerrar sesión al pie.
- Los guards existentes no cambian: `Routes.ADMIN_ROUTES` sigue protegiendo las
  rutas de administración en el grafo; "Más" solo oculta/muestra accesos.
- Turnos deja de estar en la barra inferior (decisión del rediseño); vive en "Más" y
  conserva su ruta y pantallas.

### 7. Varios
- Orientación **vertical fija** en el manifest (decisión de producto del brief).
- `AppointmentListScreen`: colores hex hardcodeados reemplazados por el tema.

## Qué NO se hace (y por qué)

| Pendiente | Motivo |
|---|---|
| Recepción en 3 pasos (s4–s6) | Rediseño profundo del formulario de orden; fase propia |
| Detalle de orden con pestañas Trabajo/Dinero/Fotos/Historial (s7) | Ídem; el detalle actual funciona |
| Teclado numérico de pago (s8) | Depende del rediseño del detalle |
| Botón "Pasar a Listo" en las tarjetas del tablero | Exige validaciones (mecánico asignado) con feedback en el tablero; el cambio de estado sigue en el detalle |
| Garantías, aviso de trabajo repetido, aceite recomendado (s4/s5/s9) | Funcionalidad nueva + columnas nuevas → migración |
| Stock mínimo, movimientos, costo/margen, pedido sugerido, proveedores (s10–s12) | Ídem: modelo de datos nuevo |
| Recordatorio de mantenimiento por km (s9) | Funcionalidad nueva |
| Dictado por voz de la queja (s5) | Funcionalidad nueva (speech-to-text) |
| Envío de pedido por WhatsApp (s12) | Depende del pedido sugerido |
| Carril "Por llegar" con turnos confirmados (s2/s13) | Cruce turnos-tablero; fase posterior |
| Buscador global de catálogos (s15) | Rediseño de pantalla completo; fase posterior |
| Reportes con gráficos de barras (s16) | Rediseño de pantalla completo; fase posterior |
| Iconos Lucide con stroke 2.75 | Exige exportar ~60 vectores a drawables; se mantienen Material Icons |
| Modo oscuro | El diseño no existe aún (pendiente declarado del prototipo); tema claro forzado |
| Reglas de negocio, BD, respaldos, PDF | Fuera de alcance: esta fase es solo estilo/navegación. **No se toca ninguna migración ni entidad** |

## Notas para la siguiente fase
- El prototipo pide radios 28–32dp también en inputs (píldora); los `OutlinedTextField`
  se dejaron con el radio del tema (los formularios se rediseñarán en su fase).
- Cuando se diseñe el modo oscuro, quitar el forzado en `Theme.kt` y mapear las rampas.
