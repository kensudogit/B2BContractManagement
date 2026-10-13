#!/usr/bin/env bash
set -euo pipefail

if [[ -n "${SPRING_DATASOURCE_URL:-}" ]]; then
  :
elif [[ -n "${PGHOST:-}" ]]; then
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${PGHOST}:${PGPORT:-5432}/${PGDATABASE}?sslmode=require"
  export SPRING_DATASOURCE_USERNAME="${PGUSER:-postgres}"
  export SPRING_DATASOURCE_PASSWORD="${PGPASSWORD:-}"
elif [[ -n "${DATABASE_URL:-}" ]]; then
  raw="${DATABASE_URL#postgresql://}"
  raw="${raw#postgres://}"
  userpass="${raw%%@*}"
  hostdb="${raw#*@}"
  export SPRING_DATASOURCE_USERNAME="${userpass%%:*}"
  export SPRING_DATASOURCE_PASSWORD="${userpass#*:}"
  hostport="${hostdb%%/*}"
  db="${hostdb#*/}"
  db="${db%%\?*}"
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${hostport}/${db}?sslmode=require"
fi

export DOCUMENT_DIR="${DOCUMENT_DIR:-/data/documents}"
mkdir -p "$DOCUMENT_DIR"

export SERVER_PORT="${SERVER_PORT:-8040}"
java -jar /app/api.jar &
api_pid=$!

for _ in $(seq 1 90); do
  if curl -sf "http://127.0.0.1:${SERVER_PORT}/actuator/health" >/dev/null; then
    break
  fi
  if ! kill -0 "$api_pid" 2>/dev/null; then
    echo "API process exited before becoming healthy" >&2
    wait "$api_pid"
    exit 1
  fi
  sleep 2
done

export HOSTNAME=0.0.0.0
export PORT="${PORT:-3040}"
export API_PROXY="http://127.0.0.1:${SERVER_PORT}"
cd /app/web
exec node server.js
