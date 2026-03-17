package ui;

public enum Commands {

    HELP("help"),
    INFO("info"),
    ADD("add"),
    SHOW("show"),
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

    Commands(String text) {
        this.text = text;
        this.code = text.hashCode();
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

}
