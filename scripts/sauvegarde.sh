#!/bin/sh
# Sauvegarde de la base TrackIT : dump MySQL compressé et horodaté.
#
# Usage : sh scripts/sauvegarde.sh
# Variables d'environnement (aucun secret dans ce fichier, voir .env.example) :
#   DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD   connexion à MySQL (comme l'application)
#   SAUVEGARDE_DIR              dossier de destination (défaut : ./sauvegardes)
#   SAUVEGARDE_RETENTION_JOURS  suppression des sauvegardes plus anciennes (défaut : 14, 0 = jamais)
#
# Le mot de passe passe par MYSQL_PWD, jamais en argument : il n'apparaît pas dans la liste des processus.
# Le dump est cohérent sans bloquer l'application (--single-transaction, tables InnoDB).
set -eu

: "${DB_HOST:=localhost}"
: "${DB_PORT:=3306}"
: "${DB_NAME:=trackit}"
: "${DB_USERNAME:?DB_USERNAME manquant}"
: "${DB_PASSWORD:?DB_PASSWORD manquant}"
: "${SAUVEGARDE_DIR:=./sauvegardes}"
: "${SAUVEGARDE_RETENTION_JOURS:=14}"

mkdir -p "$SAUVEGARDE_DIR"
HORODATAGE=$(date -u +%Y%m%d-%H%M%SZ)
FICHIER="$SAUVEGARDE_DIR/trackit-$DB_NAME-$HORODATAGE.sql.gz"
TEMPORAIRE="$FICHIER.partiel"

# Fichier temporaire puis renommage : une sauvegarde interrompue ne laisse jamais un .sql.gz incomplet.
trap 'rm -f "$TEMPORAIRE"' EXIT
export MYSQL_PWD="$DB_PASSWORD"
mysqldump --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME" \
    --single-transaction --quick --routines --triggers --no-tablespaces \
    --default-character-set=utf8mb4 "$DB_NAME" | gzip -9 > "$TEMPORAIRE"
unset MYSQL_PWD

# Contrôle minimal : l'archive est lisible et se termine par la ligne finale de mysqldump.
if ! gzip -dc "$TEMPORAIRE" | tail -n 1 | grep -q "Dump completed"; then
    echo "ERREUR : sauvegarde incomplète, fichier supprimé." >&2
    exit 1
fi
mv "$TEMPORAIRE" "$FICHIER"
trap - EXIT
echo "Sauvegarde créée : $FICHIER ($(wc -c < "$FICHIER") octets)"

if [ "$SAUVEGARDE_RETENTION_JOURS" -gt 0 ]; then
    find "$SAUVEGARDE_DIR" -name "trackit-$DB_NAME-*.sql.gz" -type f -mtime +"$SAUVEGARDE_RETENTION_JOURS" -print -delete
fi
