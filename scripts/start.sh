#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SERVER_JAR="${SERVER_JAR:-DragonsDB-server.jar}"
LB_BIN="${LB_BIN:-LoadBalancer}"
LB_PORT="${LB_PORT:-5000}"
SERVER_BASE_PORT="${SERVER_BASE_PORT:-5001}"
SERVER_COUNT="${SERVER_COUNT:-3}"
SERVER_LOG_DIR="${SERVER_LOG_DIR:-logs}"
LB_STRATEGY="${LB_STRATEGY:-round_robin}"
SERVER_JAVA_OPTS="${SERVER_JAVA_OPTS:-}"
LB_ARGS=()

mkdir -p "$SERVER_LOG_DIR"

start_server() {
  local port="$1"
  local log_file="$SERVER_LOG_DIR/server-$port.log"
  echo "Starting server on port $port"
  nohup java $SERVER_JAVA_OPTS -jar "$SERVER_JAR" --port="$port" >"$log_file" 2>&1 &
  echo $!
}

for ((i = 0; i < SERVER_COUNT; i++)); do
  port=$((SERVER_BASE_PORT + i))
  start_server "$port" >/dev/null
  LB_ARGS+=("--backend=127.0.0.1:${port}:1")
  sleep 1
done

echo "Starting load balancer on port $LB_PORT"
nohup "./$LB_BIN" --strategy="$LB_STRATEGY" "${LB_ARGS[@]}" >"$SERVER_LOG_DIR/loadbalancer.log" 2>&1 &
echo $! >"$SERVER_LOG_DIR/loadbalancer.pid"

echo "Load balancer started. Logs in $SERVER_LOG_DIR"
