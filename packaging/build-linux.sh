#!/usr/bin/env bash
# Builds the .deb and the AppImage on Linux.
# Requirements: "mvn package" (creates target/app), JDK 21 with jpackage, fakeroot (for the .deb)
# and appimagetool (https://github.com/AppImage/appimagetool/releases) for the AppImage.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f target/app/stockflow.jar ] || { echo "target/app is missing. Run first: mvn package"; exit 1; }
JPACKAGE="${JAVA_HOME:+$JAVA_HOME/bin/}jpackage"
NAME="StockFlow"
COMMON=(--app-version 1.0.0 --vendor "UCC Pasto" --description "Inventario y facturación para pequeños negocios"
  --input target/app --main-jar stockflow.jar --main-class co.edu.ucc.stockflow.Launcher
  --icon packaging/icons/app-icon.png)
rm -rf target/installers && mkdir -p target/installers

# .deb (Ubuntu, Debian, Mint)
"$JPACKAGE" --type deb --name stockflow "${COMMON[@]}" \
  --linux-menu-group Office --linux-shortcut --linux-app-category office --dest target/installers

# AppImage: jpackage folder + AppRun + .desktop
"$JPACKAGE" --type app-image --name "$NAME" "${COMMON[@]}" --dest target/appimage
APPDIR="target/appimage/$NAME.AppDir"
rm -rf "$APPDIR" && mv "target/appimage/$NAME" "$APPDIR"
cp packaging/icons/app-icon.png "$APPDIR/stockflow.png"
cat > "$APPDIR/stockflow.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=$NAME
Comment=Inventario y facturación
Exec=AppRun
Icon=stockflow
Categories=Office;
DESKTOP
cat > "$APPDIR/AppRun" <<'APPRUN'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
exec "$HERE/bin/StockFlow" "$@"
APPRUN
chmod +x "$APPDIR/AppRun"
if command -v appimagetool >/dev/null; then
  ARCH=x86_64 appimagetool "$APPDIR" "target/installers/StockFlow-1.0.0-x86_64.AppImage"
else
  echo "Warning: appimagetool not found; only the .deb was created"
fi
echo "Done: target/installers"
