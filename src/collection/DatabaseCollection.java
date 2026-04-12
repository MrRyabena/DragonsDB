package collection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import dragon.Dragon;
import storage.PostgresDragonRepository;

/** PostgreSQL-backed collection implementation that keeps an in-memory mirror. */
public class DatabaseCollection extends Collection {
    private static final Logger logger = Logger.getLogger(DatabaseCollection.class);

    public DatabaseCollection() {
        super(loadInitialStream());
    }

    @Override
    public void add(Dragon element) {
        lock.writeLock().lock();
        try {
            PostgresDragonRepository.insert(element, null);
            super.add(element);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add dragon to PostgreSQL", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void add(Stream<Dragon> elements) {
        if (elements == null) {
            return;
        }
        elements.forEach(this::add);
    }

    @Override
    public void updateById(Dragon element) {
        lock.writeLock().lock();
        try {
            PostgresDragonRepository.update(element, null);
            super.updateById(element);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update dragon in PostgreSQL", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            PostgresDragonRepository.clear();
            super.clear();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to clear PostgreSQL collection", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Stream<Dragon> getStream() {
        lock.readLock().lock();
        try {
            return super.getStream().collect(Collectors.toList()).stream();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void removeIf(Predicate<? super Dragon> filter) {
        lock.writeLock().lock();
        try {
            List<Dragon> targets = super.getStream().filter(filter).collect(Collectors.toList());
            if (targets.isEmpty()) {
                return;
            }
            List<Long> ids = new ArrayList<>();
            for (Dragon dragon : targets) {
                ids.add(dragon.getId());
            }
            PostgresDragonRepository.deleteByIds(ids);
            super.removeIf(filter);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove dragons from PostgreSQL", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int countIf(Predicate<? super Dragon> filter) {
        lock.readLock().lock();
        try {
            return super.countIf(filter);
        } finally {
            lock.readLock().unlock();
        }
    }

    private static Stream<Dragon> loadInitialStream() {
        try {
            return PostgresDragonRepository.loadAll().stream();
        } catch (Exception e) {
            logger.error("Failed to load initial collection from PostgreSQL", e);
            return Stream.empty();
        }
    }

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
}
