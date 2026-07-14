#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)

docker compose \
  --env-file "$ROOT_DIR/deploy/.env" \
  -f "$ROOT_DIR/deploy/docker-compose.yml" \
  config
