package collection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        add(element, null);
    }

    public void add(Dragon element, String ownerLogin) {
        lock.writeLock().lock();
        try {
            PostgresDragonRepository.insert(element, ownerLogin);
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
        updateById(element, null);
    }

    public void updateById(Dragon element, String ownerLogin) {
        lock.writeLock().lock();
        try {
            PostgresDragonRepository.update(element, ownerLogin);
            super.updateById(element);
        } catch (SecurityException e) {
            throw e;
        } catch (SQLException e) {
            if ("Dragon not found".equalsIgnoreCase(e.getMessage())) {
                throw new IllegalArgumentException("Dragon not found");
            }
            throw new IllegalStateException("Failed to update dragon in PostgreSQL", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        clear(null);
    }

    public int clear(String ownerLogin) {
        lock.writeLock().lock();
        try {
            if (ownerLogin == null || ownerLogin.isBlank()) {
                PostgresDragonRepository.clear();
                int removed = super.countIf(d -> true);
                super.clear();
                return removed;
            }

            Set<Long> ownedIds =
                    PostgresDragonRepository.findOwnedIds(
                            super.getStream().map(Dragon::getId).collect(Collectors.toList()), ownerLogin);
            if (ownedIds.isEmpty()) {
                return 0;
            }
            int removed = PostgresDragonRepository.clear(ownerLogin);
            super.removeIf(d -> ownedIds.contains(d.getId()));
            return removed;
        } catch (SecurityException e) {
            throw e;
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
        removeIf(filter, null);
    }

    public int removeIf(Predicate<? super Dragon> filter, String ownerLogin) {
        lock.writeLock().lock();
        try {
            List<Dragon> targets = super.getStream().filter(filter).collect(Collectors.toList());
            if (targets.isEmpty()) {
                return 0;
            }
            List<Long> ids = new ArrayList<>();
            for (Dragon dragon : targets) {
                ids.add(dragon.getId());
            }

            if (ownerLogin == null || ownerLogin.isBlank()) {
                PostgresDragonRepository.deleteByIds(ids);
                super.removeIf(filter);
                return ids.size();
            }

            Set<Long> ownedIds = PostgresDragonRepository.findOwnedIds(ids, ownerLogin);
            if (ownedIds.isEmpty()) {
                return 0;
            }
            PostgresDragonRepository.deleteByIds(new ArrayList<>(ownedIds), ownerLogin);
            super.removeIf(d -> filter.test(d) && ownedIds.contains(d.getId()));
            return ownedIds.size();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove dragons from PostgreSQL", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeById(long id, String ownerLogin) {
        lock.writeLock().lock();
        try {
            PostgresDragonRepository.deleteById(id, ownerLogin);
            super.removeIf(d -> d.getId() == id);
        } catch (SecurityException e) {
            throw e;
        } catch (SQLException e) {
            if ("Dragon not found".equalsIgnoreCase(e.getMessage())) {
                throw new IllegalArgumentException("Dragon not found");
            }
            throw new IllegalStateException("Failed to remove dragon from PostgreSQL", e);
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
