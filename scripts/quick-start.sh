#!/usr/bin/env sh
set -eu

# Author: huangbingrui.awa
ROOT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)
ENV_FILE="$ROOT_DIR/deploy/.env"
ENV_TEMPLATE="$ROOT_DIR/deploy/.env.example"
COMPOSE_FILE="$ROOT_DIR/deploy/docker-compose.yml"

random_secret() {
  od -An -N 24 -tx1 /dev/urandom | tr -d ' \n'
}

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required. Install Docker Engine or Docker Desktop first." >&2
  exit 1
fi

docker compose version >/dev/null

if [ ! -f "$ENV_FILE" ]; then
  umask 077
  DB_PASSWORD=$(random_secret)
  DB_ROOT_PASSWORD=$(random_secret)
  OPERATION_PASSWORD=$(random_secret)
  TEMP_ENV=$(mktemp "${TMPDIR:-/tmp}/ojtdm-env.XXXXXX")
  trap 'rm -f "$TEMP_ENV"' 0 HUP INT TERM

  awk \
    -v db_password="$DB_PASSWORD" \
    -v db_root_password="$DB_ROOT_PASSWORD" \
    -v operation_password="$OPERATION_PASSWORD" '
      /^TRAINING_DB_PASSWORD=/ {
        print "TRAINING_DB_PASSWORD=" db_password
        next
      }
      /^TRAINING_DB_ROOT_PASSWORD=/ {
        print "TRAINING_DB_ROOT_PASSWORD=" db_root_password
        next
      }
      /^TRAINING_OPERATION_PASSWORD=/ {
        print "TRAINING_OPERATION_PASSWORD=" operation_password
        next
      }
      { print }
    ' "$ENV_TEMPLATE" > "$TEMP_ENV"

  chmod 600 "$TEMP_ENV"
  mv "$TEMP_ENV" "$ENV_FILE"
  echo "Created deploy/.env with random local credentials."
else
  chmod 600 "$ENV_FILE"
  echo "Using existing deploy/.env; no credentials were changed."
fi

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config --quiet
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build --wait
ENV_FILE="$ENV_FILE" "$ROOT_DIR/scripts/smoke-test.sh"

FRONTEND_PORT=$(sed -n 's/^FRONTEND_PORT=//p' "$ENV_FILE" | tail -n 1)
FRONTEND_PORT=${FRONTEND_PORT:-3100}

echo "OJ Training Data Manager is ready: http://127.0.0.1:$FRONTEND_PORT"
echo "The operation password is stored locally in deploy/.env."
