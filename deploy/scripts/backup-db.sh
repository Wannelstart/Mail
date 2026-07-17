#!/usr/bin/env bash
set -e

mkdir -p backups
TS=$(date +%Y%m%d_%H%M%S)
source .env

docker exec mail_mysql sh -c "exec mysqldump -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE}" > "backups/db_${TS}.sql"
echo "数据库备份完成: backups/db_${TS}.sql"