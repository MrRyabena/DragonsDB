
mvn clean
mvn package -DskipTests

echo "Built:"
echo " - target/DragonsDB-client.jar (main: client.Client)"
echo " - target/DragonsDB-server.jar (main: server.Server)"
