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

package com.bytechef.platform.data.storage.db.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.data.storage.db.domain.DataEntry;
import com.bytechef.platform.data.storage.db.repository.DataStorageRepository;
import com.bytechef.platform.data.storage.service.DataStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
public class DbDataStorageServiceImpl implements DataStorageService, DbDataStorageService {

    private final DataStorageRepository dataStorageRepository;

    @SuppressFBWarnings("EI")
    public DbDataStorageServiceImpl(DataStorageRepository dataStorageRepository) {
        this.dataStorageRepository = dataStorageRepository;
    }

    @Override
    public void delete(String componentName, Scope scope, String scopeId, String key, Type type) {
        dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndType(componentName, scope, scopeId, key, type.ordinal())
            .ifPresentOrElse(dataStorageRepository::delete, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional
    public <T> Optional<T> fetch(String componentName, Scope scope, String scopeId, String key, Type type) {
        return dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndType(componentName, scope, scopeId, key, type.ordinal())
            .map(dataEntry -> (T) dataEntry.getValue());
    }

    @Override
    public <T> T get(String componentName, Scope scope, String scopeId, String key, Type type) {
        return OptionalUtils.get(fetch(componentName, scope, scopeId, key, type));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAll(
        String componentName, Scope scope, String scopeId, Type type) {
        return OptionalUtils.get(
            dataStorageRepository
                .findByComponentNameAndScopeAndScopeIdAndType(componentName, scope, scopeId, type.ordinal()))
            .stream()
            .collect(Collectors.toMap(dataEntry -> String.valueOf(dataEntry.getKey()),
                dataEntry -> (T) dataEntry.getValue()));
    }

    @Override
    public void put(String componentName, Scope scope, String scopeId, String key, Type type, Object value) {
        dataStorageRepository
            .findByComponentNameAndScopeAndScopeIdAndKeyAndType(componentName, scope, scopeId, key, type.ordinal())
            .ifPresentOrElse(
                dataEntry -> {
                    dataEntry.setValue(value);

                    dataStorageRepository.save(dataEntry);
                },
                () -> dataStorageRepository.save(new DataEntry(componentName, scope, scopeId, key, value, type)));
    }
}
