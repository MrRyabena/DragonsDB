package client.mvvm.model;

/** View filter parameters for client-side stream filtering/sorting. */
public class DragonsFilter {
    public enum SortField {
        ID,
        NAME,
        AGE,
        WEIGHT,
        CREATED_AT
    }

    public String query = "";
    public SortField sortField = SortField.ID;
    public boolean ascending = true;
}
