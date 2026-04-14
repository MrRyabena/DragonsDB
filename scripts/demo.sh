#!/usr/bin/env bash
# DragonsDB Lab 7 - One-line Demo Setup
# Run this to start everything needed for a demonstration

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cat <<'EOF'
╔════════════════════════════════════════════════════════════════════════════════╗
║                    DragonsDB Lab 7 - Demo Setup                              ║
║                                                                                ║
║  This script will:                                                            ║
║  1. Archive previous demo logs (if any)                                       ║
║  2. Stop old processes                                                        ║
║  3. Start fresh system (3 servers + LoadBalancer)                             ║
║  4. Open an interactive log monitor                                           ║
║                                                                                ║
║  Then open another terminal and run: ./run-client.sh                          ║
║                                                                                ║
╚════════════════════════════════════════════════════════════════════════════════╝

EOF

echo "Step 1: Managing logs..."
if [[ -d "../logs" ]]; then
  backup_count=$(ls -1d ../logs.backup.* 2>/dev/null | wc -l)
  if [[ -f "../logs/client.log" ]] || [[ -f "../logs/server.log" ]]; then
    echo "  📦 Archiving previous logs..."
    mkdir -p "../logs.backup.$(date +%Y%m%d_%H%M%S)"
    cp ../logs/*.log "../logs.backup.$(date +%Y%m%d_%H%M%S)/" 2>/dev/null || true
    rm -f ../logs/*.log 2>/dev/null || true
  fi
fi

echo "Step 2: Stopping old processes (if any)..."
./start.sh stop 2>/dev/null || true

echo "Step 3: Starting system..."
./start.sh start

sleep 2

echo ""
echo "╔════════════════════════════════════════════════════════════════════════════════╗"
echo "║                        ✅ System is Ready!                                    ║"
echo "╚════════════════════════════════════════════════════════════════════════════════╝"
echo ""
echo "📊 Starting Live Log Monitor..."
echo ""
echo "What you should see:"
echo "  • [CLIENT] - when client sends commands"
echo "  • [SERVERS] - when servers process requests"
echo "  • [LB] - when LoadBalancer routes packets"
echo ""
echo "To interact with the system:"
echo "  1. Open another terminal"
echo "  2. Run: ./run-client.sh"
echo "  3. Type commands (e.g., 'register User pass')"
echo "  4. Watch the logs update here!"
echo ""
echo "Press Ctrl+C to stop log monitoring (system keeps running)"
echo ""
echo "════════════════════════════════════════════════════════════════════════════════"
echo ""

./logs-monitor.sh all
