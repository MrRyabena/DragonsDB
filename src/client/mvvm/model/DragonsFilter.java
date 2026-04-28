package client.mvvm.model;

/**
 * Filter and sort configuration for client-side dragon list queries.
 *
 * <p>Encapsulates search query and sort preferences. Mutable record-like class used by
 * DragonsTableViewModel and DragonsQueryService for filtering dragon collections.
 */
public class DragonsFilter {
    /**
     * Sort field options.
     */
    public enum SortField {
        /** Sort by dragon ID */
        ID,
        /** Sort by dragon name */
        NAME,
        /** Sort by dragon age */
        AGE,
        /** Sort by dragon weight */
        WEIGHT,
        /** Sort by creation date */
        CREATED_AT
    }

    /** Search query (case-insensitive substring matching) */
    public String query = "";
    /** Sort field to use for ordering */
    public SortField sortField = SortField.ID;
    /** Sort direction (true = ascending, false = descending) */
    public boolean ascending = true;
}
