package collection;

import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import dragon.Dragon;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Collection implements Interaction {
    public Collection() {
        this.buffer = new HashSet<>();
    }

    public Collection(Set<Dragon> buffer) {
        this.buffer = new HashSet<>(buffer);
    }

    @Override
    public void add(Dragon element) {
        buffer.add(element);
    }

    @Override
    public void updateId(long id, Dragon element) {
        buffer.removeIf(e -> e.getId() == id);
        if (element != null)
            buffer.add(element);
    }

    @Override
    public void removeById(long id) {
        buffer.removeIf(e -> e.getId() == id);
    }

    @Override
    public void removeGreater(Dragon lower) {
        buffer.removeIf(e -> e.compareTo(lower) > 0);
    }

    @Override
    public void clear() {
        buffer.clear();
    }

    @Override
    public int countByType(dragon.DragonType type) {
        return countIf(e -> e.getType() == type);
    }

    @Override
    public int countGreaterThanType(dragon.DragonType type) {
        return countIf(e -> e.getType().compareTo(type) > 0);
    }

    private int countIf(Predicate<? super Dragon> filter) {
        int counter = 0;

        for (var x : buffer)
            if (filter.test(x))
                counter++;

        return counter;
    }

    @Override
    public Set<Dragon> filterStartsWithName(String sub_name) {
        var output = new HashSet<Dragon>();
        for (var x : buffer)
            if (x.getName().startsWith(sub_name))
                output.add(x);

        return Collections.unmodifiableSet(output);
    }

    public Stream<Dragon> getStream()
    {
        return buffer.stream();
    }

    @Override
    public Set<Dragon> getBuffer() {
        return Collections.unmodifiableSet(buffer);
    }

    private HashSet<Dragon> buffer;

}
