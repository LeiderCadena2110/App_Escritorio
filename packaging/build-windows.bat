@echo off
rem Builds the Windows installer. Requirements:
rem   1. Run Maven "package" first (IntelliJ: Maven > Lifecycle > package), which creates target\app
rem   2. JDK 21 (includes jpackage). If it is not in the PATH, set JAVA_HOME.
rem   3. For the .exe installer: WiX Toolset 3.14 (https://github.com/wixtoolset/wix3/releases)
rem Usage:  packaging\build-windows.bat        -> portable folder (no WiX needed)
rem         packaging\build-windows.bat exe    -> .exe installer
setlocal
cd /d "%~dp0\.."
if not exist target\app\stockflow.jar (
  echo target\app is missing. Run Maven "package" first.
  exit /b 1
)
set JPACKAGE=jpackage
if defined JAVA_HOME set JPACKAGE="%JAVA_HOME%\bin\jpackage"
set TYPE=app-image
if /i "%1"=="exe" set TYPE=exe
if exist target\installers rmdir /s /q target\installers

set EXTRA=
if /i "%TYPE%"=="exe" set EXTRA=--win-menu --win-shortcut --win-dir-chooser --win-per-user-install --win-menu-group "StockFlow"

%JPACKAGE% --type %TYPE% --name "StockFlow" --app-version 1.0.0 --vendor "UCC Pasto" ^
  --description "Inventario y facturacion para pequenos negocios" ^
  --input target\app --main-jar stockflow.jar --main-class co.edu.ucc.stockflow.Launcher ^
  --icon packaging\icons\app-icon.ico --dest target\installers %EXTRA%
if errorlevel 1 exit /b 1
echo Done: target\installers
