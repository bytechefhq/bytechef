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

package com.bytechef.platform.data.storage.jdbc.repository;

import com.bytechef.platform.data.storage.jdbc.domain.DataEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface DataStorageRepository
    extends ListPagingAndSortingRepository<DataEntry, Long>, ListCrudRepository<DataEntry, Long> {

    @Lock(LockMode.PESSIMISTIC_WRITE)
    Optional<DataEntry> findByComponentNameAndScopeAndScopeIdAndKeyAndEnvironmentAndType(
        String componentName, int scope, String scopeId, String key, int environment, int type);

    @Lock(LockMode.PESSIMISTIC_WRITE)
    List<DataEntry> findByComponentNameAndScopeAndScopeIdAndEnvironmentAndType(
        String componentName, int scope, String scopeId, int environment, int type);
}
