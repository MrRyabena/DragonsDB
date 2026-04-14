#!/usr/bin/env bash
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LOGS_DIR="$PROJECT_ROOT/logs"

if [[ $# -eq 0 ]]; then
  cat <<'EOF'
DragonsDB Lab 7 - Logs Management Utility

Usage: logs-manage.sh <action> [options]

Actions:
  clear           Clear all log files (interactive)
  archive         Archive logs with timestamp (non-destructive)
  list            List all existing logs
  size            Show total log size
  info            Show logs info and usage

Examples:
  ./logs-manage.sh clear          # Remove logs (with confirmation)
  ./logs-manage.sh archive        # Save to logs.backup.TIMESTAMP
  ./logs-manage.sh list           # See what's there
  ./logs-manage.sh size           # Disk usage

EOF
  exit 0
fi

action="$1"

case "$action" in
  clear)
    if [[ ! -d "$LOGS_DIR" ]]; then
      echo "No logs directory found: $LOGS_DIR"
      exit 1
    fi
    
    log_count=$(find "$LOGS_DIR" -maxdepth 1 -name "*.log" | wc -l)
    if [[ $log_count -eq 0 ]]; then
      echo "No log files to clean."
      exit 0
    fi
    
    echo "Found $log_count log files in $LOGS_DIR"
    echo ""
    find "$LOGS_DIR" -maxdepth 1 -name "*.log" -exec ls -lh {} \;
    echo ""
    read -p "Delete all log files? (y/N) " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
      find "$LOGS_DIR" -maxdepth 1 -name "*.log" -delete
      echo "✓ Log files deleted"
    else
      echo "Cancelled"
    fi
    ;;
    
  archive)
    if [[ ! -d "$LOGS_DIR" ]]; then
      echo "No logs directory found: $LOGS_DIR"
      exit 1
    fi
    
    log_count=$(find "$LOGS_DIR" -maxdepth 1 -name "*.log" | wc -l)
    if [[ $log_count -eq 0 ]]; then
      echo "No log files to archive."
      exit 0
    fi
    
    backup_dir="$PROJECT_ROOT/logs.backup.$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$backup_dir"
    
    find "$LOGS_DIR" -maxdepth 1 -name "*.log" -exec cp {} "$backup_dir/" \;
    
    echo "✓ Archived $log_count logs to: $(basename "$backup_dir")"
    ls -lh "$backup_dir"
    ;;
    
  list)
    if [[ ! -d "$LOGS_DIR" ]]; then
      echo "No logs directory found: $LOGS_DIR"
      exit 1
    fi
    
    echo "Log files in: $LOGS_DIR"
    echo ""
    
    if find "$LOGS_DIR" -maxdepth 1 -name "*.log" | grep -q .; then
      ls -lh "$LOGS_DIR"/*.log 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}'
    else
      echo "  (no log files yet)"
    fi
    
    if find "$LOGS_DIR" -maxdepth 1 -name "*.pid" | grep -q .; then
      echo ""
      echo "PID files (process tracking):"
      ls -lh "$LOGS_DIR"/*.pid 2>/dev/null | awk '{print "  " $9}'
    fi
    ;;
    
  size)
    if [[ ! -d "$LOGS_DIR" ]]; then
      echo "No logs directory found: $LOGS_DIR"
      exit 1
    fi
    
    total_size=$(du -sh "$LOGS_DIR" 2>/dev/null | awk '{print $1}')
    echo "Total logs size: $total_size"
    echo ""
    echo "Breakdown:"
    du -sh "$LOGS_DIR"/*.log 2>/dev/null | awk '{print "  " $2 ": " $1}'
    ;;
    
  info)
    cat <<EOF
DragonsDB Lab 7 - Log Files Information

Directory: $LOGS_DIR

Files:
  * client.log           - DragonsDB Client (Java) logs
  * server.log           - Aggregated server logs
  * server-5001.log      - Server instance on port 5001
  * server-5002.log      - Server instance on port 5002
  * server-5003.log      - Server instance on port 5003
  * loadbalancer.log     - UDP Load Balancer (C++) logs
  * server.pids          - PID tracking file
  * loadbalancer.pid     - PID tracking file

For real-time monitoring:
  cd scripts && ./logs-monitor.sh all

For quick access:
  cd scripts && ./logs.sh watch

For archiving before demo:
  cd scripts && ./logs-manage.sh archive

EOF
    ;;
    
  *)
    echo "Unknown action: $action"
    exit 1
    ;;
esac
