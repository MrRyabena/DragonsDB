#!/bin/bash
# Build DragonsDB with modular structure

echo "Building DragonsDB..."
echo "===================="

# Remove target directories manually before Maven (helps with WSL permission issues)
echo "Cleaning target directories..."
rm -rf target common/target client/target server/target 2>/dev/null || true

# Also clean build directories
find . -name "build" -type d -exec rm -rf {} + 2>/dev/null || true

# Build all modules (skip clean since we already did manual cleanup)
echo "Running Maven build..."
mvn package -DskipTests -T 1

if [ $? -eq 0 ]; then
    echo ""
    echo "Build successful!"
    echo "===================="
    echo "Built modules:"
    echo " - common/target/DragonsDB-common.jar (shared classes: dragon, core, collection, storage, ui)"
    echo " - client/target/DragonsDB-client.jar (main: client.Client)"
    echo " - server/target/DragonsDB-server.jar (main: server.Server)"
    echo ""
    echo "Executable JARs:"
    ls -lh client/target/DragonsDB-client.jar 2>/dev/null || echo "  (client JAR not found)"
    ls -lh server/target/DragonsDB-server.jar 2>/dev/null || echo "  (server JAR not found)"
else
    echo "Build failed!"
    exit 1
fi
