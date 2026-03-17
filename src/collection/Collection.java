package collection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dragon.Dragon;

/**
 * In-memory implementation of {@link API} backed by a {@link HashSet}.
 */
public class Collection implements collection.API {
    /** Creates an empty collection. */
    public Collection() {
        this.buffer = new HashSet<>();
    }

    /**
     * Creates a collection initialized from a set.
     *
     * @param buffer initial elements
     */
    public Collection(Set<Dragon> buffer) {
        this.buffer = new HashSet<>(buffer);
    }

    /**
     * Creates a collection initialized from a stream.
     *
     * @param stream initial elements stream
     */
    public Collection(Stream<Dragon> stream) {
        this(stream.collect(Collectors.toSet()));
    }

    /**
     * Adds a single element to the set.
     *
     * @param element element to add
     */
    @Override
    public void add(Dragon element) {
        buffer.add(element);
    }

    /**
     * Adds all elements from the provided stream.
     *
     * @param elements stream of elements to add
     */
    @Override
    public void add(Stream<Dragon> elements) {
        buffer.addAll(elements.collect(Collectors.toSet()));
    }

    /**
     * Replaces an element with the same id as the provided one.
     *
     * @param element replacement element
     */
    @Override
    public void updateById(Dragon element) {
        buffer.removeIf(e -> e.getId() == element.getId());
        if (element != null)
            buffer.add(element);
    }

    /**
     * Returns a stream view of the underlying set.
     *
     * @return stream of dragons
     */
    @Override
    public Stream<Dragon> getStream() {
        return buffer.stream();
    }

    /**
     * Removes all elements matching the predicate.
     *
     * @param filter predicate selecting elements to remove
     */
    @Override
    public void removeIf(Predicate<? super Dragon> filter) {
        buffer.removeIf(filter);
    }

    /**
     * Counts all elements matching the predicate.
     *
     * @param filter predicate selecting elements to count
     * @return number of matching elements
     */
    @Override
    public int countIf(Predicate<? super Dragon> filter) {
        int counter = 0;
        for (var x : buffer)
            if (filter.test(x))
                counter++;

        return counter;
    }

    @Override
    public void clear() {
        buffer.clear();
    }

    /**
     * Returns an unmodifiable view of the underlying set.
     *
     * @return unmodifiable set of elements
     */
    public Set<Dragon> getBuffer() {
        return Collections.unmodifiableSet(buffer);
    }

    /** Backing storage for the collection. */
    private HashSet<Dragon> buffer;

}
