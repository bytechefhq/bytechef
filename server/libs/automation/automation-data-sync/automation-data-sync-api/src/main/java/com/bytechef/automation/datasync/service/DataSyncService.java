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

package com.bytechef.automation.datasync.service;

import com.bytechef.automation.datasync.domain.DataSync;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface DataSyncService {

    DataSync create(DataSync dataSync);

    void delete(long id);

    Optional<DataSync> fetchDataSync(long id);

    DataSync getDataSync(long id);

    List<DataSync> getDataSyncs(@Nullable Long workspaceId);

    /**
     * Returns the project owning the Data Sync's generated workflow — {@code data_sync} keeps no project column of its
     * own.
     *
     * @throws IllegalStateException when no project workflow carries the Data Sync's {@code projectWorkflowUuid}
     */
    long getProjectId(DataSync dataSync);

    /**
     * Batch variant of {@link #getProjectId(DataSync)}, resolved in one query. A Data Sync whose project workflow is
     * gone is left out rather than failing the whole batch.
     *
     * @return Data Sync id &rarr; project id
     */
    Map<Long, Long> getProjectIds(Collection<DataSync> dataSyncs);

    List<DataSync> getProjectDataSyncs(long projectId);

    DataSync update(DataSync dataSync);
}
