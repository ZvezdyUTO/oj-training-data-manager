#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)
ENV_FILE=${ENV_FILE:-"$ROOT_DIR/deploy/.env"}

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing $ENV_FILE; copy deploy/.env.example to deploy/.env first." >&2
  exit 1
fi

FRONTEND_PORT=$(sed -n 's/^FRONTEND_PORT=//p' "$ENV_FILE" | tail -n 1)
BACKEND_PORT=$(sed -n 's/^BACKEND_PORT=//p' "$ENV_FILE" | tail -n 1)
FRONTEND_PORT=${FRONTEND_PORT:-3100}
BACKEND_PORT=${BACKEND_PORT:-8190}

curl --fail --silent --show-error "http://127.0.0.1:$FRONTEND_PORT/" >/dev/null
curl --fail --silent --show-error "http://127.0.0.1:$FRONTEND_PORT/api/health" >/dev/null
curl --fail --silent --show-error "http://127.0.0.1:$BACKEND_PORT/health" >/dev/null

echo "Smoke test passed: UI and both health-check paths are reachable."
