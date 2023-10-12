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

package com.bytechef.data.storage.db.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.data.storage.db.domain.DataEntry;
import com.bytechef.data.storage.db.repository.DataStorageRepository;
import com.bytechef.data.storage.service.DataStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
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
    @SuppressWarnings("unchecked")
    @Transactional
    public <T> Optional<T> fetch(String context, int scope, long scopeId, String key) {
        return dataStorageRepository.findByContextAndScopeAndScopeIdAndKey(context, scope, scopeId, key)
            .map(dataEntry -> (T) dataEntry.getValue());
    }

    @Override
    public <T> T get(String context, int scope, long scopeId, String key) {
        return OptionalUtils.get(fetch(context, scope, scopeId, key));
    }

    @Override
    public void put(String context, int scope, long scopeId, String key, Object value) {
        dataStorageRepository
            .findByContextAndScopeAndScopeIdAndKey(context, scope, scopeId, key)
            .ifPresentOrElse(
                dataEntry -> {
                    dataEntry.setValue(value);

                    dataStorageRepository.save(dataEntry);
                },
                () -> dataStorageRepository.save(new DataEntry(context, scope, scopeId, key, value)));
    }
}
