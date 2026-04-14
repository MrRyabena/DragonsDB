#!/usr/bin/env bash
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS_DIR="${LOGS_DIR:-../logs}"
LOG_DIR_ABS="$(cd "$LOGS_DIR" 2>/dev/null && pwd || echo "$LOGS_DIR")"

MODE="${1:-help}"
FOLLOW="${2:-rt}"  # 'rt' for real-time, anything else for history

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'  # No Color

print_help() {
  cat <<EOF
Usage: $0 <mode> [follow-mode]

Modes:
  help            Show this help message
  all             Show all logs in real-time (best for demo)
  client          Show only client logs
  servers         Show only server logs (all 3 ports)
  lb              Show only load balancer logs
  combined        Show servers + LB with color-coded separators
  history-all     Show all historical logs (non-blocking)
  history-client  Show historical client logs
  history-servers Show historical server logs
  history-lb      Show historical LB logs
  list            List all available log files

Follow Mode (for real-time modes):
  rt   Real-time with tail (default)
  less Browse with less pager (Ctrl+C to exit)

Examples:
  # Watch live traffic in demo format:
  $0 all

  # Combine servers and LB with nice formatting:
  $0 combined

  # View client logs with pager:
  $0 client less

  # See what happened in the past:
  $0 history-all

  # List available logs:
  $0 list

EOF
}

list_logs() {
  if [[ ! -d "$LOG_DIR_ABS" ]]; then
    echo "Logs directory not found: $LOG_DIR_ABS"
    return 1
  fi
  
  echo "Available log files in: $LOG_DIR_ABS"
  ls -lh "$LOG_DIR_ABS"/*.log 2>/dev/null || echo "No log files found yet."
}

show_real_time() {
  local pattern="$1"
  local display_name="$2"
  local use_pager="$3"

  if [[ ! -f "$LOG_DIR_ABS/$pattern" ]] && ! ls "$LOG_DIR_ABS"/$pattern 2>/dev/null | head -q >/dev/null 2>&1; then
    echo -e "${RED}Log file(s) not found: $pattern${NC}"
    return 1
  fi

  if [[ "$use_pager" == "less" ]]; then
    tail -f $LOG_DIR_ABS/$pattern 2>/dev/null | less +F
  else
    echo -e "${CYAN}=== $display_name (press Ctrl+C to stop) ===${NC}"
    tail -f $LOG_DIR_ABS/$pattern 2>/dev/null
  fi
}

show_history() {
  local pattern="$1"
  local display_name="$2"

  if [[ ! -f "$LOG_DIR_ABS/$pattern" ]] && ! ls "$LOG_DIR_ABS"/$pattern 2>/dev/null | head -q >/dev/null 2>&1; then
    echo -e "${RED}Log file(s) not found: $pattern${NC}"
    return 1
  fi

  echo -e "${CYAN}=== $display_name ===${NC}"
  cat $LOG_DIR_ABS/$pattern 2>/dev/null | tail -n 200
}

show_all_realtime() {
  echo -e "${CYAN}===============================================${NC}"
  echo -e "${CYAN}     DragonsDB Lab Logs - Real-Time Monitor${NC}"
  echo -e "${CYAN}===============================================${NC}"
  echo -e "Starting log monitoring for ALL components..."
  echo -e "Logs directory: ${BLUE}$LOG_DIR_ABS${NC}"
  echo -e "Press ${YELLOW}Ctrl+C${NC} to exit\n"
  
  # Create named pipes for multiplexing
  mkfifo /tmp/client_pipe_$$ /tmp/servers_pipe_$$ /tmp/lb_pipe_$$ 2>/dev/null || true
  
  # Function to show colored output
  show_colored() {
    local color="$1"
    local label="$2"
    local pipe="$3"
    
    while IFS= read -r line; do
      echo -e "${color}[${label}]${NC} $line"
    done < "$pipe"
  }
  
  # Start tailing each log file
  tail -f "$LOG_DIR_ABS"/client.log 2>/dev/null > /tmp/client_pipe_$$ &
  tail -f "$LOG_DIR_ABS"/server*.log 2>/dev/null > /tmp/servers_pipe_$$ &
  tail -f "$LOG_DIR_ABS"/loadbalancer.log 2>/dev/null > /tmp/lb_pipe_$$ &
  
  # Show colored output
  (show_colored "$GREEN" "CLIENT " /tmp/client_pipe_$$) &
  (show_colored "$BLUE" "SERVERS" /tmp/servers_pipe_$$) &
  (show_colored "$MAGENTA" "LB" /tmp/lb_pipe_$$) &
  
  wait
  
  # Cleanup
  rm -f /tmp/client_pipe_$$ /tmp/servers_pipe_$$ /tmp/lb_pipe_$$ 2>/dev/null || true
}

show_combined() {
  echo -e "${CYAN}===============================================${NC}"
  echo -e "${CYAN}    DragonsDB Servers + LoadBalancer Monitor${NC}"
  echo -e "${CYAN}===============================================${NC}"
  echo -e "\n${YELLOW}Packet Flow Visualization:${NC}"
  echo -e "  Client -> [LB: port 5000] -> [Servers: 5001-5003] -> back to Client\n"
  echo -e "Press ${YELLOW}Ctrl+C${NC} to exit\n"
  
  # Use tail -f with process substitution for better real-time display
  tail -f "$LOG_DIR_ABS"/server*.log "$LOG_DIR_ABS"/loadbalancer.log 2>/dev/null | while IFS= read -r line; do
    if [[ "$line" =~ "loadbalancer.log:" ]]; then
      echo -e "${MAGENTA}[LB]${NC} ${line#*:}"
    elif [[ "$line" =~ "server-" ]]; then
      port=$(echo "$line" | grep -oE 'server-[0-9]+' | grep -oE '[0-9]+')
      echo -e "${BLUE}[SERVER:$port]${NC} ${line#*:}"
    else
      echo "$line"
    fi
  done
}

show_banner() {
  cat <<'EOF'
╔═══════════════════════════════════════════════════════════╗
║        DragonsDB - Lab 7 Unified Logging System          ║
╚═══════════════════════════════════════════════════════════╝

This script aggregates logs from:
  • DragonsDB Client (Java/CLI)
  • DragonsDB Servers (3x ports 5001-5003)
  • LoadBalancer (C++ UDP LB on port 5000)

Perfect for demonstrations! Watch the packet flow in real-time:
  Client sends packet → LB routes to backend → Backend processes

EOF
}

# Create logs directory if needed
mkdir -p "$LOG_DIR_ABS" 2>/dev/null || true

case "$MODE" in
  help)
    print_help
    ;;
  all)
    show_banner
    show_all_realtime
    ;;
  client)
    show_real_time "client.log" "Client Logs" "$FOLLOW"
    ;;
  servers)
    show_real_time "server*.log" "Server Logs (Ports 5001-5003)" "$FOLLOW"
    ;;
  lb)
    show_real_time "loadbalancer.log" "LoadBalancer Logs" "$FOLLOW"
    ;;
  combined)
    show_combined
    ;;
  history-all)
    show_banner
    show_history "client.log" "━━━ CLIENT LOGS ━━━"
    echo ""
    show_history "server*.log" "━━━ SERVER LOGS ━━━"
    echo ""
    show_history "loadbalancer.log" "━━━ LOAD BALANCER LOGS ━━━"
    ;;
  history-client)
    show_history "client.log" "CLIENT LOGS"
    ;;
  history-servers)
    show_history "server*.log" "SERVER LOGS"
    ;;
  history-lb)
    show_history "loadbalancer.log" "LOAD BALANCER LOGS"
    ;;
  list)
    list_logs
    ;;
  *)
    echo "Unknown mode: $MODE"
    echo ""
    print_help
    exit 1
    ;;
esac
