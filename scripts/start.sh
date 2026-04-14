#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ACTION="${1:-start}"

SERVER_JAR="${SERVER_JAR:-DragonsDB-server.jar}"
LB_BIN="${LB_BIN:-LoadBalancer}"
LB_PORT="${LB_PORT:-5000}"
SERVER_BASE_PORT="${SERVER_BASE_PORT:-5001}"
SERVER_COUNT="${SERVER_COUNT:-3}"
SERVER_LOG_DIR="${SERVER_LOG_DIR:-logs}"
LB_STRATEGY="${LB_STRATEGY:-round_robin}"
SERVER_JAVA_OPTS="${SERVER_JAVA_OPTS:--Xms32m -Xmx256m -XX:MaxMetaspaceSize=128m}"
SERVER_PORT_SCAN_LIMIT="${SERVER_PORT_SCAN_LIMIT:-200}"
LB_ARGS=()
SERVER_PID_FILE=""
LB_PID_FILE=""

mkdir -p "$SERVER_LOG_DIR"
SERVER_PID_FILE="$SERVER_LOG_DIR/server.pids"
LB_PID_FILE="$SERVER_LOG_DIR/loadbalancer.pid"

terminate_pid() {
  local pid="$1"

  if [[ -z "$pid" ]]; then
    return
  fi

  if ! kill -0 "$pid" 2>/dev/null; then
    return
  fi

  kill "$pid" 2>/dev/null || true
  sleep 1

  if kill -0 "$pid" 2>/dev/null; then
    kill -9 "$pid" 2>/dev/null || true
  fi
}

stop_stack() {
  local stopped_any=0

  if [[ -f "$LB_PID_FILE" ]]; then
    lb_pid="$(cat "$LB_PID_FILE" 2>/dev/null || true)"
    if [[ -n "$lb_pid" ]]; then
      terminate_pid "$lb_pid"
      stopped_any=1
      echo "Stopped load balancer PID $lb_pid"
    fi
    rm -f "$LB_PID_FILE"
  fi

  if [[ -f "$SERVER_PID_FILE" ]]; then
    while read -r server_pid _; do
      if [[ -n "${server_pid:-}" ]]; then
        terminate_pid "$server_pid"
        stopped_any=1
        echo "Stopped server PID $server_pid"
      fi
    done <"$SERVER_PID_FILE"
    rm -f "$SERVER_PID_FILE"
  fi

  # Fallback for orphaned processes if pid files are missing or stale.
  if command -v pkill >/dev/null 2>&1; then
    pkill -f "java .*${SERVER_JAR}" >/dev/null 2>&1 || true
    pkill -f "./${LB_BIN}|/${LB_BIN}$" >/dev/null 2>&1 || true
  fi

  if [[ "$stopped_any" -eq 0 ]]; then
    echo "No tracked lab processes found."
  else
    echo "Lab processes stopped."
  fi
}

case "$ACTION" in
  stop)
    stop_stack
    exit 0
    ;;
  start)
    ;;
  *)
    echo "Usage: $0 [start|stop]" >&2
    exit 1
    ;;
esac

: >"$SERVER_PID_FILE"

is_port_in_use() {
  local port="$1"

  if command -v sockstat >/dev/null 2>&1; then
    sockstat -4 -l 2>/dev/null | awk '{print $6}' | grep -Eq "[.:]${port}$"
    return
  fi

  if command -v netstat >/dev/null 2>&1; then
    netstat -an 2>/dev/null | grep -Eq "[\.:]${port}[[:space:]]"
    return
  fi

  return 1
}

start_server() {
  local port="$1"
  local log_file="$SERVER_LOG_DIR/server-$port.log"
  local pid
  echo "Starting server on port $port"
  nohup env -u _JAVA_OPTIONS -u JAVA_TOOL_OPTIONS java $SERVER_JAVA_OPTS -jar "$SERVER_JAR" --port="$port" >"$log_file" 2>&1 &
  pid=$!

  sleep 1
  if ! kill -0 "$pid" 2>/dev/null; then
    echo "Server on port $port failed to start. See $log_file" >&2
    tail -n 80 "$log_file" >&2 || true
    exit 1
  fi

  echo "$pid $port" >>"$SERVER_PID_FILE"
  echo "$pid"
}

if is_port_in_use "$LB_PORT"; then
  echo "Load balancer port $LB_PORT is already in use. Set LB_PORT to a free port." >&2
  exit 1
fi

started=0
port="$SERVER_BASE_PORT"
max_port=$((SERVER_BASE_PORT + SERVER_PORT_SCAN_LIMIT))

while ((started < SERVER_COUNT)); do
  if ((port > max_port)); then
    echo "Could not find enough free backend ports. Requested: $SERVER_COUNT, found: $started" >&2
    echo "Increase SERVER_PORT_SCAN_LIMIT or stop processes using the port range." >&2
    exit 1
  fi

  if is_port_in_use "$port"; then
    echo "Port $port is busy, trying next" >&2
    port=$((port + 1))
    continue
  fi

  start_server "$port" >/dev/null
  LB_ARGS+=("--backend=127.0.0.1:${port}:1")
  started=$((started + 1))
  port=$((port + 1))
  sleep 1
done

echo "Starting load balancer on port $LB_PORT"
nohup "./$LB_BIN" --strategy="$LB_STRATEGY" "${LB_ARGS[@]}" >"$SERVER_LOG_DIR/loadbalancer.log" 2>&1 &
lb_pid=$!
echo "$lb_pid" >"$LB_PID_FILE"

sleep 1
if ! kill -0 "$lb_pid" 2>/dev/null; then
  echo "Load balancer failed to start. See $SERVER_LOG_DIR/loadbalancer.log" >&2
  tail -n 80 "$SERVER_LOG_DIR/loadbalancer.log" >&2 || true
  exit 1
fi

echo "Load balancer started. Logs in $SERVER_LOG_DIR"
