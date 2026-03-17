package collection;

import java.util.function.Predicate;
import java.util.stream.Stream;

import dragon.Dragon;

public interface API {
    void add(Dragon element);

    void add(Stream<Dragon> elements);

    void updateById(Dragon element);

    void clear();

    Stream<Dragon> getStream();

    void removeIf(Predicate<? super Dragon> filter);

    int countIf(Predicate<? super Dragon> filter);
}