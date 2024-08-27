/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.constant.AppType;
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
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, @NonNull String key,
        @NonNull AppType type) {

        dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndType(componentName, scope.ordinal(), scopeId, key,
                type.ordinal())
            .ifPresentOrElse(dataStorageRepository::delete, null);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    @Transactional
    public <T> Optional<T> fetch(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, @NonNull String key,
        @NonNull AppType type) {

        return dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndType(
                componentName, scope.ordinal(), scopeId, key, type.ordinal())
            .map(dataEntry -> (T) dataEntry.getValue());
    }

    @NonNull
    @Override
    public <T> T get(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, @NonNull String key,
        @NonNull AppType type) {

        return OptionalUtils.get(fetch(componentName, scope, scopeId, key, type));
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAll(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
        @NonNull AppType type) {

        return OptionalUtils
            .get(
                dataStorageRepository.findByComponentNameAndScopeAndScopeIdAndType(
                    componentName, scope.ordinal(), scopeId, type.ordinal()))
            .stream()
            .collect(Collectors.toMap(
                dataEntry -> String.valueOf(dataEntry.getKey()), dataEntry -> (T) dataEntry.getValue()));
    }

    @Override
    public void put(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, @NonNull String key,
        @NonNull AppType type, @NonNull Object value) {

        dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndType(
                componentName, scope.ordinal(), scopeId, key, type.ordinal())
            .ifPresentOrElse(
                dataEntry -> {
                    dataEntry.setValue(value);

                    dataStorageRepository.save(dataEntry);
                },
                () -> dataStorageRepository.save(new DataEntry(componentName, scope, scopeId, key, value, type)));
    }
}
