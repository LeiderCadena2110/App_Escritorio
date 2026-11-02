@echo off
rem Crea el instalador de Windows. Requisitos:
rem   1. Haber ejecutado "package" de Maven (IntelliJ: Maven > Lifecycle > package), que crea target\app
rem   2. JDK 21 (trae jpackage). Si no esta en el PATH, define JAVA_HOME.
rem   3. Para el .exe instalable: WiX Toolset 3.14 (https://github.com/wixtoolset/wix3/releases)
rem Uso:  empaquetado\empaquetar-windows.bat        -> carpeta portable (no necesita WiX)
rem       empaquetado\empaquetar-windows.bat exe    -> instalador .exe
setlocal
cd /d "%~dp0\.."
if not exist target\app\automatizador-reportes.jar (
  echo Falta target\app. Ejecute primero "package" de Maven.
  exit /b 1
)
set JPACKAGE=jpackage
if defined JAVA_HOME set JPACKAGE="%JAVA_HOME%\bin\jpackage"
set TIPO=app-image
if /i "%1"=="exe" set TIPO=exe
if exist target\instaladores rmdir /s /q target\instaladores

set EXTRA=
if /i "%TIPO%"=="exe" set EXTRA=--win-menu --win-shortcut --win-dir-chooser --win-per-user-install --win-menu-group "Automatizador de Reportes"

%JPACKAGE% --type %TIPO% --name "Automatizador de Reportes" --app-version 1.0.0 --vendor "UCC Pasto" ^
  --description "Genera informes Word y PDF desde Excel o CSV" ^
  --input target\app --main-jar automatizador-reportes.jar --main-class co.edu.ucc.reportes.Lanzador ^
  --icon empaquetado\iconos\icono.ico --dest target\instaladores %EXTRA%
if errorlevel 1 exit /b 1
echo Listo: target\instaladores
