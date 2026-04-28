package collection;

/**
 * Internal API command codes for audit logging and transaction tracking.
 * 
 * <p>Mirrors the Commands enum but used server-side for CommandLogger to record
 * which commands were executed during the session.
 */
public enum ApiCommand {
    HELP,
    INFO,
    ADD,
    SHOW,
    SHOW_WITH_OWNERS,
    UPDATE_BY_ID,
    REMOVE_BY_ID,
    CLEAR,
    SAVE,
    EXECUTE_SCRIPT,
    EXIT,
    REMOVE_GREATER,
    REMOVE_LOWER,
    HISTORY,
    COUNT_BY_TYPE,
    COUNT_GREATER_THAN_TYPE,
    FILTER_STARTS_WITH_NAME,
    GET_STREAM,
    REMOVE_IF,
    COUNT_IF
}
