#!/usr/bin/env bash
# Crea el .dmg de macOS (en un Mac; un .dmg por arquitectura: Apple Silicon o Intel).
# Requisitos: haber ejecutado "mvn package" (crea target/app) y JDK 21 con jpackage.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f target/app/automatizador-reportes.jar ] || { echo "Falta target/app. Ejecute primero: mvn package"; exit 1; }
JPACKAGE="${JAVA_HOME:+$JAVA_HOME/bin/}jpackage"
rm -rf target/instaladores
"$JPACKAGE" --type dmg --name "Automatizador de Reportes" --app-version 1.0.0 --vendor "UCC Pasto" \
  --description "Genera informes Word y PDF desde Excel o CSV" \
  --input target/app --main-jar automatizador-reportes.jar --main-class co.edu.ucc.reportes.Lanzador \
  --icon empaquetado/iconos/icono.icns --mac-package-identifier co.edu.ucc.reportes \
  --dest target/instaladores
# Sin firma: la primera vez, Ajustes del Sistema > Privacidad y seguridad > "Abrir de todos modos"
echo "Listo: target/instaladores"
