#!/bin/sh
# Pone la última versión de Simón en el emulador simon_api35.
#
#   ./emulador.sh            trae lo último de origin/dev (si no hay cambios sin guardar),
#                            prende el emulador si hace falta, compila, instala y abre la app
#   ./emulador.sh --local    igual, pero sin traer nada de GitHub
#   ./emulador.sh --auto     lo usan los ganchos de git: solo instala si el emulador ya está prendido
#
# Lo que pasa queda en F:/Android/emulador.log.

cd "$(dirname "$0")" || exit 1

export JAVA_HOME="${JAVA_HOME:-F:/Android/jdk-17}"
export ANDROID_HOME="${ANDROID_HOME:-F:/Android/Sdk}"
GRADLE="${GRADLE:-F:/Android/gradle-8.11.1/bin/gradle}"
ADB="$ANDROID_HOME/platform-tools/adb.exe"
EMULATOR="$ANDROID_HOME/emulator/emulator.exe"
AVD="simon_api35"
PKG="com.migux97.simon"
LOG="F:/Android/emulador.log"

MODE="${1:-}"

say() { echo "[$(date +%H:%M:%S)] $*" | tee -a "$LOG"; }

running() { "$ADB" devices 2>/dev/null | grep -q "^emulator-.*device$"; }

booted() { [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; }

if [ "$MODE" = "--auto" ] && ! running; then
    say "Emulador apagado: no se instala (corré ./emulador.sh para prenderlo)."
    exit 0
fi

# Traer lo último de GitHub (solo si no hay cambios sin guardar, para no pisar nada).
if [ -z "$MODE" ]; then
    if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
        say "Hay cambios sin commitear: se instala lo que hay en la carpeta, sin traer de GitHub."
    else
        say "Trayendo lo último de origin/dev..."
        git pull --ff-only -q origin dev >>"$LOG" 2>&1 || say "No se pudo traer de GitHub; sigo con lo que hay."
    fi
fi

if ! running; then
    say "Prendiendo el emulador $AVD..."
    "$EMULATOR" -avd "$AVD" -no-snapshot-save -no-boot-anim >>"$LOG" 2>&1 &
    "$ADB" wait-for-device
fi
i=0
until booted; do
    i=$((i + 1))
    [ $i -gt 120 ] && { say "El emulador no terminó de arrancar en 6 minutos."; exit 1; }
    sleep 3
done

say "Compilando e instalando ($(git log -1 --format='%h %s'))..."
if ! "$GRADLE" installDebug --console=plain -q >>"$LOG" 2>&1; then
    say "Falló la compilación. Mirá $LOG"
    exit 1
fi

"$ADB" shell am force-stop "$PKG"
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
say "Listo: Simón actualizado y abierto en el emulador."
