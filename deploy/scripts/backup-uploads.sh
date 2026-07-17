#!/usr/bin/env bash
set -e

mkdir -p backups
TS=$(date +%Y%m%d_%H%M%S)
docker run --rm \
  -v app_uploads:/data/uploads:ro \
  -v "$(pwd)/backups:/backup" \
  alpine sh -c "cd /data && tar czf /backup/uploads_${TS}.tar.gz uploads"
echo "附件备份完成: backups/uploads_${TS}.tar.gz"