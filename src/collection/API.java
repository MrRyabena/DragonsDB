package collection;

import java.util.function.Predicate;
import java.util.stream.Stream;

import dragon.Dragon;

/**
 * Abstraction for working with a collection of {@link Dragon} entities.
 *
 * <p>Provides basic CRUD-like operations and stream-based querying.
 */
public interface API {
    /**
     * Adds a single element to the collection.
     *
     * @param element the element to add
     */
    void add(Dragon element);

    /**
     * Adds multiple elements to the collection.
     *
     * @param elements stream of elements to add
     */
    void add(Stream<Dragon> elements);

    /**
     * Updates an element identified by its id.
     *
     * <p>Typical behavior is to remove existing element with the same id and then add the given one.
     *
     * @param element new element value (may be null depending on implementation)
     */
    void updateById(Dragon element);

    /** Removes all elements from the collection. */
    void clear();

    /**
     * Returns a sequential stream of elements in the collection.
     *
     * @return stream of elements
     */
    Stream<Dragon> getStream();

    /**
     * Removes all elements that match the given filter.
     *
     * @param filter predicate used to select elements to remove
     */
    void removeIf(Predicate<? super Dragon> filter);

    /**
     * Counts the number of elements that match the given filter.
     *
     * @param filter predicate used to select elements to count
     * @return number of matching elements
     */
    int countIf(Predicate<? super Dragon> filter);
}
