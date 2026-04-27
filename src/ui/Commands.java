package ui;

/**
 * Supported command list for the interactive UI.
 *
 * <p>Each command has a stable text form as entered by the user and a derived integer code.
 */
public enum Commands {

    HELP("help"),
    INFO("info"),
    ADD("add"),
    SHOW("show"),
    SHOW_WITH_OWNERS("show_with_owners"),
    UPDATE_BY_ID("update_by_id"),
    REMOVE_BY_ID("remove_by_id"),
    CLEAR("clear"),
    SAVE("save"),
    EXECUTE_SCRIPT("execute_script"),
    EXIT("exit"),
    REMOVE_GREATER("remove_greater"),
    REMOVE_LOWER("remove_lower"),
    HISTORY("history"),
    COUNT_BY_TYPE("count_by_type"),
    COUNT_GREATER_THAN_TYPE("count_greater_than_type"),
    FILTER_STARTS_WITH_NAME("filter_starts_with_name");

    private final int code;
    private final String text;

    /**
     * Creates a command descriptor.
     *
     * @param text command text as typed by the user
     */
    Commands(String text) {
        this.text = text;
        this.code = text.hashCode();
    }

    /**
     * Returns the integer command code.
     *
     * @return command code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the command text.
     *
     * @return command text
     */
    public String getText() {
        return text;
    }

}
