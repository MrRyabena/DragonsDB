package ui;

/**
 * Functional interface representing a command handler.
 */
public interface Executable {

    /**
     * Executes a command using the provided UI context.
     *
     * @param command parsed command data
     * @param ui UI context
     */
    void execute(Command command, UI ui);

}
