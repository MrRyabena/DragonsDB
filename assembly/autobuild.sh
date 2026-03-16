
mvn clean

mvn package -DskipTests

java --add-opens java.base/java.time=ALL-UNNAMED -jar target/JavaProject.jar
