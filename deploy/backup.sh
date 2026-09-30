#!/usr/bin/env bash
# Backs up the Volunteer Portal database to a compressed SQL file and removes old backups.
# Run on the server from anywhere; schedule nightly with cron, e.g. (crontab -e):
#   30 2 * * * /opt/volunteerportal/deploy/backup.sh >> /var/backups/volunteerportal/backup.log 2>&1
# Restore: see "Backups" in deploy/README.md.
set -euo pipefail

cd "$(dirname "$0")"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/volunteerportal}"
KEEP_DAYS="${KEEP_DAYS:-14}"
mkdir -p "$BACKUP_DIR"

file="$BACKUP_DIR/volunteerportal-$(date +%Y%m%d-%H%M%S).sql.gz"
docker compose exec -T mysql sh -c \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump --single-transaction --routines --no-tablespaces -uroot volunteerportal' \
    | gzip > "$file"

find "$BACKUP_DIR" -name 'volunteerportal-*.sql.gz' -mtime +"$KEEP_DAYS" -delete
echo "$(date -Is) backup written: $file ($(du -h "$file" | cut -f1))"
