#!/usr/bin/env bash
# Quick log viewers with sensible defaults

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Show logs selection menu
if [[ $# -eq 0 ]]; then
  cat <<EOF
DragonsDB Lab 7 - Quick Log Viewers

Usage: $(basename "$0") <command>

Commands:
  watch          Watch all logs in real-time (best for demo!)
  follow-lb      Follow LoadBalancer logs
  follow-servers Follow Server logs  
  follow-client  Follow Client logs
  less-all       Browse all logs with pager
  history        Show recent logs (last 200 lines)
  clean          Archive old logs and clear
  
Examples:
  $(basename "$0") watch           # Start monitoring
  $(basename "$0") follow-lb       # Just LB
  $(basename "$0") history         # See recent events

EOF
  exit 0
fi

case "${1:-}" in
  watch)
    "$SCRIPT_DIR/logs-monitor.sh" all
    ;;
  follow-lb)
    "$SCRIPT_DIR/logs-monitor.sh" lb rt
    ;;
  follow-servers)
    "$SCRIPT_DIR/logs-monitor.sh" servers rt
    ;;
  follow-client)
    "$SCRIPT_DIR/logs-monitor.sh" client rt
    ;;
  less-all)
    "$SCRIPT_DIR/logs-monitor.sh" combined
    ;;
  history)
    "$SCRIPT_DIR/logs-monitor.sh" history-all
    ;;
  clean)
    read -p "Archive logs to logs.backup.$(date +%s)? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
      mkdir -p "../logs.backup.$(date +%s)"
      mv ../logs/*.log "../logs.backup.$(date +%s)/" 2>/dev/null || true
      echo "Logs archived and cleared"
    fi
    ;;
  *)
    echo "Unknown command: $1"
    "$0"
    exit 1
    ;;
esac
