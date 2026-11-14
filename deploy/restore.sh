#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
source_dir="${1:?Usage: sh restore.sh BACKUP_DIRECTORY --confirm}"
test "${2:-}" = '--confirm' || { echo 'Restore overwrites database content. Supply --confirm after verifying the backup.'; exit 1; }
test -s "$source_dir/database.sql"
test -s "$source_dir/uploads.tar.gz"
docker compose stop backend
# Keep backend stopped if recovery fails, preventing writes to a partial restore.
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"' < "$source_dir/database.sql"
docker compose run --rm --no-deps -T --user root --entrypoint sh backend -c 'tar -xzf - -C /data/uploads && chown -R club:club /data/uploads' < "$source_dir/uploads.tar.gz"
docker compose start backend
echo 'Restore complete; verify published pages, images, and administrator login.'
