package client.mvvm.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import client.mvvm.model.DragonsFilter;
import dragon.Dragon;

/**
 * Provides filtering and sorting services for dragon collections.
 *
 * <p>Uses Stream API to apply query filters and sort dragons based on configurable criteria.
 * Filters by name, ID, age, weight, and coordinates using case-insensitive string matching.
 */
public class DragonsQueryService {

    /**
     * Applies filter and sort to dragon collection.
     *
     * @param source dragons to filter/sort
     * @param filter filter configuration (query, sortField, ascending)
     * @return filtered and sorted dragons
     */
    public List<Dragon> apply(List<Dragon> source, DragonsFilter filter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(filter, "filter");

        String query = filter.query == null ? "" : filter.query.trim().toLowerCase(Locale.ROOT);

        Comparator<Dragon> comparator = comparatorFor(filter.sortField);
        if (!filter.ascending) {
            comparator = comparator.reversed();
        }

        return source.stream()
                .filter(dragon -> matches(dragon, query))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Checks if dragon matches search query.
     *
     * @param dragon dragon to check
     * @param query search query (case-insensitive)
     * @return true if dragon matches
     */
    private boolean matches(Dragon dragon, String query) {
        if (query.isEmpty()) {
            return true;
        }

        return String.valueOf(dragon.getId()).contains(query)
                || dragon.getName().toLowerCase(Locale.ROOT).contains(query)
                || String.valueOf(dragon.getAge()).contains(query)
                || String.valueOf(dragon.getWeight()).contains(query)
                || String.valueOf(dragon.getCoordinates().getX()).contains(query)
                || String.valueOf(dragon.getCoordinates().getY()).contains(query)
                || String.valueOf(dragon.getCreationDate()).toLowerCase(Locale.ROOT).contains(query);
    }

    /**
     * Creates comparator for specified sort field.
     *
     * @param field sort field (NAME, AGE, WEIGHT, ID)
     * @return comparator for that field
     */
    private Comparator<Dragon> comparatorFor(DragonsFilter.SortField field) {
        return switch (field) {
            case NAME -> Comparator.comparing(Dragon::getName, String.CASE_INSENSITIVE_ORDER);
            case AGE -> Comparator.comparingInt(Dragon::getAge);
            case WEIGHT -> Comparator.comparingLong(Dragon::getWeight);
            case CREATED_AT -> Comparator.comparing(Dragon::getCreationDate);
            case ID -> Comparator.comparingLong(Dragon::getId);
        };
    }
}
