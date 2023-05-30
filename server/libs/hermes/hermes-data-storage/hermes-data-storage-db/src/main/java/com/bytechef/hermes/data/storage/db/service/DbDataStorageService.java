
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.data.storage.db.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.data.storage.domain.DataStorage;
import com.bytechef.hermes.component.Context.DataStorageScope;
import com.bytechef.hermes.data.storage.db.repository.DataStorageRepository;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
public class DbDataStorageService implements DataStorageService {

    private final DataStorageRepository dataStorageRepository;

    @SuppressFBWarnings("EI")
    public DbDataStorageService(DataStorageRepository dataStorageRepository) {
        this.dataStorageRepository = dataStorageRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public <T> T fetchValue(DataStorageScope scope, long scopeId, String key, T defaultValue) {
        return dataStorageRepository.findByScopeAndScopeIdAndKey(scope.getId(), scopeId, key)
            .map(dataStorage -> (T) dataStorage.getValue())
            .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public DataStorage getDataStorage(long id) {
        return OptionalUtils.get(dataStorageRepository.findById(id));
    }

    @Override
    public void save(DataStorageScope scope, long scopeId, String key, Object value) {
        dataStorageRepository
            .findByScopeAndScopeIdAndKey(scope.getId(), scopeId, key)
            .ifPresentOrElse(
                dataStorage -> {
                    dataStorage.setValue(value);

                    dataStorageRepository.save(dataStorage);
                },
                () -> dataStorageRepository.save(new DataStorage(key, scope.getId(), scopeId, value)));
    }
}
