#!/usr/bin/env bash
# Builds the macOS .dmg (on a Mac; one .dmg per architecture: Apple Silicon or Intel).
# Requirements: "mvn package" (creates target/app) and JDK 21 with jpackage.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f target/app/stockflow.jar ] || { echo "target/app is missing. Run first: mvn package"; exit 1; }
JPACKAGE="${JAVA_HOME:+$JAVA_HOME/bin/}jpackage"
rm -rf target/installers
"$JPACKAGE" --type dmg --name "StockFlow" --app-version 1.0.0 --vendor "UCC Pasto" \
  --description "Inventario y facturación para pequeños negocios" \
  --input target/app --main-jar stockflow.jar --main-class co.edu.ucc.stockflow.Launcher \
  --icon packaging/icons/app-icon.icns --mac-package-identifier co.edu.ucc.stockflow \
  --dest target/installers
# Unsigned: the first time, System Settings > Privacy & Security > "Open Anyway"
echo "Done: target/installers"
