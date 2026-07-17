#!/usr/bin/env bash
set -e

DOMAIN="$1"
EMAIL="$2"

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  echo "用法: bash deploy/scripts/issue-cert.sh mail.example.com you@example.com"
  exit 1
fi

docker run --rm \
  -v "$(pwd)/deploy/nginx/certbot/www:/var/www/certbot" \
  -v "$(pwd)/deploy/nginx/certbot/conf:/etc/letsencrypt" \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d "$DOMAIN" \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email