/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.data.storage.jdbc.service;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.domain.DataEntry;
import com.bytechef.platform.data.storage.jdbc.domain.DataEntryVersion;
import com.bytechef.platform.data.storage.jdbc.domain.VersionedValue;
import com.bytechef.platform.data.storage.jdbc.repository.DataStorageRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
public class JdbcDataStorageServiceImpl implements JdbcDataStorageService {

    private final DataStorageRepository dataStorageRepository;

    @SuppressFBWarnings("EI")
    public JdbcDataStorageServiceImpl(DataStorageRepository dataStorageRepository) {
        this.dataStorageRepository = dataStorageRepository;
    }

    @Override
    public void delete(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndEnvironmentAndType(
                componentName, scope.ordinal(), scopeId, key, (int) environmentId, type.ordinal())
            .ifPresent(dataStorageRepository::delete);
    }

    @Override
    @Transactional
    public void deleteScopeData(DataStorageScope scope, String scopeId, PlatformType type) {
        dataStorageRepository.deleteAllByScopeAndScopeIdAndType(scope.ordinal(), scopeId, type.ordinal());
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional
    public <T> @NonNull Optional<T> fetch(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        return dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndEnvironmentAndType(
                componentName, scope.ordinal(), scopeId, key, (int) environmentId, type.ordinal())
            .map(dataEntry -> (T) dataEntry.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional
    public <T> Optional<VersionedValue<T>> fetchVersioned(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        return dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndEnvironmentAndType(
                componentName, scope.ordinal(), scopeId, key, (int) environmentId, type.ordinal())
            .map(dataEntry -> new VersionedValue<>(
                (T) dataEntry.getValue(),
                new DataEntryVersion(Objects.requireNonNull(dataEntry.getId()), dataEntry.getVersion())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @NonNull T get(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
        PlatformType type) {

        return (T) fetch(componentName, scope, scopeId, key, environmentId, type)
            .orElseThrow();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @NonNull Map<String, T> getAll(
        String componentName, DataStorageScope scope, String scopeId, long environmentId, PlatformType type) {

        return dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndEnvironmentAndType(
                componentName, scope.ordinal(), scopeId, (int) environmentId, type.ordinal())
            .stream()
            .collect(Collectors.toMap(
                dataEntry -> String.valueOf(dataEntry.getKey()), dataEntry -> (T) dataEntry.getValue()));
    }

    @Override
    public void put(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId, PlatformType type,
        Object value) {

        dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndEnvironmentAndType(
                componentName, scope.ordinal(), scopeId, key, (int) environmentId, type.ordinal())
            .ifPresentOrElse(
                dataEntry -> {
                    dataEntry.setValue(value);

                    dataStorageRepository.save(dataEntry);
                },
                () -> dataStorageRepository.save(
                    new DataEntry(componentName, scope, scopeId, key, value, (int) environmentId, type)));
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean putIfVersion(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId, PlatformType type,
        Object value, @Nullable DataEntryVersion expectedVersion) {

        try {
            if (expectedVersion == null) {
                dataStorageRepository.save(
                    new DataEntry(componentName, scope, scopeId, key, value, (int) environmentId, type));

                return true;
            }

            Optional<DataEntry> dataEntryOptional = dataStorageRepository
                .findByComponentNameAndScopeAndScopeIdAndKeyAndEnvironmentAndType(
                    componentName, scope.ordinal(), scopeId, key, (int) environmentId, type.ordinal());

            if (dataEntryOptional.isEmpty()) {
                return false;
            }

            DataEntry dataEntry = dataEntryOptional.get();

            if (!Objects.equals(dataEntry.getId(), expectedVersion.id()) ||
                dataEntry.getVersion() != expectedVersion.version()) {

                return false;
            }

            dataEntry.setValue(value);

            dataStorageRepository.save(dataEntry);

            return true;
        } catch (RuntimeException exception) {
            if (isLostRace(exception)) {
                return false;
            }

            throw exception;
        }
    }

    private static boolean isLostRace(RuntimeException exception) {
        return ExceptionUtils.indexOfType(exception, DuplicateKeyException.class) >= 0 ||
            ExceptionUtils.indexOfType(exception, OptimisticLockingFailureException.class) >= 0;
    }
}
