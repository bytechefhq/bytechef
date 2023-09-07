
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

package com.bytechef.data.storage.db.service;

import com.bytechef.data.storage.domain.DataEntry;
import com.bytechef.data.storage.db.repository.DataStorageRepository;
import com.bytechef.data.storage.service.DataStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    @Transactional
    public <T> Optional<T> fetchData(String context, int scope, long scopeId, String key) {
        return dataStorageRepository.findByContextAndScopeAndScopeIdAndKey(context, scope, scopeId, key)
            .map(dataEntry -> (T) dataEntry.getData());
    }

    @Override
    public void save(String context, int scope, long scopeId, String key, Object data) {
        dataStorageRepository
            .findByContextAndScopeAndScopeIdAndKey(context, scope, scopeId, key)
            .ifPresentOrElse(
                dataEntry -> {
                    dataEntry.setData(data);

                    dataStorageRepository.save(dataEntry);
                },
                () -> dataStorageRepository.save(new DataEntry(key, scope, scopeId, data)));
    }
}
