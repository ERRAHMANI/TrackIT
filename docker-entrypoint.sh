#!/bin/sh
set -e

# Pièces jointes des données de démonstration : copiées une seule fois sur le volume.
mkdir -p "$UPLOADS_DIR" "$REPORTS_DIR"
if [ ! -d "$UPLOADS_DIR/demo" ]; then
    cp -r /app/demo-uploads "$UPLOADS_DIR/demo"
fi

# L'hébergeur fournit le port d'écoute dans PORT (8080 par défaut).
exec java $JAVA_OPTS -Dserver.port="${PORT:-8080}" -jar /app/app.jar
