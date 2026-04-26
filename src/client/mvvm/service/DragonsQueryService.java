package client.mvvm.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import client.mvvm.model.DragonsFilter;
import dragon.Dragon;

/** Filtering and sorting helper based on Streams API for table data. */
public class DragonsQueryService {

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
