#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

CLIENT_JAR="${CLIENT_JAR:-DragonsDB-client.jar}"
CLIENT_JAVA_OPTS="${CLIENT_JAVA_OPTS:--Xms32m -Xmx256m -XX:MaxMetaspaceSize=128m}"

env -u _JAVA_OPTIONS -u JAVA_TOOL_OPTIONS java $CLIENT_JAVA_OPTS -jar "$CLIENT_JAR" "$@"
