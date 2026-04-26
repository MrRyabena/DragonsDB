package client.mvvm.model;

/** Immutable credentials used by the MVVM service layer. */
public record Credentials(String login, String password) {
    public Credentials {
        if (login == null) {
            login = "";
        }
        if (password == null) {
            password = "";
        }
    }

    public boolean isEmpty() {
        return login.isBlank() || password.isBlank();
    }
}
