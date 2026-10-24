@echo off
rem Doble clic: pone la ultima version de Simon en el emulador (ver emulador.sh).
cd /d "%~dp0"
"C:\Program Files\Git\bin\sh.exe" ./emulador.sh %*
pause
