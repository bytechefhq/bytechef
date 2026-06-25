/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import com.bytechef.ee.platform.licence.domain.LicenceEntity;
import com.bytechef.ee.platform.licence.repository.LicenceRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link LicenceRepository} for unit tests. Only the methods used by {@link OfflineLicenceManager} are
 * implemented; all others throw {@link UnsupportedOperationException}.
 *
 * @version ee
 */
class InMemoryLicenceRepository implements LicenceRepository {

    private final Map<Long, LicenceEntity> store = new HashMap<>();
    private long nextId = 1;

    @Override
    public <S extends LicenceEntity> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }

        store.put(entity.getId(), entity);

        return entity;
    }

    @Override
    public <S extends LicenceEntity> Iterable<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();

        for (S entity : entities) {
            saved.add(save(entity));
        }

        return saved;
    }

    @Override
    public Iterable<LicenceEntity> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Iterable<LicenceEntity> findAllById(Iterable<Long> ids) {
        throw new UnsupportedOperationException("findAllById not supported in InMemoryLicenceRepository");
    }

    @Override
    public Optional<LicenceEntity> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public void delete(LicenceEntity entity) {
        if (entity.getId() != null) {
            store.remove(entity.getId());
        }
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        for (Long id : ids) {
            store.remove(id);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends LicenceEntity> entities) {
        for (LicenceEntity entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
