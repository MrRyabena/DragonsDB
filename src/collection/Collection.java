package collection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dragon.Dragon;

public class Collection implements collection.API {
    public Collection() {
        this.buffer = new HashSet<>();
    }

    public Collection(Set<Dragon> buffer) {
        this.buffer = new HashSet<>(buffer);
    }

    public Collection(Stream<Dragon> stream) {
        this(stream.collect(Collectors.toSet()));
    }

    @Override
    public void add(Dragon element) {
        buffer.add(element);
    }

    @Override
    public void add(Stream<Dragon> elements) {
        buffer.addAll(elements.collect(Collectors.toSet()));
    }

    @Override
    public void updateById(Dragon element) {
        buffer.removeIf(e -> e.getId() == element.getId());
        if (element != null)
            buffer.add(element);
    }

    @Override
    public Stream<Dragon> getStream() {
        return buffer.stream();
    }

    @Override
    public void removeIf(Predicate<? super Dragon> filter) {
        buffer.removeIf(filter);
    }

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

    public Set<Dragon> getBuffer() {
        return Collections.unmodifiableSet(buffer);
    }

    private HashSet<Dragon> buffer;

}
