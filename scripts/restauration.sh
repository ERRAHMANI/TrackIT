#!/bin/sh
# Restauration d'une sauvegarde TrackIT produite par scripts/sauvegarde.sh.
#
# Usage : sh scripts/restauration.sh <fichier.sql.gz> --confirmer
# Variables d'environnement : DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD (voir .env.example).
#
# ATTENTION : la base DB_NAME est remplacée par le contenu de la sauvegarde. Pour un test de
# restauration, pointer DB_NAME vers une base vide distincte (par exemple trackit_restauration).
set -eu

FICHIER="${1:-}"
if [ -z "$FICHIER" ] || [ ! -f "$FICHIER" ]; then
    echo "Usage : sh scripts/restauration.sh <fichier.sql.gz> --confirmer" >&2
    exit 2
fi
if [ "${2:-}" != "--confirmer" ]; then
    echo "La base « ${DB_NAME:-trackit} » sur ${DB_HOST:-localhost} va être remplacée." >&2
    echo "Relancez avec --confirmer pour continuer." >&2
    exit 2
fi

: "${DB_HOST:=localhost}"
: "${DB_PORT:=3306}"
: "${DB_NAME:=trackit}"
: "${DB_USERNAME:?DB_USERNAME manquant}"
: "${DB_PASSWORD:?DB_PASSWORD manquant}"

gzip -t "$FICHIER"
export MYSQL_PWD="$DB_PASSWORD"
gzip -dc "$FICHIER" | mysql --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME" \
    --default-character-set=utf8mb4 "$DB_NAME"
unset MYSQL_PWD
echo "Restauration terminée dans la base « $DB_NAME » à partir de $FICHIER."
