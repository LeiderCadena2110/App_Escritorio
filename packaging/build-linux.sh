#!/usr/bin/env bash
# Crea el .deb y el AppImage en Linux.
# Requisitos: "mvn package" (crea target/app), JDK 21 con jpackage, fakeroot (para el .deb)
# y appimagetool (https://github.com/AppImage/appimagetool/releases) para el AppImage.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f target/app/automatizador-reportes.jar ] || { echo "Falta target/app. Ejecute primero: mvn package"; exit 1; }
JPACKAGE="${JAVA_HOME:+$JAVA_HOME/bin/}jpackage"
NOMBRE="Automatizador de Reportes"
COMUNES=(--app-version 1.0.0 --vendor "UCC Pasto" --description "Genera informes Word y PDF desde Excel o CSV"
  --input target/app --main-jar automatizador-reportes.jar --main-class co.edu.ucc.reportes.Lanzador
  --icon empaquetado/iconos/icono.png)
rm -rf target/instaladores && mkdir -p target/instaladores

# .deb (Ubuntu, Debian, Mint)
"$JPACKAGE" --type deb --name automatizador-reportes "${COMUNES[@]}" \
  --linux-menu-group Office --linux-shortcut --linux-app-category office --dest target/instaladores

# AppImage: carpeta de jpackage + AppRun + .desktop
"$JPACKAGE" --type app-image --name "$NOMBRE" "${COMUNES[@]}" --dest target/appimage
APPDIR="target/appimage/$NOMBRE.AppDir"
mv "target/appimage/$NOMBRE" "$APPDIR"
cp empaquetado/iconos/icono.png "$APPDIR/automatizador-reportes.png"
cat > "$APPDIR/automatizador-reportes.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=$NOMBRE
Exec=AppRun
Icon=automatizador-reportes
Categories=Office;
DESKTOP
cat > "$APPDIR/AppRun" <<'APPRUN'
#!/bin/sh
AQUI="$(dirname "$(readlink -f "$0")")"
exec "$AQUI/bin/Automatizador de Reportes" "$@"
APPRUN
chmod +x "$APPDIR/AppRun"
if command -v appimagetool >/dev/null; then
  ARCH=x86_64 appimagetool "$APPDIR" "target/instaladores/Automatizador_de_Reportes-1.0.0-x86_64.AppImage"
else
  echo "Aviso: no se encontró appimagetool; se creó solo el .deb"
fi
echo "Listo: target/instaladores"
