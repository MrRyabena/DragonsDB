package core;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Application-wide default constants.
 */
public final class Defaults {
    /** Default path used when the environment variable is not provided. */
    public static final String STORAGE_PATH = "data.json";

    /** Charset used for all text I/O operations. */
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    public static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 5000;

    /** PostgreSQL default host used by storage layer. */
    public static final String DB_HOST = "localhost";

    /** PostgreSQL default port used by storage layer. */
    public static final int DB_PORT = 5432;

    /** PostgreSQL default database name used by storage layer. */
    public static final String DB_NAME = "studs";

    /** Timeout for UDP request/response from client side when server is unavailable. */
    public static final int RESPONSE_TIMEOUT = 500;
}
