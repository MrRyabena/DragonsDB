package storage;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;

/** Shared PostgreSQL connection and schema helpers. */
public final class PostgresSupport {
    public static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/studs";
    public static final String DEFAULT_DRAGON_TABLE = "dragons";
    private static final Logger logger = Logger.getLogger(PostgresSupport.class);
    private static final String DEFAULT_DB_CONFIG_PATH = "db.cfg";

    private PostgresSupport() {}

    public static Connection openConnection() throws SQLException {
        Properties fileConfig = loadDbConfig();

        String url = firstNonBlank(
                System.getenv("DATABASE_URL"),
                System.getenv("PGJDBC_URL"),
                trimToNull(fileConfig.getProperty("url")),
                DEFAULT_URL);
        String user = firstNonBlank(
                System.getenv("PGUSER"),
                System.getenv("DB_USER"),
                System.getenv("POSTGRES_USER"),
                trimToNull(fileConfig.getProperty("user")));
        String password = firstNonBlank(
                System.getenv("PGPASSWORD"),
                System.getenv("DB_PASSWORD"),
                System.getenv("POSTGRES_PASSWORD"),
                trimToNull(fileConfig.getProperty("password")));

        if (isBlank(user) || isBlank(password)) {
            logger.info("Connecting to PostgreSQL without explicit credentials, url=" + url);
            return DriverManager.getConnection(url);
        }
        logger.info("Connecting to PostgreSQL, url=" + url + ", user=" + user);
        return DriverManager.getConnection(url, user, password);
    }

    public static void ensureDragonSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "create table if not exists "
                            + DEFAULT_DRAGON_TABLE
                            + " ("
                            + "id bigserial primary key, "
                            + "name varchar(255) not null, "
                            + "x real not null, "
                            + "y real not null, "
                            + "creation_date timestamp not null default now(), "
                            + "modified_at timestamp not null default now(), "
                            + "age integer not null check (age > 0), "
                            + "weight bigint not null check (weight > 0), "
                            + "speaking boolean not null default false, "
                            + "type varchar(32), "
                            + "head_size real not null, "
                            + "head_tooth_count real not null, "
                            + "owner_login varchar(64)"
                            + ")");
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static Properties loadDbConfig() {
        Properties properties = new Properties();
        String configPath = firstNonBlank(System.getenv("DB_CONFIG_PATH"), DEFAULT_DB_CONFIG_PATH);
        try (FileInputStream inputStream = new FileInputStream(configPath)) {
            properties.load(inputStream);
            logger.info("Loaded database config from " + configPath);
        } catch (IOException e) {
            logger.warn("Database config file not found or unreadable: " + configPath + ". Using env/default values.");
        }
        return properties;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
