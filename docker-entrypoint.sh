#!/bin/sh
set -e

UPLOADS_DIR="${UPLOADS_DIR:-/data/uploads}"
REPORTS_DIR="${REPORTS_DIR:-/data/reports}"
UTILISATEUR_APP=trackit

# Pièces jointes des données de démonstration : copiées une seule fois sur le volume.
mkdir -p "$UPLOADS_DIR" "$REPORTS_DIR"
if [ ! -d "$UPLOADS_DIR/demo" ]; then
    cp -r /app/demo-uploads "$UPLOADS_DIR/demo"
fi

# L'hébergeur fournit le port d'écoute dans PORT (8080 par défaut).
set -- java $JAVA_OPTS -Dserver.port="${PORT:-8080}" -jar /app/app.jar

if [ "$(id -u)" = "0" ]; then
    # Démarrage en root (volume Railway monté en root, RAILWAY_RUN_UID=0) : le volume est rendu
    # à l'utilisateur dédié, puis Java est lancé sans aucun privilège root.
    chown -R "$UTILISATEUR_APP:$UTILISATEUR_APP" "$UPLOADS_DIR" "$REPORTS_DIR"
    exec setpriv --reuid="$UTILISATEUR_APP" --regid="$UTILISATEUR_APP" --init-groups --inh-caps=-all "$@"
fi
exec "$@"
