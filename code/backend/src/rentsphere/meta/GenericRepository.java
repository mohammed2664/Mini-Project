package rentsphere.meta;

import rentsphere.model.AbstractDomainEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic In-Memory Repository with Lambda Predicate Filtering.
 * MCA OOT Lab - Assignment 4 (Generic Programming & Meta-OOP)
 *
 * @param <T> Domain entity extending AbstractDomainEntity
 */
public class GenericRepository<T extends AbstractDomainEntity> {
    private final Map<String, T> storage = new ConcurrentHashMap<>();

    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Cannot save null entity.");
        }
        entity.touch();
        storage.put(entity.getId(), entity);
        return entity;
    }

    public Optional<T> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    public boolean deleteById(String id) {
        if (id == null) return false;
        return storage.remove(id) != null;
    }

    /**
     * Returns entities accepted by the supplied lambda or all entities when no
     * filter is supplied.
     *
     * @param predicate condition applied to each stored entity
     * @return matching entities in a new list
     */
    public List<T> findWhere(Predicate<T> predicate) {
        if (predicate == null) return findAll();
        return storage.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    public int count() {
        return storage.size();
    }

    public void clear() {
        storage.clear();
    }
}
