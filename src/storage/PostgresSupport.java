package storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Shared PostgreSQL connection and schema helpers. */
public final class PostgresSupport {
    public static final String DEFAULT_URL = "jdbc:postgresql://pg:s502499/studs";
    public static final String DEFAULT_DRAGON_TABLE = "dragons";

    private PostgresSupport() {}

    public static Connection openConnection() throws SQLException {
        String url = firstNonBlank(
                System.getenv("DATABASE_URL"),
                System.getenv("PGJDBC_URL"),
                DEFAULT_URL);
        String user = firstNonBlank(
                System.getenv("PGUSER"),
                System.getenv("DB_USER"),
                System.getenv("POSTGRES_USER"));
        String password = firstNonBlank(
                System.getenv("PGPASSWORD"),
                System.getenv("DB_PASSWORD"),
                System.getenv("POSTGRES_PASSWORD"));

        if (isBlank(user) || isBlank(password)) {
            return DriverManager.getConnection(url);
        }
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
