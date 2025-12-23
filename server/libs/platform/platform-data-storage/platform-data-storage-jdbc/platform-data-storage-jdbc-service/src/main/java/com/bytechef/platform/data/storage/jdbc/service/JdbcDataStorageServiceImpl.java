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

import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.domain.DataEntry;
import com.bytechef.platform.data.storage.jdbc.repository.DataStorageRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
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
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId, ModeType type) {

        dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndEnvironmentAndType(
                componentName, scope.ordinal(), scopeId, key, (int) environmentId, type.ordinal())
            .ifPresent(dataStorageRepository::delete);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    @Transactional
    public <T> Optional<T> fetch(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId, ModeType type) {

        return dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndEnvironmentAndType(
                componentName, scope.ordinal(), scopeId, key, (int) environmentId, type.ordinal())
            .map(dataEntry -> (T) dataEntry.getValue());
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId, ModeType type) {

        return (T) fetch(componentName, scope, scopeId, key, environmentId, type)
            .orElseThrow();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAll(
        String componentName, DataStorageScope scope, String scopeId, long environmentId, ModeType type) {

        return dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndEnvironmentAndType(
                componentName, scope.ordinal(), scopeId, (int) environmentId, type.ordinal())
            .stream()
            .collect(Collectors.toMap(
                dataEntry -> String.valueOf(dataEntry.getKey()), dataEntry -> (T) dataEntry.getValue()));
    }

    @Override
    public void put(
        String componentName, DataStorageScope scope, String scopeId, String key, long environmentId, ModeType type,
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
}
