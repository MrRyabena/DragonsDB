package storage;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.log4j.Logger;

import core.Coordinates;
import dragon.Dragon;
import dragon.DragonHead;
import dragon.DragonType;

/** PostgreSQL DAO for dragon rows. */
public final class PostgresDragonRepository {
    private static final Logger logger = Logger.getLogger(PostgresDragonRepository.class);
    private static final String TABLE = PostgresSupport.DEFAULT_DRAGON_TABLE;

    private PostgresDragonRepository() {}

    public static List<Dragon> loadAll() {
        List<Dragon> dragons = new ArrayList<>();
        try (Connection connection = PostgresSupport.openConnection()) {
            PostgresSupport.ensureDragonSchema(connection);
            try (PreparedStatement statement =
                    connection.prepareStatement(
                            "select id, name, x, y, creation_date, age, weight, speaking, type, head_size, head_tooth_count, owner_login from "
                                    + TABLE
                                    + " order by id")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        dragons.add(mapDragon(resultSet));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load dragons from PostgreSQL", e);
        }
        return dragons;
    }

    public static Dragon insert(Dragon dragon, String ownerLogin) throws SQLException {
        try (Connection connection = PostgresSupport.openConnection()) {
            PostgresSupport.ensureDragonSchema(connection);
            connection.setAutoCommit(false);
            try (PreparedStatement statement =
                    connection.prepareStatement(
                            "insert into "
                                    + TABLE
                                    + " (name, x, y, creation_date, modified_at, age, weight, speaking, type, head_size, head_tooth_count, owner_login) "
                                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id, creation_date, modified_at")) {
                bindDragon(statement, dragon, ownerLogin);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Failed to insert dragon");
                    }
                    long generatedId = resultSet.getLong("id");
                    LocalDateTime creationDate = resultSet.getTimestamp("creation_date").toLocalDateTime();
                    assignDatabaseFields(dragon, generatedId, creationDate);
                }
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
            connection.commit();
            return dragon;
        }
    }

    public static void update(Dragon dragon, String ownerLogin) throws SQLException {
        try (Connection connection = PostgresSupport.openConnection()) {
            PostgresSupport.ensureDragonSchema(connection);
            connection.setAutoCommit(false);
            String sql =
                    "update "
                            + TABLE
                            + " set name = ?, x = ?, y = ?, modified_at = now(), age = ?, weight = ?, speaking = ?, type = ?, head_size = ?, head_tooth_count = ? where id = ?";
            if (!isBlank(ownerLogin)) {
                sql += " and owner_login = ?";
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindDragonUpdate(statement, dragon, ownerLogin);
                int affected = statement.executeUpdate();
                if (affected == 0) {
                    if (!isBlank(ownerLogin) && existsById(connection, dragon.getId())) {
                        throw new SecurityException("Access denied: dragon belongs to another user");
                    }
                    throw new SQLException("Dragon not found");
                }
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
            connection.commit();
        }
    }

    public static void deleteById(long id) throws SQLException {
        deleteByIds(List.of(id));
    }

    public static void deleteById(long id, String ownerLogin) throws SQLException {
        try (Connection connection = PostgresSupport.openConnection()) {
            PostgresSupport.ensureDragonSchema(connection);
            connection.setAutoCommit(false);
            String sql = "delete from " + TABLE + " where id = ?";
            if (!isBlank(ownerLogin)) {
                sql += " and owner_login = ?";
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                if (!isBlank(ownerLogin)) {
                    statement.setString(2, ownerLogin);
                }

                int affected = statement.executeUpdate();
                if (affected == 0) {
                    if (!isBlank(ownerLogin) && existsById(connection, id)) {
                        throw new SecurityException("Access denied: dragon belongs to another user");
                    }
                    throw new SQLException("Dragon not found");
                }
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
            connection.commit();
        }
    }

    public static void deleteByIds(List<Long> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        try (Connection connection = PostgresSupport.openConnection()) {
            PostgresSupport.ensureDragonSchema(connection);
            connection.setAutoCommit(false);
            try (PreparedStatement statement =
                    connection.prepareStatement("delete from " + TABLE + " where id = ?")) {
                for (Long id : ids) {
                    statement.setLong(1, id);
                    statement.addBatch();
                }
                statement.executeBatch();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
            connection.commit();
        }
    }

    public static void deleteByIds(List<Long> ids, String ownerLogin) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        try (Connection connection = PostgresSupport.openConnection()) {
            PostgresSupport.ensureDragonSchema(connection);
            connection.setAutoCommit(false);
            String sql = "delete from " + TABLE + " where id = ?";
            if (!isBlank(ownerLogin)) {
                sql += " and owner_login = ?";
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Long id : ids) {
                    statement.setLong(1, id);
                    if (!isBlank(ownerLogin)) {
                        statement.setString(2, ownerLogin);
                    }
                    statement.addBatch();
                }
                statement.executeBatch();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
            connection.commit();
        }
    }

    public static void clear() throws SQLException {
        try (Connection connection = PostgresSupport.openConnection()) {
            PostgresSupport.ensureDragonSchema(connection);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("delete from " + TABLE);
            }
        }
    }

    public static int clear(String ownerLogin) throws SQLException {
        try (Connection connection = PostgresSupport.openConnection()) {
            PostgresSupport.ensureDragonSchema(connection);
            String sql = "delete from " + TABLE;
            if (!isBlank(ownerLogin)) {
                sql += " where owner_login = ?";
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (!isBlank(ownerLogin)) {
                    statement.setString(1, ownerLogin);
                }
                return statement.executeUpdate();
            }
        }
    }

    public static Set<Long> findOwnedIds(List<Long> ids, String ownerLogin) throws SQLException {
        if (ids == null || ids.isEmpty() || isBlank(ownerLogin)) {
            return Set.of();
        }

        Set<Long> result = new HashSet<>();
        try (Connection connection = PostgresSupport.openConnection()) {
            PostgresSupport.ensureDragonSchema(connection);
            try (PreparedStatement statement =
                    connection.prepareStatement(
                            "select id from " + TABLE + " where id = ? and owner_login = ?")) {
                for (Long id : ids) {
                    statement.setLong(1, id);
                    statement.setString(2, ownerLogin);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            result.add(resultSet.getLong("id"));
                        }
                    }
                }
            }
        }
        return result;
    }

    public static List<String> findOwnerLogins(List<Long> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<String> result = new ArrayList<>(ids.size());
        try (Connection connection = PostgresSupport.openConnection()) {
            PostgresSupport.ensureDragonSchema(connection);
            try (PreparedStatement statement =
                    connection.prepareStatement("select owner_login from " + TABLE + " where id = ?")) {
                for (Long id : ids) {
                    statement.setLong(1, id);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        result.add(resultSet.next() ? resultSet.getString("owner_login") : "");
                    }
                }
            }
        }
        return result;
    }

    public static Date getDateCreated() {
        return aggregateTimestamp("min(creation_date)");
    }

    public static Date getDateModified() {
        return aggregateTimestamp("max(modified_at)");
    }

    private static Date aggregateTimestamp(String function) {
        try (Connection connection = PostgresSupport.openConnection()) {
            PostgresSupport.ensureDragonSchema(connection);
            try (PreparedStatement statement =
                    connection.prepareStatement("select " + function + " as value from " + TABLE)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Timestamp timestamp = resultSet.getTimestamp("value");
                        return timestamp == null ? null : new Date(timestamp.getTime());
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to read collection timestamp from PostgreSQL", e);
        }
        return null;
    }

    private static Dragon mapDragon(ResultSet resultSet) throws SQLException {
        Coordinates coordinates = new Coordinates(resultSet.getFloat("x"), resultSet.getFloat("y"));
        DragonType type = parseDragonType(resultSet.getString("type"));
        DragonHead head = new DragonHead(resultSet.getFloat("head_size"), resultSet.getFloat("head_tooth_count"));
        Dragon dragon = new Dragon(
                resultSet.getString("name"),
                coordinates,
                resultSet.getInt("age"),
                resultSet.getLong("weight"),
                type,
                head);
        boolean speaking = resultSet.getBoolean("speaking");
        LocalDateTime creationDate = resultSet.getTimestamp("creation_date").toLocalDateTime();
        long id = resultSet.getLong("id");
        assignDatabaseFields(dragon, id, creationDate);
        setBooleanField(dragon, "speaking", speaking);
        return dragon;
    }

    private static void bindDragon(PreparedStatement statement, Dragon dragon, String ownerLogin)
            throws SQLException {
        statement.setString(1, dragon.getName());
        statement.setFloat(2, dragon.getCoordinates().getX());
        statement.setFloat(3, dragon.getCoordinates().getY());
        statement.setTimestamp(4, Timestamp.valueOf(dragon.getCreationDate()));
        statement.setTimestamp(5, Timestamp.valueOf(dragon.getCreationDate()));
        statement.setInt(6, dragon.getAge());
        statement.setLong(7, dragon.getWeight());
        statement.setBoolean(8, dragon.isSpeaking());
        if (dragon.getType() == null) {
            statement.setNull(9, java.sql.Types.VARCHAR);
        } else {
            statement.setString(9, dragon.getType().name());
        }
        statement.setFloat(10, dragon.getHead().getSize());
        statement.setFloat(11, dragon.getHead().getToothCount());
        statement.setString(12, ownerLogin);
    }

    private static void bindDragonUpdate(PreparedStatement statement, Dragon dragon, String ownerLogin)
            throws SQLException {
        statement.setString(1, dragon.getName());
        statement.setFloat(2, dragon.getCoordinates().getX());
        statement.setFloat(3, dragon.getCoordinates().getY());
        statement.setInt(4, dragon.getAge());
        statement.setLong(5, dragon.getWeight());
        statement.setBoolean(6, dragon.isSpeaking());
        if (dragon.getType() == null) {
            statement.setNull(7, java.sql.Types.VARCHAR);
        } else {
            statement.setString(7, dragon.getType().name());
        }
        statement.setFloat(8, dragon.getHead().getSize());
        statement.setFloat(9, dragon.getHead().getToothCount());
        statement.setLong(10, dragon.getId());
        if (!isBlank(ownerLogin)) {
            statement.setString(11, ownerLogin);
        }
    }

    private static boolean existsById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("select 1 from " + TABLE + " where id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static DragonType parseDragonType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return DragonType.valueOf(type.trim().toUpperCase(Locale.ROOT));
    }

    private static void assignDatabaseFields(Dragon dragon, long id, LocalDateTime creationDate) {
        setField(dragon, "id", id);
        setField(dragon, "creationDate", creationDate);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = Dragon.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field '" + fieldName + "'", e);
        }
    }

    private static void setBooleanField(Dragon target, String fieldName, boolean value) {
        try {
            Field field = Dragon.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setBoolean(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field '" + fieldName + "'", e);
        }
    }
}
