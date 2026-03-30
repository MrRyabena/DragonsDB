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
}
