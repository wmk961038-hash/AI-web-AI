#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
dest="${1:-../backups/$(date +%Y%m%d-%H%M%S)}"
mkdir -p "$dest"
# Stop writers so SQL and media form a consistent snapshot. Always restart on exit.
trap 'docker compose start backend >/dev/null' EXIT
docker compose stop backend
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump --no-tablespaces --single-transaction -u"$MYSQL_USER" "$MYSQL_DATABASE"' > "$dest/database.sql"
docker compose run --rm --no-deps -T --user root --entrypoint sh backend -c 'tar -czf - -C /data/uploads .' > "$dest/uploads.tar.gz"
test -s "$dest/database.sql"
test -s "$dest/uploads.tar.gz"
echo "Backup saved: $dest"
