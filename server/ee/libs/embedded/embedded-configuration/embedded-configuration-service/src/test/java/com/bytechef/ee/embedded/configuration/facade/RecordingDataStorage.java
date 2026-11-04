/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.DataStorageScopeDeleter.ScopeData;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RecordingDataStorage implements DataStorage {

    private final AtomicInteger attempts = new AtomicInteger();
    private final List<ScopeData> deletedScopeDataList = new CopyOnWriteArrayList<>();
    private final AtomicBoolean failing = new AtomicBoolean();

    int getAttempts() {
        return attempts.get();
    }

    List<ScopeData> getDeletedScopeDataList() {
        return List.copyOf(deletedScopeDataList);
    }

    void reset() {
        attempts.set(0);
        deletedScopeDataList.clear();
        failing.set(false);
    }

    void setFailing(boolean failing) {
        this.failing.set(failing);
    }

    @Override
    public void deleteScopeData(DataStorageScope scope, String scopeId, PlatformType type) {
        attempts.incrementAndGet();

        if (failing.get()) {
            throw new IllegalStateException("Data Storage is unavailable");
        }

        deletedScopeDataList.add(new ScopeData(scope, scopeId, type));
    }

    @Override
    public void delete(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Optional<T> fetch(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T get(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Map<String, T> getAll(
        String componentName, DataStorageScope scope, String scopeId, long environmentId, PlatformType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void put(
        String componentName, DataStorageScope scope, String scopeId, String key, Object value,
        long environmentId, PlatformType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T update(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type, Function<Optional<T>, T> updater) {

        throw new UnsupportedOperationException();
    }
}
