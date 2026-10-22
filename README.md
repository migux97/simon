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
  ui/          pantallas en Jetpack Compose
```
