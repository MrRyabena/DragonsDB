package client.mvvm.model;

/**
 * Immutable login credentials record.
 *
 * <p>Encapsulates username and password for authentication. Empty strings are used
 * as defaults for null values to ensure non-null fields.
 */
public record Credentials(String login, String password) {
    public Credentials {
        if (login == null) {
            login = "";
        }
        if (password == null) {
            password = "";
        }
    }

    /**
     * Checks if credentials are empty.
     *
     * @return true if login or password is blank
     */
    public boolean isEmpty() {
        return login.isBlank() || password.isBlank();
    }
}
