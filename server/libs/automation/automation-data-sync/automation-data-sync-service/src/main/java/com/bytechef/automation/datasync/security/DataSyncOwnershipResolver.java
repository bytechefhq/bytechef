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
import com.bytechef.automation.datasync.repository.DataSyncRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps a Data Sync id to its owning workspace ({@code data_sync.workspace_id}). Reads the repository directly (not the
 * {@code @PreAuthorize}-guarded facade) to avoid recursion. Fails closed when the Data Sync cannot be resolved.
 *
 * <p>
 * {@code data_sync.workspace_id} is nullable in the schema and {@link DataSync#getWorkspaceId()} is
 * {@code @Nullable Long}, so a workspace-less Data Sync is a real row shape rather than a theoretical one. It must fail
 * closed: {@code Optional.map} collapses the null to {@link ResourceOwner#unknown()}, which both editions'
 * {@code PermissionServiceImpl} deny. Substituting any default workspace here would grant every member of that
 * workspace a foreign Data Sync.
 *
 * @author Ivica Cardic
 */
@Component
public class DataSyncOwnershipResolver implements ResourceOwnershipResolver {

    private final DataSyncRepository dataSyncRepository;

    @SuppressFBWarnings("EI")
    public DataSyncOwnershipResolver(DataSyncRepository dataSyncRepository) {
        this.dataSyncRepository = dataSyncRepository;
    }

    @Override
    public String resourceType() {
        return "DataSync";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return dataSyncRepository.findById(id)
            .map(DataSync::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
