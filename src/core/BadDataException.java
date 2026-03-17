package core;

/**
 * Runtime exception indicating that provided data violates domain constraints.
 */
public class BadDataException extends RuntimeException {
    /** Creates an exception without message. */
    public BadDataException() {
        super();
    }

    /**
     * Creates an exception with a message.
     *
     * @param message error details
     */
    public BadDataException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and a root cause.
     *
     * @param message error details
     * @param cause root cause
     */
    public BadDataException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns a human-readable message with a fixed prefix.
     *
     * @return formatted error message
     */
    @Override
    public String getMessage() {
        return "Incorrect data: " + super.getMessage();
    }
}
