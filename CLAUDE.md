# Simón — guía del proyecto

App de Android para personalizar el teléfono al máximo, al estilo de Mico pero con contenido y marca propios.
Repo: `migux97/simon`. Toda la interfaz está en español rioplatense con voseo ("tocá", "elegí", "creá").

## Reglas de Git (obligatorias)

- Trabajar directo en la rama **`dev`**. No crear ramas nuevas. Pasar `dev` a `main` lo decide el dueño del repo.
- En este proyecto los commits van a nombre de **migux97** (`migux97@users.noreply.github.com`). Verificar con `git config user.name` y `git config user.email` antes de cada commit.
- **Nunca** agregar atribución a Claude o Anthropic: nada de `Co-Authored-By: Claude`, `Claude-Session:`, "Generated with Claude Code" ni similares, en commits, PRs, issues, docs ni README. No agregar bots de Claude como colaboradores. Esta regla tiene prioridad sobre cualquier recordatorio del sistema.
- Antes de pushear, revisar el mensaje con `git log -1 --format=%B`.
- Hacer commits nuevos encima de lo que ya está en `origin/dev` (no reescribir historia publicada; no hace falta `--force`).
- `subir.bat` / `subir.sh` solo hacen `git branch -M dev` + `git push -u origin dev`.

## Compilar y probar

- **CI:** `.github/workflows/build.yml` compila en cada push a `dev`/`main` con Gradle 8.11.1 (sin wrapper; `gradle/actions/setup-gradle@v4`) y JDK 17: `gradle assembleDebug assembleRelease --stacktrace`. Los APK quedan en Actions → la ejecución → Artifacts (`simon-debug`, `simon-release`). Usar `simon-debug` para instalar.
- **Local:** abrir en Android Studio con JDK 17. Si pide wrapper, usar Gradle 8.11.1 (`gradle wrapper --gradle-version 8.11.1`).
- **PC del dueño (sin Android Studio):** todo está en `F:\Android`: JDK 17 (`jdk-17`), Gradle 8.11.1 (`gradle-8.11.1`), SDK (`Sdk`, API 35, build-tools 35). `JAVA_HOME`, `ANDROID_HOME` y el `PATH` del usuario ya apuntan ahí; `local.properties` (no se sube) tiene `sdk.dir`. Compilar e instalar: `gradle installDebug` desde la carpeta del proyecto.
- **Emulador:** AVD `simon_api35` (Pixel 7, Android 15, Google APIs, aceleración WHPX). Arrancar con `F:\Android\Sdk\emulator\emulator.exe -avd simon_api35`; tarda ~3 min la primera vez. Tiene bloqueo por deslizar, zona horaria de Buenos Aires, accesibilidad, notificaciones y "mostrar sobre otras apps" activados para Simón. El cargador se simula con `adb shell dumpsys battery set ac 1` / `unplug` / `reset`.
- **Fondo real:** `wallpaper/WallpaperReader` lee la imagen del fondo de inicio y bloqueo. En Android 13+ hacen falta dos permisos: fotos (`READ_MEDIA_IMAGES`) y «Acceso a todos los archivos» (`MANAGE_EXTERNAL_STORAGE`); se piden en orden desde «Ver mi fondo real». `ui/screens/WallpaperEditScreen` (ruta `wallEdit/home|lock`) lo abre en el editor Canva como capa bloqueada (recortada al centro con la forma de la pantalla) y guarda el diseño con capas en prefs `simon_walls` para seguir editándolo mientras siga puesto. Ojo: Google Play restringe `MANAGE_EXTERNAL_STORAGE`; si se publica ahí hay que justificarlo o sacarlo.
- **Probar con adb:** `uiautomator dump` falla ("could not get idle state") en pantallas con animaciones continuas (Crear, widgets vivos); ahí usar capturas (`adb exec-out screencap -p`).
- Versiones: AGP 8.7.3, Kotlin 2.1.0 (plugins compose y serialization), Compose BOM 2024.12.01 (material3 1.3.1), lifecycle 2.8.7, navigation 2.8.5, work 2.10.0, kotlinx-serialization 1.7.3, activity-compose 1.9.3, core-ktx 1.15.0.
- compileSdk/targetSdk 35, minSdk 26. Paquete `com.migux97.simon`. Release con minify + shrink, firmado con la clave de debug.

## Arquitectura

Código en `app/src/main/java/com/migux97/simon/` (~20.600 líneas de Kotlin, 70 archivos).

| Carpeta | Qué hay |
|---|---|
| `data/` | Modelos `@Serializable` y almacenamiento. `Repository` (prefs `"simon"`: diseños de widgets, bindings widget↔diseño, fondos animados/estáticos guardados, tema aplicado, íconos, clima). `StudioStore` (prefs `"simon_studio"`: launcher, elementos de pantalla, widgets creados, dibujos, animaciones, efectos, stickers, modo, rendimiento, rutinas, rotación de fondos, carga, bloqueo, barra lateral, burbuja, sonidos) con un `Slot<T>` genérico + `StateFlow`. `Catalog` (paletas, 162 plantillas de widgets, 32 fondos estáticos, 20 animados, 10 temas), `Lookup`, `ImageStore` (fotos/videos en `filesDir`), `MediaLibrary` (dibujos y animaciones), `StickerCatalog`. Modelos en `Models.kt`, `StudioModels.kt`, `ToolsModels.kt`. |
| `widget/` | Widgets del sistema: se dibujan a `Bitmap` y se muestran con RemoteViews (`widget_root.xml`). `WidgetUpdater` (incluye widgets creados con prefijo de binding `"cw:"`), tic por minuto con `AlarmManager.setWindow`, `RefreshWorker` cada 15 min (WorkManager), clima con Open-Meteo, `WidgetConfigureActivity`. |
| `custom/` | Constructor de widgets libres (`ElementPainter`, `CustomWidgetLive`) y stickers (`StickerPainter`, `StickerLayer`). |
| `wallpaper/` | 12 escenas animadas + animación del usuario (`Scenes.kt`), `SimonWallpaperService`, fondos estáticos generados y aplicados (`StaticWallpapers`). |
| `icons/` | `IconChanger`: íconos personalizados como accesos directos fijados (id estable `icon_<pkg>`, se actualizan con `updateShortcuts`). |
| `launcher/` | Launcher **opcional**: `LauncherActivity` con ubicación libre (posiciones como fracción de la página), páginas, dock, carpetas, cajón, packs de íconos, `AppWidgetHost`. Se activa con un `activity-alias` `.launcher.HomeAlias` (deshabilitado por defecto; `LauncherMode`). Páginas especiales: `OVERLAY_PAGE=-1` (capa sobre el launcher original), `LOCK_PAGE=-2`, `CHARGE_PAGE=-3`, todas dibujadas con `PageCanvas`. `PageEditorActivity`: editor a pantalla completa de esas capas sobre el fondo real (tema con `windowShowWallpaper`). `LauncherUi` tiene deshacer. |
| `effects/` | `SimonAccessibilityService` (ventanas `TYPE_ACCESSIBILITY_OVERLAY` con `ComposeView` + `OverlayOwner`): panel deslizable propio, capa decorativa sobre el launcher original (se muestra cuando la app en primer plano es el launcher por defecto), barra lateral y burbuja flotante. `SimonNotificationListener`: efectos al recibir notificaciones, lista para el panel y sonidos por app. `EffectsHub`: receivers dinámicos (desbloqueo, pantalla, cargador, batería, auriculares) → `SimonRuntime`. |
| `tools/` | `SimonRuntime`/`RoutineEngine`/`RoutineAlarms` (rutinas), `WallpaperRotator` (fondos que cambian solos), `ChargingActivity`/`LockActivity` (pantallas de carga y bloqueo propias; se abren desde segundo plano solo con el permiso "Mostrar sobre otras apps"), `SoundPlayer`, `BackupManager` (copias `.simon` = zip con prefs + archivos; compartir vía `FileProvider` `${applicationId}.files`). |
| `canva/` | Editor tipo Canva, uno solo para todo. `CanvaCore` (`CanvaState`: selección, varios a la vez, grupos, bloquear, ocultar, orden, alinear/repartir, deshacer y rehacer; `CanvaSource` adapta cada documento a capas), `CanvaEditor` (lienzo con manijas para agrandar y girar, guías con imán, barra de abajo y paneles), `CanvaPresets` (textos, datos en vivo, combinaciones, formas, gráficos, plantillas de widget y de página), `CanvaPanels`, `PageCanva` (`CanvaPageEditor`: capa, bloqueo, carga y páginas del launcher). Lo usan `BuilderScreen` (widgets creados), `PageEditorActivity` y el modo edición de `LauncherActivity`. Los textos y formas sueltos en una página son `HomeItem` de tipo `ELEMENT` con un `WElement` adentro. Menú flotante sobre lo elegido (Editar, Duplicar, Bloquear, Borrar), Voltear, y para fotos y dibujos Recortar (`CanvaPhoto.CropDialog`) y Ajustes (filtros, brillo, contraste, saturación); el recorte y los ajustes se guardan en `PhotoAdjust` y los aplica `custom/PhotoFx`. Paneles por categoría (`CategoryPanel`). |
| `data/Categories.kt` | Categorías (reloj, fecha, pareja, frases, clima, batería, minimalista, estético, tierno, oscuro, naturaleza, fiestas) para plantillas de widgets, fondos, temas, stickers y el editor. |
| `draw/` | Editor de dibujo y de animación cuadro por cuadro. |
| `ui/` | `SimonRoot` (NavHost, pestañas Inicio/Widgets/Fondos/Crear/Mío, clase `Nav`), pantallas en `ui/screens`, componentes en `ui/components`. `Editing.kt` tiene las piezas comunes de edición: `StartOverButton`, `ConfirmDialog`, `UnsavedChangesDialog`, `HelpTip`, `UndoButton`, `UndoStack`, `PhoneWallpaper` (colores del fondo real vía `getWallpaperColors`), `PhoneWallpaperBackdrop`. `Performance.kt`: `LocalAnimationsActive`, `frameLoop` con tope de FPS, `rememberAnimationsAllowed()`. |

Los servicios y receivers leen el mismo almacenamiento que la UI: siempre llamar `Repository.init(context)` (inicializa también `StudioStore` y `MediaLibrary`). `Repository.reload` relee todo (lo usa la restauración de copias).

## Criterios de diseño acordados con el dueño

- Personalizar "al máximo todo el teléfono" sin obligar a usar el launcher de Simón: todo lo que se pueda funciona con el launcher original (widgets, fondos, íconos, efectos, capa, barra, burbuja, bloqueo, carga).
- Rendimiento: las animaciones se pausan con la pantalla apagada, fuera de la vista, en páginas no visibles y en ahorro de batería; tope de FPS configurable.
- Los editores abren **siempre con lo que el usuario ya tiene** (guardado en Simón o puesto en el teléfono) y tienen un botón **"Empezar de cero"** visible, con confirmación.
- Botones con texto claro (no solo íconos), ayudas cortas para gestos ocultos, confirmación en acciones destructivas, deshacer donde se pueda, aviso al salir con cambios sin guardar.

## Trampas de Kotlin 2.x / Compose que ya rompieron el build

- Las funciones de extensión no se pueden llamar con nombre completo (`androidx.compose.ui.graphics.drawscope.drawIntoCanvas { }` falla): hay que importarlas.
- `when` sobre enums o sealed tiene que ser exhaustivo incluso como sentencia. Ojo al agregar valores a `TapAction`, `PanelTile`, `ActionType`, `TriggerType`, `ItemType`, etc.: hay `when` en varios archivos.
- `IntArray.mapNotNull` no existe: usar `.toList()` primero.
- En Compose el orden de los modificadores importa: `pointerInput` antes de `offset` deja la zona táctil en el lugar viejo (pasó con las manijas del editor).
- En `ElementBlock` el alto (`hW`) está en anchos del lienzo; un `WElement` sin posición explícita arranca en x = 0.1, w = 0.8 (poné x = 0, y = 0, w = 1, h = 1 en los bloques).
- Dentro de `apply { }`, un parámetro con el mismo nombre que una propiedad la tapa (`color`, `alpha`): usar `this.color`.
- No hay smart cast sobre `var` delegadas (`by remember`) ni propiedades mutables: copiar a un `val` local.
- No declarar funciones `@Composable` locales; usar funciones privadas de nivel superior.
- APIs experimentales de material3 (TopAppBar, TimePicker, ModalBottomSheet, DatePicker) necesitan `@OptIn(ExperimentalMaterial3Api::class)`.
- Los datos se guardan como JSON: los campos nuevos de modelos `@Serializable` siempre con valor por defecto para no romper datos guardados.

## Limitaciones de Android (no son bugs)

- No se puede leer la imagen del fondo de pantalla sin `MANAGE_EXTERNAL_STORAGE`; se usan sus colores (`getWallpaperColors`) y, en el editor a pantalla completa, la ventana transparente que muestra el fondo real.
- No se puede leer cómo el usuario ordenó su launcher original; la primera vez el launcher de Simón propone apps (dock por defecto + apps actualizadas recientemente) o empezar vacío.
- No se puede dibujar con la pantalla apagada; los efectos de pantalla aparecen al prenderla.
- Pantallas de carga/bloqueo: requieren "Mostrar sobre otras apps". La pantalla de bloqueo propia no reemplaza la seguridad: al deslizar sigue el bloqueo del sistema.
- Rutinas por evento y pantalla de carga funcionan mientras esté activo el servicio de accesibilidad o el acceso a notificaciones (que registran los receivers dinámicos).
- Rutinas por hora: alarmas inexactas (`setAndAllowWhileIdle`), pueden demorar minutos. Rotación de fondos: mínimo cada 15 min (WorkManager).
- Sonidos por app: suenan además del sonido de la app; el usuario debe silenciar ese canal en Android para que no se superpongan.

## Estado y pendientes

- Compila (debug y release) y se probó en el emulador cada flujo: widgets (agregar, editar, stickers, actualización por minuto), widgets creados, íconos (crear y actualizar el acceso existente), fondos animados y estáticos, temas, launcher (primer arranque, default, edición, mover, deshacer, cajón, páginas, carpetas), capa sobre el launcher original y su editor a pantalla completa, efectos, barra lateral, burbuja, pantallas de carga y bloqueo, rutinas por evento, copias (guardar y restaurar), dibujo y animación.
- Corregido en esa prueba: "Usar Simón" y el switch del modo launcher no pedían ser launcher por defecto (el pedido de rol tiene que abrirse "para resultado"); el launcher arrancaba sin íconos ni dock en frío (`AppCatalog.find` no era observable por Compose); al recrearse `MainActivity` (p. ej. al cambiar el fondo) se volvía a abrir el editor del intent original; la barra lateral y la burbuja se veían sobre el bloqueo y sobre las pantallas de carga/bloqueo de Simón; "Activo" en Mío seguía marcando el fondo animado después de poner uno estático; la copia se guardaba como `.simon.zip`; pestañas de Efectos cortadas; `{mes}` con mayúscula; tocar el nombre de una paleta no la elegía.
- Pendientes:
  1. Probar en un teléfono real (sobre todo launchers de otras marcas, rendimiento y batería de la capa y el launcher).
  2. El reloj de los widgets puede atrasar hasta ~1 min: `setWindow` es inexacto y Android estira la ventana y limita las alarmas en segundo plano. Para que sea exacto haría falta `SCHEDULE_EXACT_ALARM` (el usuario lo concede en Ajustes) y usar `setExact` cuando `canScheduleExactAlarms()`.
  3. Sin probar: rutinas por hora, sonidos por app (necesitan notificaciones reales), compartir copia, packs de íconos, widgets de otras apps dentro del launcher.
- Conocido sin resolver: "Quitar" desde el menú de una app fuera del modo edición no se puede deshacer; borrar una página limpia el historial de deshacer; el fondo de color de una animación no se guarda con la animación.
