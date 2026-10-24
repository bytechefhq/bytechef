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

package com.bytechef.automation.datasync.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.domain.DataSyncElement;
import com.bytechef.automation.datasync.repository.DataSyncElementRepository;
import com.bytechef.automation.datasync.repository.DataSyncRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps a Data Sync element id to its owning workspace by traversing element &rarr; Data Sync &rarr;
 * {@code data_sync.workspace_id}. Reads the repositories directly (not the {@code @PreAuthorize}-guarded facade) to
 * avoid recursion. Fails closed when either hop cannot be resolved.
 *
 * <p>
 * An element has no workspace of its own: {@code updateDataSyncElement} is keyed on the element's own id, so the owning
 * workspace has to be walked to rather than read off an argument.
 *
 * <p>
 * {@code DataSyncElement.dataSyncId} is a primitive {@code long}, so the first hop cannot be null; the second can —
 * {@link DataSync#getWorkspaceId()} is {@code @Nullable Long} and the tail here matches
 * {@link DataSyncOwnershipResolver} exactly so the child path and the Data Sync path cannot disagree about a
 * workspace-less Data Sync.
 *
 * @author Ivica Cardic
 */
@Component
public class DataSyncElementOwnershipResolver implements ResourceOwnershipResolver {

    private final DataSyncElementRepository dataSyncElementRepository;
    private final DataSyncRepository dataSyncRepository;

    @SuppressFBWarnings("EI")
    public DataSyncElementOwnershipResolver(
        DataSyncElementRepository dataSyncElementRepository, DataSyncRepository dataSyncRepository) {

        this.dataSyncElementRepository = dataSyncElementRepository;
        this.dataSyncRepository = dataSyncRepository;
    }

    @Override
    public String resourceType() {
        return "DataSyncElement";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return dataSyncElementRepository.findById(id)
            .map(DataSyncElement::getDataSyncId)
            .flatMap(dataSyncRepository::findById)
            .map(DataSync::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
