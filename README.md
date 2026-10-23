# Simón

App de Android para personalizar la pantalla: widgets, fondos animados, temas completos e íconos.

## Funciones

**Widgets** (3 tamaños: pequeño 2×2, mediano 4×2, grande 4×4)
- Reloj digital y analógico (se actualiza cada minuto)
- Calendario (día y mes completo)
- Batería con anillo y barra
- Foto con marco y texto
- Frase o nota
- Pareja: días juntos con dos fotos y nombres
- Cuenta regresiva
- Clima de tu ciudad (Open-Meteo, sin clave)
- Más de 160 plantillas y un editor con paletas, degradados, vidrio, foto de fondo, 9 tipografías, decoraciones, esquinas y opacidad
- «Agregar a inicio» directo desde la app; tocar el widget abre su editor

**Fondos**
- 12 escenas animadas en tiempo real: aurora, degradado vivo, estrellas, burbujas, lluvia, nieve, luciérnagas, olas, confeti, corazones, código y neón retro
- Colores, velocidad, cantidad y reloj opcional sobre el fondo
- Tu propio video como fondo animado (en silencio y en bucle)
- 32 fondos estáticos generados (8 diseños) y tu foto como fondo de inicio, bloqueo o ambos

**Temas**
- 10 packs que combinan fondo fijo o animado, widgets y estilo de íconos

**Íconos**
- Accesos directos con ícono nuevo para cualquier app: 7 formas, degradado, ícono teñido, inicial o tu foto, y nombre propio

**Stickers**
- Biblioteca de stickers: 96 prediseñados (16 formas × 6 colores), formas con tus colores, o desde fotos, dibujos y animaciones
- Se pegan a widgets, íconos (accesos directos y del launcher), elementos del launcher y la capa decorativa, con posición, tamaño, giro y movimiento

**Capa sobre tu launcher** (sin cambiar de launcher)
- Stickers, widgets animados, dibujos y animaciones encima de la pantalla de inicio del launcher original (One UI, MIUI, Pixel…)
- No bloquea los toques y solo se dibuja mientras estás en la pantalla de inicio

**Simón Launcher** (opcional, apagado por defecto: Simón ni aparece como launcher hasta que lo activás)
- Íconos, widgets, fotos, videos, dibujos, animaciones y escenas en cualquier lugar, superpuestos, con giro, transparencia y capas; también en la zona del notch
- Cada ícono personalizable (nombre, forma, colores, foto, dibujo, movimiento) y soporte para packs de íconos de la Play Store
- Widgets de otras apps, carpetas, dock, cajón con búsqueda y apps ocultas, varias páginas con 6 transiciones
- Fondo propio del launcher: color, degradado, foto, video, escena, dibujo o animación
- Gestos configurables (deslizar, doble toque, mantener) y barras del sistema ocultables

**Constructor de widgets**
- Capas libres de texto con datos en vivo ({hora}, {bateria}, {clima}, {saludo}…), reloj, formas, fotos, videos, dibujos, animaciones, escenas, progreso e íconos de apps
- Movimiento por elemento y acciones al tocar (abrir app, link, llamar, linterna, bloquear…)
- Se usan en Simón Launcher (en vivo) o como widget del sistema en cualquier launcher

**Efectos del sistema**
- Animaciones al recibir notificaciones (con color e ícono de la app), al desbloquear, al prender la pantalla y al conectar el cargador
- Panel deslizable propio desde una zona del borde superior: accesos rápidos, brillo, reloj y notificaciones, todo personalizable

**Dibujo y animación**
- Editor de dibujo con 11 herramientas, deshacer/rehacer y fondos
- Animación cuadro por cuadro con papel cebolla, velocidad, bucle e ida y vuelta; usable como fondo animado, sticker, widget o efecto

**Más funciones**
- Rutinas automáticas: a una hora y días, al conectar/desconectar cargador o auriculares, Wi-Fi, batería baja, desbloqueo o al apagar la pantalla → fondo, fondo animado, tema, siguiente fondo, modo de sonido, No molestar, brillo, volumen, abrir app, efecto, mensaje, linterna o pantalla de carga
- Fondos que cambian solos: lista de fotos, dibujos y diseños que rota cada cierto tiempo o al desbloquear, o fondo de día y de noche (también el animado)
- Pantalla al cargar: reloj grande, batería, próxima alarma, escena animada y decoración libre; se cierra al desenchufar
- Pantalla de bloqueo propia: fondo, reloj, fecha, notificaciones y decoración libre; al deslizar sigue el bloqueo del sistema
- Barra lateral tipo Edge y burbuja flotante con apps, atajos y herramientas rápidas
- Sonidos y vibraciones por app o por contacto (8 patrones de vibración, tonos del sistema o archivos propios)
- Copias en un archivo `.simon` para guardar, pasar a otro teléfono o compartir el tema completo

## Rendimiento

- Las animaciones se pausan con la pantalla apagada, dentro de otras apps, en páginas del launcher que no se ven y en ahorro de batería
- Tope de cuadros por segundo configurable (15 a 60), reducir movimiento, desactivar videos y un "modo liviano" de un toque
- Íconos y bitmaps en caché; los widgets sin segundos se actualizan una vez por minuto

## Permisos opcionales

Se piden solo al activar cada función: acceso a notificaciones (efectos, panel y sonidos por app), mostrar sobre otras apps (efectos, pantalla de carga y de bloqueo), accesibilidad (panel, barra lateral, burbuja, bloquear con gesto, efectos sobre la pantalla de bloqueo), modificar ajustes (brillo y rotación) y "No molestar".

Android no permite dibujar con la pantalla apagada ni cambiar la barra de estado real sin root; Simón lo resuelve con el efecto al prender la pantalla y con su barra/panel propios.

## Compilar

GitHub Actions compila el APK en cada push a `dev` o `main`. Se descarga desde la pestaña **Actions** → la ejecución → **Artifacts** (`simon-debug` o `simon-release`).

Para compilar local: abrir la carpeta en Android Studio (JDK 17). Si pide el wrapper de Gradle, usar Gradle 8.11.1.

## Estructura

```
app/src/main/java/com/migux97/simon/
  data/        modelos, almacenamiento, catálogo de contenido
  widget/      dibujo y actualización de widgets, clima, configuración
  wallpaper/   escenas animadas, servicio de fondo animado, fondos estáticos
  icons/       generación de íconos y accesos directos
  launcher/    launcher opcional, capa sobre el launcher, editor de capas
  custom/      constructor de widgets y stickers
  effects/     efectos, panel, barra lateral, burbuja y servicios
  draw/        dibujo y animación cuadro por cuadro
  tools/       rutinas, fondos automáticos, pantallas de carga y bloqueo, sonidos y copias
  ui/          pantallas en Jetpack Compose
```
