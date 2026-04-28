package ui;

/**
 * Supported command list for the interactive CLI interface.
 * 
 * <p>Each command has a stable text form as entered by the user. Commands are used to identify
 * user requests, collect parameters, and execute server-side operations. Includes dragon collection
 * management, information queries, and admin operations.
 */
public enum Commands {

    /** Display help information */
    HELP("help"),
    /** Display general information about the collection */
    INFO("info"),
    /** Add new dragon to collection */
    ADD("add"),
    /** Display all dragons in collection */
    SHOW("show"),
    /** Display all dragons with owner information (parallel array of owner logins) */
    SHOW_WITH_OWNERS("show_with_owners"),
    /** Update dragon by ID */
    UPDATE_BY_ID("update_by_id"),
    /** Remove dragon by ID */
    REMOVE_BY_ID("remove_by_id"),
    /** Clear entire collection */
    CLEAR("clear"),
    /** Save collection to file */
    SAVE("save"),
    /** Execute commands from script file */
    EXECUTE_SCRIPT("execute_script"),
    /** Exit application */
    EXIT("exit"),
    /** Remove dragons greater than specified */
    REMOVE_GREATER("remove_greater"),
    /** Remove dragons lower than specified */
    REMOVE_LOWER("remove_lower"),
    /** Display command execution history */
    HISTORY("history"),
    /** Count dragons by type */
    COUNT_BY_TYPE("count_by_type"),
    /** Count dragons greater than specified type */
    COUNT_GREATER_THAN_TYPE("count_greater_than_type"),
    /** Filter dragons starting with name */
    FILTER_STARTS_WITH_NAME("filter_starts_with_name");

    private final int code;
    private final String text;

    /**
     * Creates a command descriptor.
     *
     * @param text command text as entered by the user
     */
    Commands(String text) {
        this.text = text;
        this.code = text.hashCode();
    }

    /**
     * Returns the integer command code derived from command text.
     *
     * @return command code (hash of command text)
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the command text.
     *
     * @return text representation of command as entered by user
     */
    public String getText() {
        return text;
    }

}
